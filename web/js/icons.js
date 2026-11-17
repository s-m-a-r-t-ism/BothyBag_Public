// Inline SVG icons (stroke style) and the bothy map markers.

const ICONS = {
  search: '<circle cx="11" cy="11" r="7"/><path d="M16.5 16.5L21 21"/>',
  moon: '<path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/>',
  sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/>',
  news: '<rect x="4" y="4" width="16" height="16" rx="2"/><path d="M8 9h8M8 12.5h8M8 16h5"/>',
  back: '<path d="M19 12H5M12 19l-7-7 7-7"/>',
  locate: '<circle cx="12" cy="12" r="7"/><circle cx="12" cy="12" r="2.5" class="solid"/><path d="M12 2v3M12 19v3M2 12h3M19 12h3"/>',
  filter: '<path d="M3 6h18M7 12h10M10 18h4"/>',
  check: '<circle cx="12" cy="12" r="9"/><path d="M8 12.5l2.5 2.5L16 9.5"/>',
  navigate: '<path d="M12 2l7 19-7-4-7 4z" class="solid"/>',
  globe: '<circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3a14 14 0 0 1 0 18a14 14 0 0 1 0-18"/>',
  share: '<path d="M12 3v12M8 7l4-4 4 4M5 12v7a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-7"/>',
  close: '<path d="M6 6l12 12M18 6L6 18"/>',
  home: '<path d="M3 11l9-8 9 8M5 10v10h14V10"/>',
  external: '<path d="M14 4h6v6M20 4l-9 9M18 14v5a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V7a1 1 0 0 1 1-1h5"/>',
  refresh: '<path d="M20 11a8 8 0 1 0-2.3 6.3M20 4v7h-7"/>',
  chevron: '<path d="M9 6l6 6-6 6"/>',
};

export function iconSvg(name) {
  return `<svg class="ico" viewBox="0 0 24 24" aria-hidden="true">${ICONS[name]}</svg>`;
}

/** Returns a detached SVG element for the named icon. */
export function icon(name) {
  const t = document.createElement('template');
  t.innerHTML = iconSvg(name);
  return t.content.firstChild;
}

// Marker artwork, converted from the Android vector drawables
// (ic_bothy_mba.xml, ic_bothy_other.xml, ic_bothy_closed.xml).
const HOUSE = {
  mba:
    '<path fill="#FFB300" d="M6 16L16 6l10 10v12H6z"/>' +
    '<path fill="#FF8F00" d="M4 17L16 5l12 12h-2L16 7 6 17z"/>' +
    '<path fill="#5D4037" d="M13 20h6v8h-6z"/>' +
    '<path fill="#BBDEFB" d="M9 18h3v4H9z"/>' +
    '<path fill="#795548" d="M21 8h3v6l-3-3z"/>',
  other:
    '<path fill="#78909C" d="M6 16L16 6l10 10v12H6z"/>' +
    '<path fill="#546E7A" d="M4 17L16 5l12 12h-2L16 7 6 17z"/>' +
    '<path fill="#37474F" d="M13 20h6v8h-6z"/>' +
    '<path fill="#B0BEC5" d="M9 18h3v4H9z"/>',
  closed:
    '<path fill="#9E9E9E" fill-opacity=".5" d="M6 16L16 6l10 10v12H6z"/>' +
    '<path fill="#757575" fill-opacity=".5" d="M4 17L16 5l12 12h-2L16 7 6 17z"/>' +
    '<path fill="none" stroke="#E53935" stroke-width="2" d="M10 14l12 12M22 14L10 26"/>',
};

// Green ring plus a check badge, shown on visited bothies.
const VISITED =
  '<circle cx="16" cy="16" r="14.4" fill="none" stroke="#4CAF50" stroke-width="2.4"/>' +
  '<circle cx="25" cy="25" r="6" fill="#4CAF50" stroke="#FFF" stroke-width="1.2"/>' +
  '<path fill="none" stroke="#FFF" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" d="M22.2 25.1l2 2 3.6-3.9"/>';

export function markerSvg(kind, visited) {
  const overlay = visited && kind !== 'closed' ? VISITED : '';
  return `<svg viewBox="0 0 32 32" aria-hidden="true">${HOUSE[kind]}${overlay}</svg>`;
}
