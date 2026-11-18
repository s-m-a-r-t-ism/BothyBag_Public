"""
Fetch the Mountain Bothies Association news feed and save it as JSON for the web app.

Browsers may not read the MBA feed directly (no CORS headers), so a scheduled job
runs this script and publishes the result next to the app as data/news.json.

If the feed cannot be reached, the previously published copy (--fallback-url) is
kept, so a temporary outage never empties the news screen.

Usage:
  python scripts/fetch_news.py --out dist-web/data/news.json
"""

import argparse
import gzip
import html
import json
import re
import shutil
import subprocess
import sys
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from email.utils import parsedate_to_datetime
from pathlib import Path

RSS_URL = "https://www.mountainbothies.org.uk/feed/"
ALT_RSS_URL = "https://www.mountainbothies.org.uk/?feed=rss2"  # same feed, WordPress's plain address
MAX_PAGES = 5  # about 50 articles, same as the Android app
MAX_BYTES = 5 * 1024 * 1024
TIMEOUT = 20

# The MBA server rejects requests that do not look like they come from a browser,
# so send the headers a desktop Chrome sends when it opens a page.
BROWSER_HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                  "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,"
              "application/rss+xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
    "Accept-Language": "en-GB,en;q=0.9",
    "Accept-Encoding": "gzip",
    "Cache-Control": "no-cache",
    "Pragma": "no-cache",
    "Referer": "https://www.mountainbothies.org.uk/",
    "Sec-Ch-Ua": '"Google Chrome";v="131", "Chromium";v="131", "Not_A Brand";v="24"',
    "Sec-Ch-Ua-Mobile": "?0",
    "Sec-Ch-Ua-Platform": '"Windows"',
    "Sec-Fetch-Dest": "document",
    "Sec-Fetch-Mode": "navigate",
    "Sec-Fetch-Site": "same-origin",
    "Sec-Fetch-User": "?1",
    "Upgrade-Insecure-Requests": "1",
}


class FetchError(Exception):
    pass


def _check_url(url: str) -> None:
    if not url.startswith("https://"):
        raise ValueError(f"refusing non-https URL: {url}")


def _describe_refusal(status, headers, body: bytes) -> str:
    """Short explanation of a refused request, for the workflow log."""
    server = headers.get("Server", "?") if headers else "?"
    hints = []
    if headers and (headers.get("cf-ray") or headers.get("cf-mitigated")):
        hints.append("Cloudflare")
    snippet = re.sub(r"\s+", " ", re.sub(r"<[^>]*>", " ", body[:3000].decode("utf-8", "replace"))).strip()
    if re.search(r"just a moment|attention required|cloudflare", snippet, re.I):
        hints.append("bot challenge page")
    return f"HTTP {status}, server {server}{', ' + ', '.join(hints) if hints else ''}: {snippet[:160]!r}"


def _with_urllib(url: str) -> bytes:
    request = urllib.request.Request(url, headers=BROWSER_HEADERS)
    try:
        with urllib.request.urlopen(request, timeout=TIMEOUT) as response:
            body = response.read(MAX_BYTES + 1)
            if response.headers.get("Content-Encoding", "").lower() == "gzip":
                body = gzip.decompress(body)
    except urllib.error.HTTPError as error:
        raise FetchError(_describe_refusal(error.code, error.headers, error.read(3000))) from None
    if len(body) > MAX_BYTES:
        raise FetchError(f"response larger than {MAX_BYTES} bytes")
    return body


def _with_curl(url: str) -> bytes:
    """curl presents itself differently from Python at the TLS level, which some
    bot filters treat differently."""
    if not shutil.which("curl"):
        raise FetchError("curl not installed")
    command = ["curl", "--silent", "--show-error", "--location", "--compressed",
               "--max-time", str(TIMEOUT), "--max-filesize", str(MAX_BYTES),
               "--write-out", "\n%{http_code}"]
    for name, value in BROWSER_HEADERS.items():
        if name != "Accept-Encoding":  # --compressed sets it
            command += ["--header", f"{name}: {value}"]
    result = subprocess.run(command + [url], capture_output=True, timeout=TIMEOUT + 10)
    if result.returncode != 0:
        raise FetchError(f"curl failed: {result.stderr.decode('utf-8', 'replace').strip()}")
    body, _, status = result.stdout.rpartition(b"\n")
    if status.strip() != b"200":
        raise FetchError(_describe_refusal(status.decode().strip(), None, body))
    return body


