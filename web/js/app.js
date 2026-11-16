// BothyBag web app: map, bothy detail and MBA news. A port of the Android app.

import { icon, iconSvg, markerSvg } from './icons.js';
import { visited, notes, prefs, newsCache, todayIso } from './store.js';
import {
  loadBothies, allBothies, getBothy, isMba, isClosed, regionOf, statusBadge,
  fireInfo, sizeInfo, elevationText, safeUrl, matchBothy,
} from './bothies.js';

/* global L */

const $ = (id) => document.getElementById(id);

const IS_IOS = /iP(hone|ad|od)/.test(navigator.userAgent) ||
  (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
const IS_STANDALONE = window.matchMedia('(display-mode: standalone)').matches ||
  navigator.standalone === true;

/** Tiny DOM builder. Text always goes in as text, never as HTML. */
function h(tag, props = {}, ...children) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(props)) {
    if (v == null || v === false) continue;
    if (k === 'class') el.className = v;
    else if (k.startsWith('on')) el.addEventListener(k.slice(2), v);
    else el.setAttribute(k, v === true ? '' : v);
  }
  for (const c of children.flat()) {
    if (c == null || c === false) continue;
    el.append(c.nodeType ? c : document.createTextNode(String(c)));
  }
  return el;
}

/* ---------- Toast ---------- */

let toastTimer = 0;

function toast(message, action) {
  const el = $('toast');
  el.replaceChildren(message);
  if (action) el.append(h('button', { onclick: action.run }, action.label));
  el.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { el.hidden = true; }, action ? 9000 : 3000);
}

/* ---------- Map ---------- */

const LIGHT_TILES = {
  url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
  options: {
    maxZoom: 19,
    crossOrigin: true,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap</a>',
  },
};

const DARK_TILES = {
  url: 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',
  options: {
    maxZoom: 19,
    subdomains: 'abc',
    crossOrigin: true,
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap</a> &copy; <a href="https://carto.com/attributions" target="_blank" rel="noopener">CARTO</a>',
  },
};

const DETAIL_ZOOM = 14;
const VIEW_MAX_AGE_MS = 30 * 60 * 1000;

let map;
let tileLayer;
let markerLayer;
let popup;
let popupBothyId = null;
const markers = new Map();
const iconCache = new Map();

let isDarkMap = prefs.get('dark_map', false);
let showMba = true;
let showNonMba = true;

function markerIcon(b, isVisited) {
  const kind = isClosed(b) ? 'closed' : isMba(b) ? 'mba' : 'other';
  const key = `${kind}_${isVisited}`;
  if (!iconCache.has(key)) {
    iconCache.set(key, L.divIcon({
      className: 'bothy-marker',
      html: markerSvg(kind, isVisited),
      iconSize: [40, 40],
      iconAnchor: [20, 36],
    }));
  }
  return iconCache.get(key);
}

function setTiles() {
  if (tileLayer) tileLayer.remove();
  const t = isDarkMap ? DARK_TILES : LIGHT_TILES;
  tileLayer = L.tileLayer(t.url, t.options).addTo(map);
  $('btn-dark').innerHTML = iconSvg(isDarkMap ? 'sun' : 'moon');
}

function initMap() {
  // After a reload (iOS often restarts web apps) return to where the user was.
  const saved = prefs.get('view', null);
  const recent = saved && Date.now() - saved.t < VIEW_MAX_AGE_MS;
  const center = recent ? [saved.lat, saved.lng] : [56.5, -4.0];
  const zoom = recent ? saved.z : (window.innerWidth < 600 ? 6 : 7);

  map = L.map('map', { center, zoom, zoomControl: false, minZoom: 4, maxZoom: 19 });
  map.attributionControl.setPrefix(false);
  setTiles();
  markerLayer = L.layerGroup().addTo(map);
  popup = L.popup({ closeButton: false, offset: [0, -30], autoPanPadding: [24, 24] });

  const updateZoomClass = () => {
    const z = map.getZoom();
    const el = map.getContainer();
    el.classList.toggle('zoom-far', z < 7.5);
    el.classList.toggle('zoom-mid', z >= 7.5 && z < 9.5);
  };
  map.on('zoomend', updateZoomClass);
  updateZoomClass();

  map.on('popupclose', () => { popupBothyId = null; });
  map.on('moveend', () => {
    const c = map.getCenter();
    prefs.set('view', { lat: c.lat, lng: c.lng, z: map.getZoom(), t: Date.now() });
  });
}

