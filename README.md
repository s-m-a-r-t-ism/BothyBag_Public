# BothyBag

An Android app for finding bothies in Britain and keeping track of the ones
you have visited, with a web version for iPhone and any other browser.

## What it does

- **Map** of 262 locations: 103 Mountain Bothies Association (MBA) bothies and
  159 other shelters from OpenStreetMap. Light and dark map styles, search by
  name, and a filter for MBA or non-MBA only.
- **Bothy details**: region, elevation, OS grid reference, fire and size,
  features, access, and the MBA's own description and warnings. Closed and
  private locations are marked as such.
- **Bagging**: mark a bothy as visited with a date, keep personal notes, and
  see how many MBA bothies you have visited.
- **MBA news**: the latest articles from the MBA website, linked to the bothy
  they are about.
- **Share**: share a bothy's name and grid reference.
- **Backup**: visited bothies and notes are included in Android's own backup,
  and can be exported to and imported from a file through the map menu.

Map tiles are cached as you browse, so areas you have looked at stay available
without a signal. Everything else works offline except the news.

## Building

Requirements: Android Studio (Meerkat 2024.3.1 Patch 1 or newer), or JDK 17
and the Android SDK with platform 36.

```
./gradlew assembleDebug        # debug APK in app/build/outputs/apk/debug
./gradlew testDebugUnitTest    # unit tests
./gradlew assembleRelease      # shrunk release build (unsigned)
```

The app needs Android 8.0 (API 26) or newer.

Every pull request and every change on `main` is built and tested by GitHub
Actions (`.github/workflows/build.yml`).

### Releasing a new version

Raise both `versionCode` and `versionName` in `app/build.gradle.kts`. The
version shown in the app's menu is read from there.

## Web app (iPhone and other browsers)

`web/` holds a web version of the app with the same features. It is a
Progressive Web App: open it in Safari on an iPhone, tap Share, then
"Add to Home Screen", and it works like an installed app, offline included.
It uses the same `bothies.json` as the Android app, and its backup files are
interchangeable with the Android export.

It is published to GitHub Pages by `.github/workflows/pages.yml` whenever the
web app or the dataset changes on `main`, and once a day to refresh the MBA
news (browsers cannot read the MBA feed directly, so the workflow saves it as
`data/news.json`).

To try it locally:

```
python scripts/build_web.py --serve 8000    # then open http://localhost:8000
```

The version shown in the web app's menu is also read from
`app/build.gradle.kts`.

## Project layout

```
app/src/main/assets/bothies.json   the bothy dataset
app/src/main/java/com/bothybag/app
  data/         dataset, visited list, notes, news, backup
  ui/map/       map screen and markers
  ui/detail/    bothy detail screen
  ui/news/      MBA news screen
  navigation/   screen navigation
  location/     device location
app/src/test    unit tests
web/            web app (PWA)
scripts/        dataset maintenance, web build and news fetch
```

## The dataset

`bothies.json` holds one entry per location. The fields the app relies on:

| Field | Meaning |
| --- | --- |
| `id` | Unique id, `mba_…` or `osm_…`. Visited dates and notes are stored against it, so never reuse or renumber ids. |
| `type` | `MBA` or `Non-MBA` |
| `status` | `open`, `closed`, `permanently_closed` or `seasonal` |
| `access` | OpenStreetMap access value. `private`, `customers` and `no` are shown as Private. |
| `notes` | Short warning shown at the top of the detail screen |

`BothyDatasetTest` checks the file for duplicate ids, unknown status values,
impossible coordinates and scraping leftovers, so run the unit tests after
editing it.

To refresh the MBA descriptions and features from the MBA website:

```
pip install requests beautifulsoup4
python scripts/scrape_descriptions.py          # only bothies without a description
python scripts/scrape_descriptions.py --all    # everything
```

Bothy closures and restrictions change. The bundled descriptions are a
snapshot, so check the MBA website before relying on a bothy.

## Data sources and credits

- Bothy locations and details: [Mountain Bothies Association](https://www.mountainbothies.org.uk/)
  and haroldstreet.org.uk
- Other shelters: © [OpenStreetMap](https://www.openstreetmap.org/copyright)
  contributors, available under the Open Database License
- Map tiles: OpenStreetMap, and [CARTO](https://carto.com/attributions) for the
  dark style
- Map display: [osmdroid](https://github.com/osmdroid/osmdroid), and
  [Leaflet](https://leafletjs.com/) in the web app

BothyBag is an independent project and is not affiliated with the Mountain
Bothies Association.

Created by Mart Zijp.
