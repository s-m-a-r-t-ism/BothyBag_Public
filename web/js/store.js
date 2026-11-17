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

const DATE_RE = /^(\d{4})-(\d{2})-(\d{2})$/;

function isValidDate(value) {
  const m = DATE_RE.exec(value);
  if (!m) return false;
  const d = new Date(Date.UTC(+m[1], +m[2] - 1, +m[3]));
  return d.getUTCFullYear() === +m[1] && d.getUTCMonth() === +m[2] - 1 && d.getUTCDate() === +m[3];
}

function allNotes() {
  const out = {};
  try {
    for (let i = 0; i < localStorage.length; i += 1) {
      const key = localStorage.key(i);
      if (!key || !key.startsWith(NOTE_PREFIX)) continue;
      const text = read(key, '');
      if (typeof text === 'string' && text.trim()) out[key.slice(NOTE_PREFIX.length)] = text;
    }
  } catch {
    // storage unavailable: nothing to export
  }
  return out;
}

/**
 * Backup of visited bothies and notes. Same file format as the Android app
 * (UserDataBackup.kt), so a backup can move between phone and web app.
 */
export const backup = {
  exportJson() {
    return JSON.stringify({
      app: 'BothyBag',
      version: 1,
      exported: todayIso(),
      visited: visited.map(),
      notes: allNotes(),
    });
  },

  /**
   * Merges a backup: entries from the file are added and replace the entry for the
   * same bothy. Nothing is removed. Returns null when the text is not a BothyBag backup.
   */
  importJson(text) {
    let data;
    try {
      data = JSON.parse(text);
    } catch {
      return null;
    }
    if (!data || typeof data !== 'object' || Array.isArray(data)) return null;
    const isMap = (v) => v && typeof v === 'object' && !Array.isArray(v);
    if (!isMap(data.visited) && !isMap(data.notes)) return null;

    const newVisits = {};
    for (const [id, date] of Object.entries(isMap(data.visited) ? data.visited : {})) {
      if (id.trim() && typeof date === 'string' && isValidDate(date)) newVisits[id] = date;
    }
    const newNotes = {};
    for (const [id, note] of Object.entries(isMap(data.notes) ? data.notes : {})) {
      if (id.trim() && typeof note === 'string' && note.trim()) newNotes[id] = note;
    }

    let ok = write(VISITED_KEY, { ...visited.map(), ...newVisits });
    for (const [id, note] of Object.entries(newNotes)) ok = write(NOTE_PREFIX + id, note) && ok;
    if (!ok) return { error: 'Could not save. Storage may be full or disabled.' };
    return { visitedCount: Object.keys(newVisits).length, notesCount: Object.keys(newNotes).length };
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