def download_feed(url: str) -> bytes:
    """Fetches a feed page, trying Python first and curl second. Logs why each attempt failed."""
    _check_url(url)
    problems = []
    for name, method in (("python", _with_urllib), ("curl", _with_curl)):
        try:
            body = method(url)
            if problems:
                print(f"  {url}: worked with {name} after: {'; '.join(problems)}")
            return body
        except (FetchError, OSError, subprocess.SubprocessError) as error:
            problems.append(f"{name}: {error}")
    raise FetchError(f"{url}: " + " | ".join(problems))


def download(url: str) -> bytes:
    """Plain download, used for the previously published news.json."""
    _check_url(url)
    with urllib.request.urlopen(url, timeout=TIMEOUT) as response:
        body = response.read(MAX_BYTES + 1)
    if len(body) > MAX_BYTES:
        raise ValueError(f"response larger than {MAX_BYTES} bytes")
    return body


def strip_html(text: str) -> str:
    text = re.sub(r"<[^>]*>", "", text)
    text = html.unescape(text)
    return re.sub(r"\s+", " ", text).strip()


def iso_date(rss_date: str) -> str:
    """'Mon, 16 Mar 2026 10:01:58 +0000' -> '2026-03-16T10:01:58+00:00' (unchanged if unparseable)."""
    try:
        return parsedate_to_datetime(rss_date).isoformat()
    except (TypeError, ValueError):
        return rss_date


def parse_rss(xml_bytes: bytes) -> list[dict]:
    root = ET.fromstring(xml_bytes)
    items = []
    for item in root.iter("item"):
        title = strip_html(item.findtext("title") or "")
        link = (item.findtext("link") or "").strip()
        if not title:
            continue
        items.append({
            "title": title,
            "link": link if link.startswith(("https://", "http://")) else "",
            "date": iso_date((item.findtext("pubDate") or "").strip()),
            "description": strip_html(item.findtext("description") or ""),
        })
    return items


def first_page() -> bytes:
    """The feed's first page, trying WordPress's other feed address if the usual one is refused."""
    errors = []
    for url in (RSS_URL, ALT_RSS_URL):
        try:
            return download_feed(url)
        except FetchError as error:
            errors.append(str(error))
    raise FetchError("\n  ".join(errors))


def fetch_feed() -> list[dict]:
    items: list[dict] = []
    seen: set[str] = set()
    for page in range(1, MAX_PAGES + 1):
        try:
            if page == 1:
                page_items = parse_rss(first_page())
            else:
                page_items = parse_rss(download_feed(f"{RSS_URL}?paged={page}"))
        except Exception as error:
            if page == 1:
                raise  # only a failing first page is fatal
            print(f"Stopped at page {page}: {error}", file=sys.stderr)
            break
        if not page_items:
            break
        for it in page_items:
            key = it["link"] or it["title"]
            if key not in seen:
                seen.add(key)
                items.append(it)
    return items


def valid_payload(payload) -> bool:
    return isinstance(payload, dict) and isinstance(payload.get("items"), list)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.strip().splitlines()[0])
    parser.add_argument("--out", type=Path, required=True, help="where to write news.json")
    parser.add_argument("--fallback-url", help="published news.json to reuse if the feed is down")
    args = parser.parse_args()

    payload = None
    try:
        items = fetch_feed()
        if not items:
            raise ValueError("feed contained no items")
        payload = {"fetched_at": datetime.now(timezone.utc).isoformat(timespec="seconds"), "items": items}
        print(f"Fetched {len(items)} news items")
    except Exception as error:  # noqa: BLE001 - any failure falls back to the old copy
        print(f"WARNING: could not fetch MBA feed: {error}", file=sys.stderr)
        if args.fallback_url:
            try:
                previous = json.loads(download(args.fallback_url))
                if valid_payload(previous) and previous["items"]:
                    payload = previous
                    print(f"Kept {len(previous['items'])} previously published items")
            except Exception as fallback_error:  # noqa: BLE001
                print(f"WARNING: no previous news either: {fallback_error}", file=sys.stderr)

    if payload is None:
        payload = {"fetched_at": None, "items": []}

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(payload, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main())