function openBubble(b) {
  const visitedMark = visited.date(b.id) ? ' ✓' : '';
  const region = regionOf(b);
  const snippet = (isMba(b) ? 'MBA' : 'Non-MBA') + (region ? ` • ${region}` : '') + visitedMark;
  const content = h('button', { class: 'bubble', onclick: () => go(`#/bothy/${b.id}`) },
    h('span', { class: 'bubble-text' },
      h('div', { class: 'bubble-title' }, b.name),
      h('div', { class: 'bubble-snippet' }, snippet)),
    icon('chevron'));
  popup.setLatLng([b.latitude, b.longitude]).setContent(content).openOn(map);
  popupBothyId = b.id;
}

function renderMarkers() {
  markerLayer.clearLayers();
  markers.clear();
  const visitedMap = visited.map();
  for (const b of allBothies()) {
    if (isMba(b) ? !showMba : !showNonMba) continue;
    const m = L.marker([b.latitude, b.longitude], {
      icon: markerIcon(b, b.id in visitedMap),
      title: b.name,
      alt: b.name,
      keyboard: false,
    });
    // Leaflet closes the open bubble just before it reports the tap, so note what was open first.
    let bubbleWasOpen = false;
    m.on('preclick', () => { bubbleWasOpen = popupBothyId === b.id; });
    m.on('click', () => {
      // First tap shows the name bubble, a second tap opens the bothy.
      if (bubbleWasOpen || popupBothyId === b.id) {
        go(`#/bothy/${b.id}`);
      } else {
        openBubble(b);
        map.panTo([b.latitude, b.longitude]);
      }
    });
    markers.set(b.id, m);
    markerLayer.addLayer(m);
  }
  if (popupBothyId && !markers.has(popupBothyId)) map.closePopup();
}

function updateFilterLabel() {
  const label = $('filter-label');
  label.hidden = showMba && showNonMba;
  label.textContent = showMba ? 'MBA only' : 'Non-MBA only';
}

function cycleFilter() {
  // All -> MBA only -> Non-MBA only -> All
  if (showMba && showNonMba) showNonMba = false;
  else if (showMba) { showMba = false; showNonMba = true; }
  else showMba = true;
  updateFilterLabel();
  renderMarkers();
}

function focusBothy(b, withBubble) {
  map.setView([b.latitude, b.longitude], Math.max(map.getZoom(), DETAIL_ZOOM), { animate: true });
  if (withBubble && markers.has(b.id)) openBubble(b);
}

/* ---------- Location ---------- */

let watchId = null;
let lastFix = null;
let meMarker = null;
let meCircle = null;
let centerOnNextFix = false;

function onFix(pos) {
  const { latitude, longitude, accuracy } = pos.coords;
  lastFix = [latitude, longitude];
  if (!meMarker) {
    meCircle = L.circle(lastFix, {
      radius: accuracy, stroke: false, fillColor: '#1976D2', fillOpacity: 0.15, interactive: false,
    }).addTo(map);
    meMarker = L.marker(lastFix, {
      icon: L.divIcon({ className: '', html: '<div class="me-marker"></div>', iconSize: [18, 18], iconAnchor: [9, 9] }),
      interactive: false,
      keyboard: false,
      zIndexOffset: -500,
    }).addTo(map);
  } else {
    meMarker.setLatLng(lastFix);
    meCircle.setLatLng(lastFix).setRadius(accuracy);
  }
  $('btn-locate').classList.add('active');
  if (centerOnNextFix) {
    centerOnNextFix = false;
    map.setView(lastFix, DETAIL_ZOOM, { animate: true });
  }
}

function onFixError(err) {
  if (err.code === err.PERMISSION_DENIED) {
    stopLocation();
    toast('Location access is off. Allow it in your browser settings to see where you are.');
  } else if (centerOnNextFix) {
    toast('Could not find your location yet');
  }
  centerOnNextFix = false;
}

function startLocation() {
  if (watchId != null || !navigator.geolocation) return;
  watchId = navigator.geolocation.watchPosition(onFix, onFixError, {
    enableHighAccuracy: true, maximumAge: 10000, timeout: 20000,
  });
}

