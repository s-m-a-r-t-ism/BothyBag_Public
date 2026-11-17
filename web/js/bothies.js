// Bothy data: loading bothies.json and the derived fields from the Android Bothy data class.

let bothies = [];
let byId = new Map();

export async function loadBothies() {
  const res = await fetch('data/bothies.json');
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const raw = await res.json();
  bothies = (Array.isArray(raw) ? raw : [])
    .filter((b) => b && typeof b.id === 'string' && Number.isFinite(b.latitude) && Number.isFinite(b.longitude))
    .map((b) => ({ ...b, name: String(b.name ?? b.id) }));
  byId = new Map(bothies.map((b) => [b.id, b]));
  return bothies;
}

export const allBothies = () => bothies;
export const getBothy = (id) => byId.get(id) ?? null;

export const isMba = (b) => b.type === 'MBA';
const statusOf = (b) => String(b.status ?? 'open').toLowerCase();
export const isOpen = (b) => statusOf(b) === 'open';
export const isClosed = (b) => ['closed', 'permanently_closed'].includes(statusOf(b));

const accessOf = (b) => String(b.access ?? '').trim().toLowerCase();

/** Not freely open to walkers: private, paying customers only, or no access. */
export const isPrivate = (b) => ['private', 'customers', 'no'].includes(accessOf(b));

/** Readable access text for the detail screen. */
export function accessInfo(b) {
  const raw = String(b.access ?? '').trim();
  if (!raw) return null;
  return {
    private: 'Private, no public access',
    customers: 'Paying customers only',
    no: 'No access',
    permissive: 'Open by permission of the owner',
    yes: 'Public',
  }[raw.toLowerCase()] ?? raw;
}

export function regionOf(b) {
  return b.region && b.region !== 'Unknown' ? b.region : null;
}

export function statusBadge(b) {
  const s = statusOf(b);
  if (s === 'open') return { text: 'Open', kind: 'open' };
  if (s === 'closed') return { text: 'Closed', kind: 'closed' };
  if (s === 'permanently_closed') return { text: 'Permanently closed', kind: 'closed' };
  if (s === 'seasonal') return { text: 'Seasonal', kind: 'seasonal' };
  const text = s.replace(/_/g, ' ');
  return { text: text.charAt(0).toUpperCase() + text.slice(1), kind: 'other' };
}

export function fireInfo(b) {
  if (b.fire) return b.fire;
  if (!b.fireplace) return null;
  const f = String(b.fireplace).toLowerCase();
  if (f === 'yes') return 'Fireplace available';
  if (f === 'no') return 'No fireplace';
  return String(b.fireplace);
}

export function sizeInfo(b) {
  if (b.size) return b.size;
  return b.capacity ? `Capacity: ${b.capacity}` : null;
}

export function elevationText(b) {
  if (b.elevation_m == null) return null;
  return `${b.elevation_m}m (${Math.trunc(b.elevation_m * 3.28084)}ft)`;
}

/** Only http(s) links are ever opened. */
export function safeUrl(url) {
  if (typeof url !== 'string') return null;
  try {
    const u = new URL(url.trim());
    return u.protocol === 'https:' || u.protocol === 'http:' ? u.href : null;
  } catch {
    return null;
  }
}

/** Lowercase, strip apostrophes and special characters, collapse whitespace. */
export function normalize(text) {
  return String(text)
    .toLowerCase()
    .replace(/['`‘’‚‛]/g, '')
    .replace(/[^a-z0-9\s]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

// Bothy names that are everyday words or place names once "Bothy" is dropped.
// These only match when the full name is in the title.
const AMBIGUOUS_SHORT_NAMES = new Set(['green', 'oban']);

let lookup = null;

// Same rules as NewsMatcher.kt in the Android app.
function nameLookup() {
  if (lookup) return lookup;
  lookup = [];
  // Only MBA bothies: the feed is the MBA's own news, and names such as
  // "Red Bothy" caused false matches.
  for (const b of bothies.filter(isMba)) {
    const full = normalize(b.name);
    if (full) lookup.push([full, b]);
    const stripped = normalize(b.name.replace(/\s+bothy$/i, ''));
    if (stripped && stripped !== full && !AMBIGUOUS_SHORT_NAMES.has(stripped)) lookup.push([stripped, b]);
  }
  // Longest names first, so "Glen Dubh Lighe" wins over "Glen".
  lookup.sort((a, b) => b[0].length - a[0].length);
  return lookup;
}

/** Finds the bothy whose name appears in a news title, or null. */
export function matchBothy(title) {
  const t = ` ${normalize(title)} `;
  for (const [name, b] of nameLookup()) {
    // The second check covers possessives: "Shenavall's" normalizes to "shenavalls".
    if (t.includes(` ${name} `) || t.includes(` ${name}s `)) return b;
  }
  return null;
}
