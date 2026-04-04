"""
Scrape bothy descriptions and features from MBA website pages.
Updates app/src/main/assets/bothies.json.

Modes:
  --missing-only   Only scrape bothies with empty description (default)
  --features       Re-scrape features for bothies with empty bothy_features
  --all            Re-scrape everything
"""

import json
import sys
import time
import argparse
import requests
from bs4 import BeautifulSoup
from pathlib import Path

# Fix Windows console encoding
sys.stdout.reconfigure(encoding='utf-8')

BOTHIES_JSON = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "assets" / "bothies.json"

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
}

DELAY = 3  # seconds between requests


def extract_description(content_div):
    """Extract description paragraphs from content-text div.

    Collects text from <p>, <h3>, and info <div> elements between the
    grid-ref div and the first <hr> (which precedes Bothy Features).
    """
    paragraphs = []
    started = False
    for child in content_div.children:
        if not hasattr(child, 'name') or not child.name:
            continue
        if child.name == 'h1':
            continue
        if child.name == 'div' and not started:
            started = True
            continue
        if not started:
            continue
        if child.name == 'hr':
            break
        if child.name == 'div':
            text = child.get_text(strip=True)
            if len(text) > 20:
                paragraphs.append(text)
            continue
        if child.name in ('p', 'h3'):
            text = child.get_text(strip=True)
            if text:
                paragraphs.append(text)

    return "\n\n".join(paragraphs)


def extract_features(content_div):
    """Extract bothy features from the <ul> after <h2>Bothy Features</h2>."""
    h2 = content_div.find('h2', string=lambda t: t and 'Bothy Features' in t)
    if not h2:
        return []
    ul = h2.find_next_sibling('ul')
    if not ul:
        return []
    return [li.get_text(strip=True) for li in ul.find_all('li') if li.get_text(strip=True)]


def scrape_bothy(url):
    """Fetch a bothy page and extract description + features."""
    r = requests.get(url, headers=HEADERS, timeout=15)
    r.raise_for_status()
    soup = BeautifulSoup(r.text, 'html.parser')
    content = soup.find(id='content-text')
    if not content:
        return None, []
    description = extract_description(content)
    features = extract_features(content)
    return description, features


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--features', action='store_true',
                        help='Re-scrape features for bothies with empty bothy_features')
    parser.add_argument('--all', action='store_true',
                        help='Re-scrape everything')
    args = parser.parse_args()

    with open(BOTHIES_JSON, encoding='utf-8') as f:
        bothies = json.load(f)

    if args.all:
        to_scrape = [
            (i, b) for i, b in enumerate(bothies)
            if b.get('type') == 'MBA' and b.get('mba_url')
        ]
    elif args.features:
        to_scrape = [
            (i, b) for i, b in enumerate(bothies)
            if b.get('type') == 'MBA' and b.get('mba_url') and not b.get('bothy_features')
        ]
    else:
        to_scrape = [
            (i, b) for i, b in enumerate(bothies)
            if b.get('type') == 'MBA' and b.get('mba_url') and not b.get('description')
        ]

    print(f"Found {len(to_scrape)} MBA bothies to scrape")

    for idx, (i, bothy) in enumerate(to_scrape):
        name = bothy['name']
        url = bothy['mba_url']
        print(f"[{idx + 1}/{len(to_scrape)}] {name} — {url}")

        try:
            description, features = scrape_bothy(url)
            if description:
                bothies[i]['description'] = description
                print(f"  OK Description: {len(description)} chars")
            else:
                print(f"  SKIP No description found")
            if features:
                bothies[i]['bothy_features'] = features
                print(f"  OK Features: {features}")
            else:
                print(f"  SKIP No features found")
        except Exception as e:
            print(f"  ERROR: {e}")

        if idx < len(to_scrape) - 1:
            time.sleep(DELAY)

    with open(BOTHIES_JSON, 'w', encoding='utf-8') as f:
        json.dump(bothies, f, indent=2, ensure_ascii=False)

    print(f"\nDone. Updated {BOTHIES_JSON}")


if __name__ == '__main__':
    main()
