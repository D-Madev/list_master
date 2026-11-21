// Persistencia local. Los datos van a IndexedDB; los ajustes chicos, a
// localStorage. Nada de esto sale del dispositivo.

const DB_NAME = 'list_master_pwa';
const DB_VERSION = 1;
const STORE = 'kv';
const DATA_KEY = 'data';
const SETTINGS_KEY = 'list_master_pwa_settings';

let dbPromise = null;

function openDb() {
  if (!('indexedDB' in globalThis)) return Promise.reject(new Error('IndexedDB no disponible.'));
  if (dbPromise) return dbPromise;
  dbPromise = new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);
    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(STORE)) db.createObjectStore(STORE);
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error || new Error('No se pudo abrir la base.'));
  });
  return dbPromise;
}

async function withStore(mode, action) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const transaction = db.transaction(STORE, mode);
    const store = transaction.objectStore(STORE);
    let result;
    try {
      result = action(store);
    } catch (error) {
      reject(error);
      return;
    }
    transaction.oncomplete = () => resolve(result);
    transaction.onerror = () => reject(transaction.error || new Error('No se pudo guardar.'));
    transaction.onabort = () => reject(transaction.error || new Error('No se pudo guardar.'));
  });
}

export const EMPTY_DATA = { projects: [], tasks: [], lists: [] };

export async function loadData() {
  // En sesión web los datos viven en el celular, no acá.
  const bridge = globalThis.LM_REMOTE;
  if (bridge) return bridge.load();
  try {
    const db = await openDb();
    const value = await new Promise((resolve, reject) => {
      const transaction = db.transaction(STORE, 'readonly');
      const request = transaction.objectStore(STORE).get(DATA_KEY);
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
    if (value && typeof value === 'object') return normalizeStored(value);
  } catch (error) {
    console.warn('No se pudo leer IndexedDB; se prueba localStorage.', error);
  }
  try {
    const raw = localStorage.getItem(DATA_KEY);
    if (raw) return normalizeStored(JSON.parse(raw));
  } catch (error) {
    console.warn('No se pudo leer el respaldo local.', error);
  }
  return { ...EMPTY_DATA };
}

export async function saveData(data) {
  const bridge = globalThis.LM_REMOTE;
  if (bridge) {
    await bridge.save(data);
    return;
  }
  const value = {
    projects: data.projects,
    tasks: data.tasks,
    lists: data.lists,
  };
  try {
    await withStore('readwrite', (store) => store.put(value, DATA_KEY));
    // El espejo en localStorage es solo una red por si IndexedDB falla.
    try {
      localStorage.setItem(DATA_KEY, JSON.stringify(value));
    } catch {
      // Sin espacio: alcanza con IndexedDB.
    }
  } catch (error) {
    console.warn('IndexedDB falló; se guarda en localStorage.', error);
    localStorage.setItem(DATA_KEY, JSON.stringify(value));
  }
}

export function loadSettings(defaults) {
  try {
    const raw = localStorage.getItem(SETTINGS_KEY);
    if (!raw) return { ...defaults };
    const parsed = JSON.parse(raw);
    return { ...defaults, ...parsed };
  } catch {
    return { ...defaults };
  }
}

export function saveSettings(settings) {
  try {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings));
  } catch (error) {
    console.warn('No se pudieron guardar los ajustes.', error);
  }
}

function normalizeStored(value) {
  return {
    projects: Array.isArray(value.projects) ? value.projects : [],
    tasks: Array.isArray(value.tasks) ? value.tasks : [],
    lists: Array.isArray(value.lists) ? value.lists : [],
  };
}
