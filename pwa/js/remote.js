// Modo remoto. Solo se carga cuando la página la sirve el celular desde
// Ajustes → Sesión web. Es la misma PWA, pero en vez de IndexedDB habla con la
// app Android: el celular guarda todo y acá solo se muestra y se edita.
//
// Si el archivo se carga fuera de una sesión (no hay servidor), no hace nada.

import { decodeBackup, encodeBackup } from './backup.js';

const API = '/api';
const CODE_KEY = 'list_master_remote_code';
const POLL_MS = 2000;

let code = '';
let version = 0;
let polling = null;
let offlineBadge = null;

function withCode(path) {
  return `${API}/${path}${path.includes('?') ? '&' : '?'}code=${encodeURIComponent(code)}`;
}

async function readState() {
  const response = await fetch(withCode('state'), { cache: 'no-store' });
  if (response.status === 403) throw new Error('Código inválido o sesión cerrada.');
  if (!response.ok) throw new Error('No pudimos leer la sesión.');
  return response.json();
}

async function verifySession(candidate) {
  const response = await fetch(`${API}/session?code=${encodeURIComponent(candidate)}`, {
    cache: 'no-store',
  });
  return response.ok;
}

function showOffline(show) {
  if (show && !offlineBadge) {
    offlineBadge = document.createElement('div');
    offlineBadge.className = 'remote-offline';
    offlineBadge.setAttribute('role', 'status');
    offlineBadge.textContent = 'Sin conexión con el celular';
    document.body.appendChild(offlineBadge);
  } else if (!show && offlineBadge) {
    offlineBadge.remove();
    offlineBadge = null;
  }
}

function dispatchUpdate() {
  globalThis.dispatchEvent(new CustomEvent('lm-remote-updated'));
}

function startPolling() {
  if (polling) return;
  polling = setInterval(async () => {
    try {
      const payload = await readState();
      showOffline(false);
      if (payload.version !== version) {
        version = payload.version;
        dispatchUpdate();
      }
    } catch {
      showOffline(true);
    }
  }, POLL_MS);
}

const remote = {
  async load() {
    const payload = await readState();
    version = payload.version;
    showOffline(false);
    startPolling();
    return decodeBackup(JSON.stringify(payload.datos));
  },

  async save(data) {
    const body = { version, datos: JSON.parse(encodeBackup(data)) };
    try {
      const response = await fetch(withCode('state'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      if (response.status === 403) {
        showOffline(true);
        throw new Error('La sesión web se cerró.');
      }
      if (response.status === 409) {
        // Otro dispositivo guardó primero: se toma lo del celular.
        const current = await response.json();
        version = current.version;
        dispatchUpdate();
        return;
      }
      if (!response.ok) throw new Error('No pudimos guardar en el celular.');
      const payload = await response.json();
      version = payload.version;
      showOffline(false);
    } catch (error) {
      showOffline(true);
      throw error;
    }
  },
};

function mountGate(message = '') {
  const layer = document.createElement('div');
  layer.className = 'remote-gate';
  layer.innerHTML = `
    <form class="remote-gate-card" autocomplete="off">
      <h1>List Master</h1>
      <p>Sesión web del celular. Escribí el código que muestra la app.</p>
      <label class="field">
        <span class="field-label">Código</span>
        <input class="input remote-code" name="code" inputmode="numeric" autocomplete="off" maxlength="6" placeholder="123456" aria-label="Código de la sesión">
      </label>
      <p class="remote-gate-error" role="alert">${message}</p>
      <button type="submit" class="primary-button">Entrar</button>
    </form>`;
  document.body.appendChild(layer);
  const form = layer.querySelector('form');
  const input = layer.querySelector('input');
  const errorLine = layer.querySelector('.remote-gate-error');
  requestAnimationFrame(() => input.focus());
  return new Promise((resolve) => {
    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      const candidate = input.value.replace(/\D/g, '').trim();
      if (candidate.length < 4) {
        errorLine.textContent = 'El código tiene 6 números.';
        return;
      }
      input.disabled = true;
      try {
        if (!(await verifySession(candidate))) {
          errorLine.textContent = 'Ese código no está activo.';
          input.disabled = false;
          input.select();
          return;
        }
        code = candidate;
        layer.remove();
        resolve(candidate);
      } catch {
        errorLine.textContent = 'No llegamos al celular. Revisá que estén en la misma red.';
        input.disabled = false;
        input.select();
      }
    });
  });
}

async function boot() {
  const fromUrl = new URLSearchParams(location.search).get('code') || '';
  let candidate = (fromUrl || localStorage.getItem(CODE_KEY) || '').replace(/\D/g, '').trim();
  if (candidate && !(await verifySession(candidate).catch(() => false))) candidate = '';
  if (!candidate) {
    candidate = await mountGate(fromUrl ? 'Ese código ya no sirve.' : '');
  }
  code = candidate;
  try {
    localStorage.setItem(CODE_KEY, code);
  } catch {
    // Sin localStorage igual funciona mientras dure la pestaña.
  }
  globalThis.LM_REMOTE = remote;
}

await boot();