function stopLocation() {
  if (watchId != null) navigator.geolocation.clearWatch(watchId);
  watchId = null;
  $('btn-locate').classList.remove('active');
}

function locateMe() {
  if (!navigator.geolocation) {
    toast('Location is not available on this device');
    return;
  }
  if (lastFix) {
    map.setView(lastFix, DETAIL_ZOOM, { animate: true });
    return;
  }
  centerOnNextFix = true;
  startLocation();
}

/** Start tracking straight away only if permission was granted before; otherwise wait for a tap. */
async function resumeLocation() {
  try {
    const status = await navigator.permissions.query({ name: 'geolocation' });
    if (status.state === 'granted') startLocation();
  } catch {
    // Permissions API not available: the locate button still works.
  }
}

/* ---------- Menu and search ---------- */

function mbaTotals() {
  const visitedMap = visited.map();
  let total = 0;
  let done = 0;
  for (const b of allBothies()) {
    if (!isMba(b)) continue;
    total += 1;
    if (b.id in visitedMap) done += 1;
  }
  return { total, done };
}

function openMenu() {
  const { total, done } = mbaTotals();
  $('menu-visited').textContent = `${done}/${total} MBA visited`;
  $('search').value = '';
  $('search-results').replaceChildren();
  $('menu').hidden = false;
  $('menu-backdrop').hidden = false;
  $('btn-menu').setAttribute('aria-expanded', 'true');
  // Only desktop gets autofocus; on phones it would pop the keyboard over the map.
  if (window.matchMedia('(hover: hover) and (pointer: fine)').matches) $('search').focus();
}

function closeMenu() {
  $('menu').hidden = true;
  $('menu-backdrop').hidden = true;
  $('btn-menu').setAttribute('aria-expanded', 'false');
  $('search').blur();
}

function runSearch() {
  const q = $('search').value.trim().toLowerCase();
  const list = $('search-results');
  if (q.length < 2) {
    list.replaceChildren();
    return;
  }
  const results = allBothies().filter((b) => b.name.toLowerCase().includes(q)).slice(0, 8);
  list.replaceChildren(...results.map((b) =>
    h('li', {}, h('button', {
      onclick: () => {
        closeMenu();
        // Make sure the bothy is not hidden by the current filter.
        if (!markers.has(b.id)) {
          showMba = true;
          showNonMba = true;
          updateFilterLabel();
          renderMarkers();
        }
        map.setView([b.latitude, b.longitude], DETAIL_ZOOM, { animate: true });
        openBubble(b);
      },
    }, b.name, h('small', {}, [isMba(b) ? 'MBA' : 'Non-MBA', regionOf(b)].filter(Boolean).join(' • '))))));
  if (!results.length) list.replaceChildren(h('li', { class: 'menu-note' }, 'No bothies found'));
}

/* ---------- News ---------- */

const NEWS_URL = 'data/news.json';
let newsItems = null;
let newsFetchedAt = null;
let newsLoading = null;

function formatNewsDate(value) {
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return String(value ?? '');
  return d.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC' });
}

function prepareNews(payload) {
  newsFetchedAt = payload.fetched_at ?? null;
  newsItems = payload.items
    .filter((it) => it && typeof it.title === 'string')
    .map((it) => {
      const b = matchBothy(it.title);
      return {
        title: it.title,
        link: safeUrl(it.link),
        date: formatNewsDate(it.date),
        description: typeof it.description === 'string' ? it.description : '',
        bothyId: b ? b.id : null,
        bothyName: b ? b.name : null,
      };
    });
  return newsItems;
}

/**
 * News is collected into data/news.json by a scheduled job, because browsers are
 * not allowed to read the MBA feed directly. Falls back to the last copy we saw.
 */
function getNews(force = false) {
  if (newsItems && !force) return Promise.resolve(newsItems);
  if (newsLoading) return newsLoading;
  newsLoading = (async () => {
    try {
      const res = await fetch(NEWS_URL, { cache: force ? 'reload' : 'no-cache' });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const payload = await res.json();
      if (!payload || !Array.isArray(payload.items)) throw new Error('Unexpected news format');
      newsCache.set(payload);
      return prepareNews(payload);
    } catch (err) {
      const cached = newsCache.get();
      if (cached) return prepareNews(cached);
      throw err;
    } finally {
      newsLoading = null;
    }
  })();
  return newsLoading;
}

