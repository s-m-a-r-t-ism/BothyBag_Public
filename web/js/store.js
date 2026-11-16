// Everything the user creates lives in localStorage on their own device.
// Keys mirror the Android SharedPreferences files (visited_bothies, bothy_notes, map_prefs).

const VISITED_KEY = 'visited_map';
const NOTE_PREFIX = 'note_';
const PREFS_KEY = 'map_prefs';
const NEWS_KEY = 'news_cache';

function read(key, fallback) {
  try {
    const raw = localStorage.getItem(key);
    if (raw == null) return fallback;
    const value = JSON.parse(raw);
    return value == null ? fallback : value;
  } catch {
    return fallback;
  }
}

let askedForPersistence = false;

function write(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    return false;
  }
  // Once the user has saved something, ask the browser not to evict it.
  if (!askedForPersistence && key !== NEWS_KEY) {
    askedForPersistence = true;
    navigator.storage?.persist?.().catch(() => {});
  }
  return true;
}

/** Today's date in local time as yyyy-MM-dd. */
export function todayIso() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export const visited = {
  /** Map of bothyId -> date string (yyyy-MM-dd). */
  map() {
    const m = read(VISITED_KEY, {});
    return m && typeof m === 'object' && !Array.isArray(m) ? m : {};
  },
  date(id) {
    return this.map()[id] ?? null;
  },
  mark(id, isoDate) {
    const m = this.map();
    m[id] = isoDate || todayIso();
    return write(VISITED_KEY, m);
  },
  unmark(id) {
    const m = this.map();
    delete m[id];
    return write(VISITED_KEY, m);
  },
};

export const notes = {
  get(id) {
    const v = read(NOTE_PREFIX + id, '');
    return typeof v === 'string' ? v : '';
  },
  save(id, text) {
    if (text) return write(NOTE_PREFIX + id, text);
    try {
      localStorage.removeItem(NOTE_PREFIX + id);
    } catch {
      return false;
    }
    return true;
  },
};

export const prefs = {
  get(name, fallback) {
    const p = read(PREFS_KEY, {});
    return name in p ? p[name] : fallback;
  },
  set(name, value) {
    const p = read(PREFS_KEY, {});
    p[name] = value;
    write(PREFS_KEY, p);
  },
};

export const newsCache = {
  get() {
    const c = read(NEWS_KEY, null);
    return c && Array.isArray(c.items) ? c : null;
  },
  set(payload) {
    write(NEWS_KEY, payload);
  },
};
