// Íconos de línea en SVG. Se incluyen en el propio HTML para no pedir nada a
// ningún servidor externo.

const PATHS = {
  folder:
    '<path d="M3.5 7.5a2 2 0 0 1 2-2h4l2 2h7a2 2 0 0 1 2 2v7.5a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2z"/>',
  layers:
    '<path d="m12 3 8.5 4.5L12 12 3.5 7.5 12 3z"/><path d="m3.5 12 8.5 4.5 8.5-4.5"/><path d="m3.5 16.5 8.5 4.5 8.5-4.5"/>',
  list: '<path d="M8 6h12M8 12h12M8 18h12"/><circle cx="4" cy="6" r="1"/><circle cx="4" cy="12" r="1"/><circle cx="4" cy="18" r="1"/>',
  'check-list':
    '<rect x="3.5" y="4" width="17" height="16" rx="3"/><path d="m7 9 1.6 1.6L11.5 7.5"/><path d="m7 15 1.6 1.6 2.9-3.1"/><path d="M14 9h3.5M14 15h3.5"/>',
  tune:
    '<path d="M4 7h10M18 7h2M4 17h2M10 17h10"/><circle cx="16" cy="7" r="2"/><circle cx="8" cy="17" r="2"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  more: '<circle cx="12" cy="5" r="1.3"/><circle cx="12" cy="12" r="1.3"/><circle cx="12" cy="19" r="1.3"/>',
  'chevron-down': '<path d="m6 9 6 6 6-6"/>',
  'chevron-right': '<path d="m9 6 6 6-6 6"/>',
  back: '<path d="M19 12H5"/><path d="m12 19-7-7 7-7"/>',
  check: '<path d="m20 6-11 11-5-5"/>',
  circle: '<circle cx="12" cy="12" r="8.5"/>',
  flag: '<path d="M5.5 21V4"/><path d="M5.5 4.5h11l-2 4 2 4h-11"/>',
  calendar:
    '<rect x="3.5" y="5" width="17" height="15" rx="3"/><path d="M8 3v4M16 3v4M3.5 10h17"/>',
  close: '<path d="M6 6l12 12M18 6 6 18"/>',
  trash:
    '<path d="M4 7h16M9 7V4.5h6V7M6.5 7l1 12.5h9l1-12.5M10 11v5M14 11v5"/>',
  edit: '<path d="M4 20h4l10-10-4-4L4 16z"/><path d="m13.5 6.5 4 4"/>',
  download: '<path d="M12 3v12M7.5 11l4.5 4.5 4.5-4.5"/><path d="M4 20h16"/>',
  upload: '<path d="M12 16V4M7.5 8.5 12 4l4.5 4.5"/><path d="M4 20h16"/>',
  star: '<path d="m12 3.5 2.6 5.4 5.9.8-4.3 4.1 1 5.9L12 17l-5.2 2.7 1-5.9L3.5 9.7l5.9-.8z"/>',
  bolt: '<path d="M13 2.5 5.5 13H11l-1 8.5L18.5 11H13z"/>',
  heart: '<path d="M12 20s-7-4.4-7-9.3A4.1 4.1 0 0 1 12 7.6a4.1 4.1 0 0 1 7 3.1C19 15.6 12 20 12 20z"/>',
  target: '<circle cx="12" cy="12" r="8.5"/><circle cx="12" cy="12" r="4.5"/><circle cx="12" cy="12" r="1"/>',
  book: '<path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v15H6.5A2.5 2.5 0 0 0 4 20.5z"/><path d="M4 20.5A2.5 2.5 0 0 1 6.5 18H20v3H6.5A2.5 2.5 0 0 1 4 20.5z"/>',
  cart: '<path d="M3 4h2.2l2.1 10.2a2 2 0 0 0 2 1.6h8.4a2 2 0 0 0 2-1.6L21 7H6"/><circle cx="10" cy="20" r="1.2"/><circle cx="18" cy="20" r="1.2"/>',
  home: '<path d="m3.5 11 8.5-7 8.5 7"/><path d="M5.5 9.5V20h13V9.5"/><path d="M9.5 20v-6h5v6"/>',
  briefcase:
    '<rect x="3.5" y="7" width="17" height="12.5" rx="2.5"/><path d="M9 7V4.5h6V7M3.5 12h17"/>',
  'status-active': '<circle cx="12" cy="12" r="8.5"/><path d="M10 8.5v7l5-3.5z"/>',
  'status-paused': '<circle cx="12" cy="12" r="8.5"/><path d="M10 9v6M14 9v6"/>',
  'status-postponed':
    '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/>',
  'status-done': '<circle cx="12" cy="12" r="8.5"/><path d="m8 12 2.7 2.7L16.5 9"/>',
  'status-cancelled':
    '<circle cx="12" cy="12" r="8.5"/><path d="m9 9 6 6M15 9l-6 6"/>',
  drag: '<path d="M8 6h8M8 12h8M8 18h8"/>',
  sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M18.7 5.3l-1.4 1.4M6.7 17.3l-1.4 1.4"/>',
  moon: '<path d="M20 14.5A8.5 8.5 0 0 1 9.5 4 8.5 8.5 0 1 0 20 14.5z"/>',
  phone: '<rect x="7" y="2.5" width="10" height="19" rx="2.5"/><path d="M11 5h2M11.5 18.5h1"/>',
  info: '<circle cx="12" cy="12" r="8.5"/><path d="M12 11v5M12 8h.01"/>',
  shield: '<path d="M12 3 19 6v5.5c0 4.5-3 7.8-7 9.5-4-1.7-7-5-7-9.5V6z"/><path d="m9 12 2 2 4-4"/>',
  lock: '<rect x="5" y="10" width="14" height="10" rx="2.5"/><path d="M8.5 10V7.5a3.5 3.5 0 0 1 7 0V10"/>',
};

export const ICON_KEYS = [
  { key: 'layers', label: 'Capas' },
  { key: 'star', label: 'Estrella' },
  { key: 'bolt', label: 'Rayo' },
  { key: 'heart', label: 'Corazón' },
  { key: 'target', label: 'Objetivo' },
  { key: 'book', label: 'Libro' },
  { key: 'cart', label: 'Carrito' },
  { key: 'home', label: 'Casa' },
  { key: 'briefcase', label: 'Maletín' },
];

export function icon(name, className = 'icon') {
  const path = PATHS[name];
  if (!path) return '';
  return `<svg class="${className}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${path}</svg>`;
}

export function projectIconMarkup(project) {
  const value = project.icon;
  if (value && PATHS[value]) return icon(value, 'icon project-icon-svg');
  if (value) return `<span class="emoji-icon" aria-hidden="true">${escapeHtml(value)}</span>`;
  return icon(project.parentId ? 'layers' : 'folder', 'icon project-icon-svg');
}

export function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}
