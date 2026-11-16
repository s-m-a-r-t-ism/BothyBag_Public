"""
Build the BothyBag web app (PWA) into a folder that can be hosted as a static site.

Copies web/ and the shared app/src/main/assets/bothies.json, then stamps the
service worker with a content hash so installed copies notice new versions.

Usage:
  python scripts/build_web.py                 # writes dist-web/
  python scripts/build_web.py --serve 8000    # build, then preview on http://localhost:8000
"""

import argparse
import hashlib
import http.server
import json
import shutil
from functools import partial
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
WEB = ROOT / "web"
BOTHIES_JSON = ROOT / "app" / "src" / "main" / "assets" / "bothies.json"

# Not precached: the worker itself, and news, which is fetched fresh when online.
NOT_PRECACHED = {"sw.js", "data/news.json"}


def build(out: Path) -> None:
    if out.exists():
        shutil.rmtree(out)
    shutil.copytree(WEB, out)

    data = out / "data"
    data.mkdir(exist_ok=True)
    # Re-serialise compactly: fails loudly on broken JSON and makes the download smaller.
    bothies = json.loads(BOTHIES_JSON.read_text(encoding="utf-8"))
    (data / "bothies.json").write_text(
        json.dumps(bothies, ensure_ascii=False, separators=(",", ":")), encoding="utf-8"
    )
    # Placeholder so the app shows "No news available" until fetch_news.py has run.
    (data / "news.json").write_text('{"fetched_at": null, "items": []}\n', encoding="utf-8")

    files = sorted(
        p.relative_to(out).as_posix() for p in out.rglob("*") if p.is_file()
    )
    precache = [f for f in files if f not in NOT_PRECACHED and f != "index.html"]
    precache.insert(0, "./")

    digest = hashlib.sha256()
    for f in files:
        if f in NOT_PRECACHED:
            continue
        digest.update(f.encode())
        digest.update((out / f).read_bytes())
    version = digest.hexdigest()[:12]

    sw = out / "sw.js"
    text = sw.read_text(encoding="utf-8")
    for old, new in (
        ("const VERSION = 'dev';", f"const VERSION = '{version}';"),
        ("const PRECACHE = [];", f"const PRECACHE = {json.dumps(precache)};"),
    ):
        if text.count(old) != 1:
            raise SystemExit(f"build_web: expected exactly one {old!r} in web/sw.js")
        text = text.replace(old, new)
    sw.write_text(text, encoding="utf-8")

    print(f"Built {out} ({len(files)} files, {len(bothies)} bothies, version {version})")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.strip().splitlines()[0])
    parser.add_argument("--out", type=Path, default=ROOT / "dist-web", help="output folder")
    parser.add_argument("--serve", type=int, metavar="PORT", help="preview the build on this port")
    args = parser.parse_args()

    out = args.out.resolve()
    if out == ROOT or ROOT in out.parents and out.name in {"web", "app", "scripts", ".git"}:
        raise SystemExit(f"build_web: refusing to overwrite {out}")
    build(out)

    if args.serve:
        handler = partial(http.server.SimpleHTTPRequestHandler, directory=str(out))
        print(f"Preview on http://localhost:{args.serve} (Ctrl+C to stop)")
        http.server.ThreadingHTTPServer(("127.0.0.1", args.serve), handler).serve_forever()


if __name__ == "__main__":
    main()
