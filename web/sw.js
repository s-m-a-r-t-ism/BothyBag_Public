// BothyBag service worker: makes the app start offline and keeps map tiles you have already seen.
// scripts/build_web.py rewrites the two lines below on every build.
const VERSION = 'dev';
const PRECACHE = [];

const SHELL_CACHE = `bothybag-shell-${VERSION}`;
const DATA_CACHE = 'bothybag-data-v1';
const TILE_CACHE = 'bothybag-tiles-v1';
const TILE_HOSTS = ['tile.openstreetmap.org', 'basemaps.cartocdn.com'];
const MAX_TILES = 2500;
const TRIM_TO = 2000;

self.addEventListener('install', (event) => {
  event.waitUntil((async () => {
    const cache = await caches.open(SHELL_CACHE);
    // 'reload' skips the browser's HTTP cache so a new version never precaches stale files.
    await cache.addAll(PRECACHE.map((url) => new Request(url, { cache: 'reload' })));
    await self.skipWaiting();
  })());
});

self.addEventListener('activate', (event) => {
  event.waitUntil((async () => {
    const names = await caches.keys();
    await Promise.all(names
      .filter((n) => n.startsWith('bothybag-shell-') && n !== SHELL_CACHE)
      .map((n) => caches.delete(n)));
    await self.clients.claim();
  })());
});

let tilePuts = 0;

async function trimTiles(cache) {
  const keys = await cache.keys();
  if (keys.length <= MAX_TILES) return;
  // Keys come back oldest first.
  await Promise.all(keys.slice(0, keys.length - TRIM_TO).map((k) => cache.delete(k)));
}

async function tile(request) {
  const cache = await caches.open(TILE_CACHE);
  const hit = await cache.match(request);
  if (hit) return hit;
  const res = await fetch(request);
  if (res.ok) {
    await cache.put(request, res.clone());
    tilePuts += 1;
    if (tilePuts % 50 === 0) trimTiles(cache).catch(() => {});
  }
  return res;
}

async function networkFirst(request) {
  const cache = await caches.open(DATA_CACHE);
  try {
    const res = await fetch(request);
    if (res.ok) await cache.put(request.url, res.clone());
    return res;
  } catch (err) {
    const hit = await cache.match(request.url);
    if (hit) return hit;
    throw err;
  }
}

async function shell(request) {
  const cache = await caches.open(SHELL_CACHE);
  const key = request.mode === 'navigate' ? './' : request;
  const hit = await cache.match(key, { ignoreSearch: true });
  return hit || fetch(request);
}

self.addEventListener('fetch', (event) => {
  const { request } = event;
  if (request.method !== 'GET') return;
  const url = new URL(request.url);

  if (TILE_HOSTS.some((host) => url.hostname === host || url.hostname.endsWith(`.${host}`))) {
    event.respondWith(tile(request));
  } else if (url.origin === self.location.origin) {
    event.respondWith(url.pathname.endsWith('/data/news.json') ? networkFirst(request) : shell(request));
  }
});