function newsRow(item) {
  const body = [
    h('div', { class: 'news-date' }, item.date),
    h('div', { class: 'news-title-row' },
      icon('news'),
      h('div', { class: 'news-title clamp2' }, item.title),
      item.link && icon('external')),
    item.description && h('div', { class: 'news-desc clamp2' }, item.description),
  ];
  const main = item.link
    ? h('a', { class: 'news-link', href: item.link, target: '_blank', rel: 'noopener' }, body)
    : h('div', { class: 'news-link' }, body);
  return h('div', { class: 'news-item' },
    main,
    item.bothyId && h('button', { class: 'news-bothy', onclick: () => go(`#/bothy/${item.bothyId}`) },
      icon('home'), item.bothyName));
}

async function renderNews(force = false) {
  const body = $('news-body');
  const refresh = $('news-refresh');
  if (!newsItems) body.replaceChildren(h('div', { class: 'state' }, h('div', { class: 'spinner' })));
  refresh.classList.add('spinning');
  try {
    const items = await getNews(force);
    if (!items.length) {
      body.replaceChildren(h('div', { class: 'state' }, 'No news available'));
      return;
    }
    const updated = newsFetchedAt ? `Updated ${formatNewsDate(newsFetchedAt)} • ` : '';
    body.replaceChildren(
      ...items.map(newsRow),
      h('div', { class: 'news-footer' }, `${updated}Source: mountainbothies.org.uk`));
    if (force) body.scrollTop = 0;
  } catch (err) {
    body.replaceChildren(h('div', { class: 'state error' }, `Could not load news: ${err.message}`));
  } finally {
    refresh.classList.remove('spinning');
  }
}

/* ---------- Bothy detail ---------- */

let detailId = null;

function formatVisitDate(iso) {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso ?? '');
  if (!m) return iso;
  return new Date(+m[1], +m[2] - 1, +m[3])
    .toLocaleDateString('en-GB', { day: 'numeric', month: 'long', year: 'numeric' });
}

function updateVisitUi() {
  const date = visited.date(detailId);
  const btn = $('btn-visit');
  btn.classList.toggle('is-visited', !!date);
  btn.replaceChildren(icon('check'), date ? 'Visited' : 'Visit');
  btn.setAttribute('aria-pressed', date ? 'true' : 'false');
  const line = $('visited-line');
  if (line) {
    line.hidden = !date;
    line.replaceChildren(icon('check'), `Visited: ${formatVisitDate(date)}`);
  }
}

function setVisited(isoDate) {
  const ok = isoDate ? visited.mark(detailId, isoDate) : visited.unmark(detailId);
  if (!ok) toast('Could not save. Storage may be full or disabled.');
  updateVisitUi();
  renderMarkers();
}

function onVisitClick() {
  if (visited.date(detailId)) {
    setVisited(null);
    return;
  }
  const input = $('visit-date');
  const today = todayIso();
  input.max = today;
  input.value = today;
  const dialog = $('date-dialog');
  dialog.returnValue = '';
  dialog.showModal();
}

function navigateTo(b) {
  const dest = `${b.latitude},${b.longitude}`;
  const url = IS_IOS
    ? `https://maps.apple.com/?daddr=${dest}&dirflg=w`
    : `https://www.google.com/maps/dir/?api=1&destination=${dest}&travelmode=walking`;
  window.open(url, '_blank', 'noopener');
}

async function shareBothy(b) {
  const sep = ' — ';
  let text = b.name;
  if (regionOf(b)) text += sep + regionOf(b);
  if (b.os_grid_ref) text += `${sep}Grid ref: ${b.os_grid_ref}`;
  const url = isMba(b) ? safeUrl(b.mba_url) : null;
  if (url) text += `${sep}More info: ${url}`;

  if (navigator.share) {
    try {
      await navigator.share({ text });
    } catch (err) {
      if (err.name !== 'AbortError') toast('Could not share');
    }
    return;
  }
  try {
    await navigator.clipboard.writeText(text);
    toast('Copied to clipboard');
  } catch {
    toast('Sharing is not available in this browser');
  }
}

