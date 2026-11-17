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
import html
import json
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from email.utils import parsedate_to_datetime
from pathlib import Path

RSS_URL = "https://www.mountainbothies.org.uk/feed/"
MAX_PAGES = 5  # about 50 articles, same as the Android app
MAX_BYTES = 5 * 1024 * 1024
TIMEOUT = 20

# The MBA server rejects requests without a browser-like User-Agent.
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                  "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    "Accept": "application/rss+xml, application/xml, text/xml, */*",
}


def download(url: str) -> bytes:
    if not url.startswith("https://"):
        raise ValueError(f"refusing non-https URL: {url}")
    request = urllib.request.Request(url, headers=HEADERS)
    with urllib.request.urlopen(request, timeout=TIMEOUT) as response:
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


def fetch_feed() -> list[dict]:
    items: list[dict] = []
    seen: set[str] = set()
    for page in range(1, MAX_PAGES + 1):
        url = RSS_URL if page == 1 else f"{RSS_URL}?paged={page}"
        try:
            page_items = parse_rss(download(url))
        except Exception:
            if page == 1:
                raise  # only a failing first page is fatal
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