function updatesCard(items) {
  return h('section', { class: 'card news' },
    h('h2', { class: 'card-title' }, icon('news'), 'Latest updates'),
    items.map((item) => {
      const body = [
        h('div', { class: 'update-title clamp2' }, item.title),
        h('div', { class: 'update-date' }, item.date),
        item.description && h('div', { class: 'update-desc clamp2' }, item.description),
      ];
      return item.link
        ? h('a', { class: 'update', href: item.link, target: '_blank', rel: 'noopener' }, body)
        : h('div', { class: 'update' }, body);
    }));
}

function renderDetail(id) {
  const b = getBothy(id);
  detailId = b ? id : null;
  const body = $('detail-body');
  $('screen-detail').querySelector('.bottombar').hidden = !b;
  if (!b) {
    $('detail-title').textContent = 'BothyBag';
    body.replaceChildren(h('div', { class: 'state' }, 'Bothy not found'));
    return;
  }
  $('detail-title').textContent = b.name;

  const status = statusBadge(b);
  const rows = [
    ['Region', regionOf(b)],
    ['Elevation', elevationText(b)],
    ['OS Grid Ref', b.os_grid_ref],
    ['Coordinates', `${b.latitude.toFixed(5)}, ${b.longitude.toFixed(5)}`],
    ['Fire/Stove', fireInfo(b)],
    ['Size', sizeInfo(b)],
    ['Features', Array.isArray(b.bothy_features) && b.bothy_features.length ? b.bothy_features.join(' • ') : null],
  ].filter(([, value]) => value != null && value !== '');

  const updatesSlot = h('div', {});
  const note = h('textarea', {
    id: 'note', rows: '2', placeholder: 'e.g. Great spot, avoid in winter', 'aria-label': 'Personal notes',
  });
  note.value = notes.get(id);
  const clear = h('button', { class: 'notes-clear', 'aria-label': 'Clear notes', hidden: !note.value }, icon('close'));
  const autosize = () => {
    note.style.height = 'auto';
    note.style.height = `${note.scrollHeight + 2}px`;
  };
  note.addEventListener('input', () => {
    if (!notes.save(id, note.value)) toast('Could not save. Storage may be full or disabled.');
    clear.hidden = !note.value;
    autosize();
  });
  clear.addEventListener('click', () => {
    note.value = '';
    notes.save(id, '');
    clear.hidden = true;
    autosize();
  });

  const mbaUrl = isMba(b) ? safeUrl(b.mba_url) : null;
  const website = safeUrl(b.website);

  body.replaceChildren(h('div', { class: 'detail' },
    h('div', { class: 'badges' },
      h('span', { class: `badge ${isMba(b) ? 'mba' : 'non-mba'}` }, isMba(b) ? 'MBA' : 'Non-MBA'),
      h('span', { class: `badge ${status.kind}` }, status.text)),
    h('div', { class: 'visited-line', id: 'visited-line', hidden: true }),
    updatesSlot,
    h('section', { class: 'card' },
      h('dl', {}, rows.map(([label, value]) =>
        h('div', { class: 'info-row' }, h('dt', {}, label), h('dd', {}, value))))),
    b.description && h('p', { class: 'description' }, b.description),
    h('div', { class: 'notes' }, h('label', { for: 'note' }, 'Personal notes'), note, clear),
    h('div', { class: 'actions' },
      mbaUrl && h('a', { class: 'btn-outline', href: mbaUrl, target: '_blank', rel: 'noopener' }, icon('globe'), 'MBA Website'),
      !mbaUrl && website && h('a', { class: 'btn-outline', href: website, target: '_blank', rel: 'noopener' }, icon('globe'), 'Website'),
      h('button', { class: 'btn-outline', onclick: () => shareBothy(b) }, icon('share'), 'Share')),
    b.source && h('p', { class: 'source' }, `Source: ${b.source}`)));

  body.scrollTop = 0;
  autosize();
  updateVisitUi();

  getNews().then((items) => {
    const mine = items.filter((it) => it.bothyId === id);
    if (mine.length && detailId === id) updatesSlot.replaceChildren(updatesCard(mine));
  }).catch(() => {});
}

/* ---------- Routing ---------- */

let depth = 0; // how many screens we pushed ourselves, so Back knows whether history has somewhere to go
let currentRoute = { name: 'map' };

function parseRoute() {
  const hash = location.hash.replace(/^#\/?/, '');
  const bothy = /^bothy\/(.+)$/.exec(hash);
  if (bothy) return { name: 'detail', id: decodeURIComponent(bothy[1]) };
  if (hash === 'news') return { name: 'news' };
  return { name: 'map' };
}

function go(hash) {
  if (location.hash === hash) return;
  depth += 1;
  location.hash = hash;
}

function back() {
  if (depth > 0) {
    depth -= 1;
    history.back();
  } else {
    history.replaceState(null, '', location.pathname + location.search);
    applyRoute();
  }
}

function applyRoute() {
  const prev = currentRoute;
  const route = parseRoute();
  currentRoute = route;
  closeMenu();
  if ($('date-dialog').open) $('date-dialog').close('cancel');

  $('screen-detail').hidden = route.name !== 'detail';
  $('screen-news').hidden = route.name !== 'news';
  $('screen-map').inert = route.name !== 'map';

  if (route.name === 'detail') {
    map.closePopup();
    renderDetail(route.id);
  } else if (route.name === 'news') {
    renderNews();
  } else {
    detailId = null;
    map.invalidateSize();
    // Coming back from a bothy: show it on the map, as the Android app does.
    const b = prev.name === 'detail' ? getBothy(prev.id) : null;
    if (b) focusBothy(b, false);
  }
}

/* ---------- Start-up ---------- */

function wireUi() {
  $('btn-menu').innerHTML = iconSvg('search');
  $('btn-news').innerHTML = iconSvg('news');
  $('btn-filter').innerHTML = iconSvg('filter');
  $('btn-locate').innerHTML = iconSvg('locate');
  $('detail-back').innerHTML = iconSvg('back');
  $('news-back').innerHTML = iconSvg('back');
  $('news-refresh').innerHTML = iconSvg('refresh');
  $('btn-navigate').append(icon('navigate'), 'Navigate');

  $('btn-menu').addEventListener('click', () => ($('menu').hidden ? openMenu() : closeMenu()));
  $('menu-backdrop').addEventListener('click', closeMenu);
  $('search').addEventListener('input', runSearch);
  $('btn-dark').addEventListener('click', () => {
    isDarkMap = !isDarkMap;
    prefs.set('dark_map', isDarkMap);
    setTiles();
  });
  $('btn-news').addEventListener('click', () => go('#/news'));
  $('btn-filter').addEventListener('click', cycleFilter);
  $('btn-locate').addEventListener('click', locateMe);

  $('detail-back').addEventListener('click', back);
  $('news-back').addEventListener('click', back);
  $('news-refresh').addEventListener('click', () => renderNews(true));
  $('btn-visit').addEventListener('click', onVisitClick);
  $('btn-navigate').addEventListener('click', () => {
    const b = getBothy(detailId);
    if (b) navigateTo(b);
  });
  $('date-dialog').addEventListener('close', () => {
    const value = $('visit-date').value;
    if ($('date-dialog').returnValue === 'confirm' && detailId && /^\d{4}-\d{2}-\d{2}$/.test(value)) {
      setVisited(value);
    }
  });

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && !$('menu').hidden) closeMenu();
  });
  window.addEventListener('hashchange', applyRoute);

  if (IS_IOS && !IS_STANDALONE) {
    const hint = $('install-hint');
    hint.textContent = 'Install: tap the Share button in Safari, then "Add to Home Screen".';
    hint.hidden = false;
    if (!prefs.get('install_hint_shown', false)) {
      prefs.set('install_hint_shown', true);
      setTimeout(() => toast('Tip: tap Share, then "Add to Home Screen" to install BothyBag'), 2500);
    }
  }
}

function registerServiceWorker() {
  if (!('serviceWorker' in navigator)) return;
  const hadController = !!navigator.serviceWorker.controller;
  navigator.serviceWorker.addEventListener('controllerchange', () => {
    if (hadController) toast('BothyBag was updated.', { label: 'Reload', run: () => location.reload() });
  });
  navigator.serviceWorker.register('sw.js').catch(() => {});
}

async function start() {
  wireUi();
  initMap();
  try {
    await loadBothies();
  } catch (err) {
    toast(`Could not load bothies: ${err.message}`);
  }
  renderMarkers();
  applyRoute();
  resumeLocation();
  getNews().catch(() => {});
  registerServiceWorker();
}

start();
