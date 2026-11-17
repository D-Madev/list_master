// List Master PWA. Toda la lógica vive acá: estado, ruteo, eventos y hojas.
// No se hace ninguna petición de red después de cargar los archivos.

import {
  ALL_PRIORITIES,
  DEFAULT_STATUSES,
  DomainError,
  MAX_ITEM_TITLE_LENGTH,
  MAX_SUBPROJECT_DEPTH,
  PRIORITIES,
  PROJECT_STATUSES,
  canHaveSublist,
  canHaveSubproject,
  flattenLists,
  flattenProjects,
  isClosed,
  listDepth,
  normalizeDescription,
  normalizeIcon,
  normalizeTitle,
  nowIso,
  newId,
  priorityMeta,
  projectDepth,
  subtreeIds,
  validListParents,
  validProjectParents,
} from './domain.js';
import { decodeBackup, encodeBackup } from './backup.js';
import { loadData, loadSettings, saveData, saveSettings } from './store.js';
import { ICON_KEYS, escapeHtml, icon } from './icons.js';
import { formatLongDate, needsNav, renderNav, renderScreen } from './views.js';

const APP_VERSION = '1.8.0';
const DEFAULT_SETTINGS = {
  theme: 'system',
  showDoneTasks: false,
  showDoneListItems: false,
  filter: {
    statuses: [...DEFAULT_STATUSES],
    priorities: [...ALL_PRIORITIES],
  },
};

let data = { projects: [], tasks: [], lists: [] };
let settings = cloneSettings(DEFAULT_SETTINGS);
let route = parseRoute(location.hash);
let ui = {
  expandedProjects: new Set(),
  expandedLists: new Set(),
  collapsedLists: new Set(),
  addingProjectId: null,
};

const sheets = [];
let saveTimer = null;
let toastTimer = null;
let toastClearTimer = null;
let toastAction = null;
let suppressClick = false;
let dragState = null;

const appRoot = document.getElementById('app');
const navRoot = document.getElementById('nav-root');
const overlayRoot = document.getElementById('overlay-root');
const toastRoot = document.getElementById('toast-root');
const importInput = document.getElementById('import-input');

// Arranque

async function boot() {
  data = await loadData();
  settings = normalizeSettings(loadSettings(DEFAULT_SETTINGS));
  applyTheme();
  bindEvents();
  if (!location.hash) {
    history.replaceState(null, '', '#/pending');
    route = parseRoute(location.hash);
  }
  render();
  registerServiceWorker();
}

// Ruteo

function parseRoute(hash) {
  const path = String(hash || '').replace(/^#\/?/, '').split('?')[0];
  const [name, id] = path.split('/');
  if (name === 'project' && id) return { name: 'project', id: decodeURIComponent(id) };
  if (name === 'list' && id) return { name: 'list', id: decodeURIComponent(id) };
  if (['pending', 'projects', 'lists', 'settings'].includes(name)) return { name };
  return { name: 'pending' };
}

function navigate(name, id = null) {
  closeAllSheets();
  const target = `#/${name}${id ? `/${encodeURIComponent(id)}` : ''}`;
  if (location.hash === target) {
    route = parseRoute(target);
    render();
    return;
  }
  location.hash = target;
}

function handleHashChange() {
  route = parseRoute(location.hash);
  closeAllSheets();
  render();
}

// Render

function render({ preserveScroll = false } = {}) {
  const scroll = preserveScroll ? window.scrollY : 0;
  const ctx = { data, settings, ui, route, version: APP_VERSION };
  appRoot.innerHTML = renderScreen(ctx);
  navRoot.innerHTML = needsNav(route) ? renderNav(route) : '';
  document.body.classList.toggle('has-nav', needsNav(route));
  document.body.dataset.route = route.name;
  document.title = routeTitle(route);
  window.scrollTo(0, scroll);
}

function routeTitle(current) {
  if (current.name === 'project') {
    const project = findProject(current.id);
    return project ? `${project.title} · List Master` : 'List Master';
  }
  if (current.name === 'list') {
    const list = findList(current.id);
    return list ? `${list.title} · List Master` : 'List Master';
  }
  const titles = {
    pending: 'Pendientes',
    projects: 'Mis Proyectos',
    lists: 'Mis Listas',
    settings: 'Ajustes',
  };
  return `${titles[current.name] || 'List Master'} · List Master`;
}

function renderOverlay({ focus = false } = {}) {
  overlayRoot.innerHTML = sheets
    .map((sheet, index) => renderSheet(sheet, index))
    .join('');
  document.body.classList.toggle('sheet-open', sheets.length > 0);
  if (focus && sheets.length) {
    const top = sheets[sheets.length - 1];
    if (top && top.focusField !== false) {
      requestAnimationFrame(() => {
        const input = overlayRoot.querySelector('.sheet-layer:last-child [data-autofocus]');
        if (input && typeof input.focus === 'function') input.focus();
      });
    }
  }
}

// Hojas

function pushSheet(sheet) {
  hideToast();
  sheets.push(sheet);
  renderOverlay({ focus: true });
}

function popSheet() {
  sheets.pop();
  renderOverlay();
}

function closeAllSheets() {
  if (!sheets.length) return;
  sheets.length = 0;
  renderOverlay();
}

function renderSheet(sheet, index) {
  switch (sheet.type) {
    case 'task':
      return renderTaskSheet(sheet, index);
    case 'project':
      return renderProjectSheet(sheet, index);
    case 'list':
      return renderListSheet(sheet, index);
    case 'item':
      return renderItemSheet(sheet, index);
    case 'project-menu':
      return renderProjectMenu(sheet, index);
    case 'list-menu':
      return renderListMenu(sheet, index);
    case 'status-picker':
      return renderStatusPicker(sheet, index);
    case 'priority-picker':
      return renderPriorityPicker(sheet, index);
    case 'project-picker':
      return renderProjectPicker(sheet, index);
    case 'list-picker':
      return renderListPicker(sheet, index);
    case 'icon-picker':
      return renderIconPicker(sheet, index);
    case 'filters':
      return renderFilters(sheet, index);
    case 'confirm':
      return renderConfirm(sheet, index);
    default:
      return '';
  }
}

function sheetShell(index, { title, body, footer = '', form = null }) {
  const tag = form ? 'form' : 'div';
  const attrs = form ? ` data-sheet-form="${form}" novalidate` : '';
  return `<div class="sheet-layer" style="z-index:${100 + index}" data-sheet-layer="${index}">
    <div class="scrim" data-action="close-sheet" data-layer="${index}"></div>
    <${tag} class="sheet"${attrs} role="dialog" aria-modal="true" aria-label="${escapeHtml(title)}">
      <div class="sheet-grab" aria-hidden="true"></div>
      <header class="sheet-head">
        <h2>${escapeHtml(title)}</h2>
        <button type="button" class="icon-button" data-action="close-sheet" data-layer="${index}" aria-label="Cerrar">${icon('close')}</button>
      </header>
      <div class="sheet-body">${body}</div>
      ${footer ? `<footer class="sheet-foot">${footer}</footer>` : ''}
    </${tag}>
  </div>`;
}

function renderTaskSheet(sheet, index) {
  const draft = sheet.draft;
  const task = sheet.taskId ? findTask(sheet.taskId) : null;
  const project = findProject(draft.projectId);
  const title = task ? 'Editar Tarea' : 'Nueva Tarea';
  const dateText = draft.dueDate ? formatLongDate(draft.dueDate) : 'Sin Fecha';
  const body = `
    <label class="field">
      <span class="field-label">Título</span>
      <input class="input" name="title" data-autofocus value="${escapeHtml(draft.title)}" maxlength="120" autocomplete="off" autocapitalize="sentences" enterkeyhint="next" placeholder="Comprar filtros">
    </label>
    <label class="field">
      <span class="field-label">Descripción <span class="optional">Opcional</span></span>
      <textarea class="input textarea" name="description" maxlength="2000" rows="3" autocapitalize="sentences" placeholder="Un detalle que no querés olvidar">${escapeHtml(draft.description)}</textarea>
    </label>
    <div class="field">
      <span class="field-label">Prioridad</span>
      <div class="option-row">${priorityOptions('set-task-priority', draft.priority)}</div>
    </div>
    <div class="field">
      <span class="field-label">Fecha <span class="optional">Opcional</span></span>
      <div class="date-row">
        <span class="date-text ${draft.dueDate ? '' : 'muted'}">${icon('calendar', 'tiny')}${escapeHtml(dateText)}</span>
        ${draft.dueDate ? `<button type="button" class="icon-button small" data-action="clear-task-date" aria-label="Quitar Fecha">${icon('close', 'tiny')}</button>` : ''}
        <input class="date-input" type="date" name="dueDate" value="${escapeHtml(draft.dueDate || '')}" aria-label="Elegir Fecha">
      </div>
    </div>
    <div class="field">
      <span class="field-label">Proyecto</span>
      <button type="button" class="selector-row" data-action="open-task-project">
        <span class="selector-icon">${icon('layers', 'small-icon')}</span>
        <span class="selector-main"><span class="selector-value">${escapeHtml(project ? project.title : 'Elegir Proyecto')}</span></span>
        ${icon('chevron-right', 'chevron muted')}
      </button>
      ${task ? '<p class="help">Podés moverla a otro proyecto sin recrearla.</p>' : ''}
    </div>
    ${sheet.error ? `<p class="form-error" role="alert">${escapeHtml(sheet.error)}</p>` : ''}
  `;
  const footer = `<button type="submit" class="primary-button">Guardar</button>
    ${task ? `<button type="button" class="text-button danger" data-action="request-delete-task">Eliminar</button>` : ''}`;
  return sheetShell(index, { title, body, footer, form: 'task' });
}

function renderProjectSheet(sheet, index) {
  const draft = sheet.draft;
  const project = sheet.projectId ? findProject(sheet.projectId) : null;
  const parent = draft.parentId ? findProject(draft.parentId) : null;
  const title = project ? 'Editar Proyecto' : draft.parentId ? 'Nuevo Subproyecto' : 'Nuevo Proyecto';
  const emojiValue = draft.icon && !ICON_KEYS.some((entry) => entry.key === draft.icon) ? draft.icon : '';
  const body = `
    <label class="field">
      <span class="field-label">Título</span>
      <input class="input" name="title" data-autofocus value="${escapeHtml(draft.title)}" maxlength="120" autocomplete="off" autocapitalize="sentences" enterkeyhint="next" placeholder="Diseño Web">
    </label>
    <div class="field">
      <span class="field-label">Ícono</span>
      <div class="icon-grid">
        <button type="button" class="icon-choice ${draft.icon ? '' : 'is-selected'}" data-action="pick-icon" data-icon="" aria-label="Predeterminado" aria-pressed="${draft.icon ? 'false' : 'true'}">${icon(draft.parentId ? 'layers' : 'folder')}</button>
        ${ICON_KEYS.map(
          (entry) => `<button type="button" class="icon-choice ${draft.icon === entry.key ? 'is-selected' : ''}" data-action="pick-icon" data-icon="${entry.key}" aria-label="${entry.label}" aria-pressed="${draft.icon === entry.key ? 'true' : 'false'}">${icon(entry.key)}</button>`,
        ).join('')}
      </div>
      <label class="emoji-field">
        <span>O usá un emoji</span>
        <input class="input emoji-input" name="iconEmoji" value="${escapeHtml(emojiValue)}" maxlength="16" autocomplete="off" placeholder="🚀" aria-label="Emoji del proyecto">
      </label>
    </div>
    <div class="field">
      <span class="field-label">Estado</span>
      <div class="option-row">${statusOptions('set-project-status-draft', draft.status)}</div>
    </div>
    <div class="field">
      <span class="field-label">Prioridad</span>
      <div class="option-row">${priorityOptions('set-project-priority-draft', draft.priority)}</div>
    </div>
    <div class="field">
      <span class="field-label">Proyecto Padre</span>
      <button type="button" class="selector-row" data-action="open-project-parent">
        <span class="selector-icon">${icon('layers', 'small-icon')}</span>
        <span class="selector-main"><span class="selector-value">${escapeHtml(parent ? parent.title : 'Sin Proyecto Padre')}</span></span>
        ${icon('chevron-right', 'chevron muted')}
      </button>
      <p class="help">Los subproyectos pueden anidarse hasta tres niveles.</p>
    </div>
    ${sheet.error ? `<p class="form-error" role="alert">${escapeHtml(sheet.error)}</p>` : ''}
  `;
  const footer = `<button type="submit" class="primary-button">Guardar</button>
    ${project ? `<button type="button" class="text-button danger" data-action="request-delete-project">Eliminar</button>` : ''}`;
  return sheetShell(index, { title, body, footer, form: 'project' });
}

function renderListSheet(sheet, index) {
  const draft = sheet.draft;
  const list = sheet.listId ? findList(sheet.listId) : null;
  const project = draft.projectId ? findProject(draft.projectId) : null;
  const parent = draft.parentId ? findList(draft.parentId) : null;
  const title = list ? 'Editar Lista' : draft.parentId ? 'Nueva Sublista' : 'Nueva Lista';
  const body = `
    <label class="field">
      <span class="field-label">Título</span>
      <input class="input" name="title" data-autofocus value="${escapeHtml(draft.title)}" maxlength="120" autocomplete="off" autocapitalize="sentences" enterkeyhint="next" placeholder="Compras">
    </label>
    <div class="field">
      <span class="field-label">Proyecto Vinculado <span class="optional">Opcional</span></span>
      <button type="button" class="selector-row" data-action="open-list-project">
        <span class="selector-icon">${icon('folder', 'small-icon')}</span>
        <span class="selector-main"><span class="selector-value">${escapeHtml(project ? project.title : 'Sin Proyecto')}</span></span>
        ${icon('chevron-right', 'chevron muted')}
      </button>
    </div>
    <div class="field">
      <span class="field-label">Lista Padre</span>
      <button type="button" class="selector-row" data-action="open-list-parent">
        <span class="selector-icon">${icon('list', 'small-icon')}</span>
        <span class="selector-main"><span class="selector-value">${escapeHtml(parent ? parent.title : 'Lista Principal')}</span></span>
        ${icon('chevron-right', 'chevron muted')}
      </button>
      <p class="help">Las listas pueden anidarse hasta tres niveles.</p>
    </div>
    ${sheet.error ? `<p class="form-error" role="alert">${escapeHtml(sheet.error)}</p>` : ''}
  `;
  const footer = `<button type="submit" class="primary-button">Guardar</button>
    ${list ? `<button type="button" class="text-button danger" data-action="request-delete-list">Eliminar</button>` : ''}`;
  return sheetShell(index, { title, body, footer, form: 'list' });
}

function renderItemSheet(sheet, index) {
  const draft = sheet.draft;
  const body = `
    <label class="field">
      <span class="field-label">Ítem</span>
      <input class="input" name="title" data-autofocus value="${escapeHtml(draft.title)}" maxlength="200" autocomplete="off" autocapitalize="sentences" enterkeyhint="done">
    </label>
    <label class="toggle-row sheet-toggle">
      <span>Hecho</span>
      <input type="checkbox" class="switch" name="done" data-action="item-done-field" ${draft.done ? 'checked' : ''}>
    </label>
    ${sheet.error ? `<p class="form-error" role="alert">${escapeHtml(sheet.error)}</p>` : ''}
  `;
  const footer = `<button type="submit" class="primary-button">Guardar</button>
    <button type="button" class="text-button danger" data-action="request-delete-item">Eliminar</button>`;
  return sheetShell(index, { title: 'Editar Ítem', body, footer, form: 'item' });
}

function renderProjectMenu(sheet, index) {
  const project = findProject(sheet.projectId);
  if (!project) return '';
  const canAdd = canHaveSubproject(project.id, data.projects);
  const body = `<p class="menu-caption">${escapeHtml(project.title)}</p>
    <button type="button" class="menu-row" data-action="menu-edit-project" data-id="${escapeHtml(project.id)}">${icon('edit', 'menu-icon')}<span>Editar</span></button>
    <button type="button" class="menu-row" data-action="menu-status-project" data-id="${escapeHtml(project.id)}">${icon('status-active', 'menu-icon')}<span>Cambiar Estado</span></button>
    <button type="button" class="menu-row" data-action="menu-priority-project" data-id="${escapeHtml(project.id)}">${icon('flag', 'menu-icon')}<span>Cambiar Prioridad</span></button>
    ${canAdd ? `<button type="button" class="menu-row" data-action="menu-add-subproject" data-id="${escapeHtml(project.id)}">${icon('layers', 'menu-icon')}<span>Agregar Subproyecto</span></button>` : ''}
    <button type="button" class="menu-row" data-action="menu-add-task" data-id="${escapeHtml(project.id)}">${icon('plus', 'menu-icon')}<span>Agregar Tarea</span></button>
    <div class="menu-separator"></div>
    <button type="button" class="menu-row danger" data-action="menu-delete-project" data-id="${escapeHtml(project.id)}">${icon('trash', 'menu-icon')}<span>Eliminar</span></button>`;
  return sheetShell(index, { title: 'Opciones Del Proyecto', body });
}

function renderListMenu(sheet, index) {
  const list = findList(sheet.listId);
  if (!list) return '';
  const canAdd = canHaveSublist(list.id, data.lists);
  const body = `<p class="menu-caption">${escapeHtml(list.title)}</p>
    <button type="button" class="menu-row" data-action="menu-edit-list" data-id="${escapeHtml(list.id)}">${icon('edit', 'menu-icon')}<span>Editar</span></button>
    ${canAdd ? `<button type="button" class="menu-row" data-action="menu-add-sublist" data-id="${escapeHtml(list.id)}">${icon('list', 'menu-icon')}<span>Agregar Sublista</span></button>` : ''}
    <button type="button" class="menu-row" data-action="menu-add-item" data-id="${escapeHtml(list.id)}">${icon('plus', 'menu-icon')}<span>Agregar Ítem</span></button>
    <div class="menu-separator"></div>
    <button type="button" class="menu-row danger" data-action="menu-delete-list" data-id="${escapeHtml(list.id)}">${icon('trash', 'menu-icon')}<span>Eliminar</span></button>`;
  return sheetShell(index, { title: 'Opciones De La Lista', body });
}

function renderStatusPicker(sheet, index) {
  const project = findProject(sheet.projectId);
  const current = project ? project.status : 'Activo';
  const body = `${PROJECT_STATUSES.map(
    (status) => `<button type="button" class="picker-row" data-action="choose-status" data-value="${status.value}">
      <span class="picker-icon tone-${status.tone}">${icon(status.icon, 'small-icon')}</span>
      <span class="picker-title">${escapeHtml(status.label)}</span>
      ${current === status.value ? icon('check', 'picker-check') : ''}
    </button>`,
  ).join('')}`;
  return sheetShell(index, { title: 'Cambiar Estado', body });
}

function renderPriorityPicker(sheet, index) {
  const project = findProject(sheet.projectId);
  const current = project ? project.priority : null;
  const options = [{ value: '', label: 'Sin Prioridad', tone: 'neutral' }, ...PRIORITIES];
  const body = `${options.map(
    (option) => `<button type="button" class="picker-row" data-action="choose-priority" data-value="${option.value}">
      <span class="picker-icon tone-${option.tone}">${icon('flag', 'small-icon')}</span>
      <span class="picker-title">${escapeHtml(option.label)}</span>
      ${(current || '') === option.value ? icon('check', 'picker-check') : ''}
    </button>`,
  ).join('')}`;
  return sheetShell(index, { title: 'Cambiar Prioridad', body });
}

function renderProjectPicker(sheet, index) {
  const options = projectPickerOptions(sheet);
  const body = options.length
    ? options
        .map(
          (row) => `<button type="button" class="picker-row" data-action="choose-project" data-id="${escapeHtml(row.project.id)}" style="--picker-indent:${row.depth * 16}px">
            <span class="picker-icon">${icon(row.project.parentId ? 'layers' : 'folder', 'small-icon')}</span>
            <span class="picker-title">${escapeHtml(row.project.title)}</span>
            ${isClosed(row.project) ? `<span class="muted small">${escapeHtml(row.project.status)}</span>` : ''}
            ${sheet.currentId === row.project.id ? icon('check', 'picker-check') : ''}
          </button>`,
        )
        .join('')
    : `<p class="empty-inline">No hay proyectos disponibles.</p>`;
  return sheetShell(index, {
    title: sheet.title || 'Elegir Proyecto',
    body: `${sheet.allowNone ? `<button type="button" class="picker-row" data-action="choose-project" data-id=""><span class="picker-icon">${icon('folder', 'small-icon')}</span><span class="picker-title">${escapeHtml(sheet.noneLabel || 'Sin Proyecto')}</span>${sheet.currentId ? '' : icon('check', 'picker-check')}</button>` : ''}${body}${sheet.help ? `<p class="help">${escapeHtml(sheet.help)}</p>` : ''}`,
  });
}

function renderListPicker(sheet, index) {
  const options = listPickerOptions(sheet);
  const body = options.length
    ? options
        .map(
          (row) => `<button type="button" class="picker-row" data-action="choose-list" data-id="${escapeHtml(row.list.id)}" style="--picker-indent:${row.depth * 16}px">
            <span class="picker-icon">${icon('list', 'small-icon')}</span>
            <span class="picker-title">${escapeHtml(row.list.title)}</span>
            ${sheet.currentId === row.list.id ? icon('check', 'picker-check') : ''}
          </button>`,
        )
        .join('')
    : `<p class="empty-inline">No hay listas disponibles.</p>`;
  return sheetShell(index, {
    title: sheet.title || 'Elegir Lista',
    body: `${sheet.allowNone ? `<button type="button" class="picker-row" data-action="choose-list" data-id=""><span class="picker-icon">${icon('list', 'small-icon')}</span><span class="picker-title">${escapeHtml(sheet.noneLabel || 'Lista Principal')}</span>${sheet.currentId ? '' : icon('check', 'picker-check')}</button>` : ''}${body}${sheet.help ? `<p class="help">${escapeHtml(sheet.help)}</p>` : ''}`,
  });
}

function renderIconPicker(sheet, index) {
  const body = `<div class="icon-grid large">
    ${ICON_KEYS.map(
      (entry) => `<button type="button" class="icon-choice ${sheet.current === entry.key ? 'is-selected' : ''}" data-action="choose-icon" data-icon="${entry.key}">${icon(entry.key)}<span>${entry.label}</span></button>`,
    ).join('')}
  </div>
  <button type="button" class="text-button" data-action="choose-icon" data-icon="">Usar Predeterminado</button>`;
  return sheetShell(index, { title: 'Ícono Del Proyecto', body });
}

function renderFilters(sheet, index) {
  const body = `<div class="field">
      <span class="field-label">Estados</span>
      <div class="option-row">${PROJECT_STATUSES.map(
        (status) => `<button type="button" class="option-chip tone-${status.tone} ${settings.filter.statuses.includes(status.value) ? 'is-selected' : ''}" data-action="toggle-filter-status" data-value="${status.value}" aria-pressed="${settings.filter.statuses.includes(status.value)}">${icon(status.icon, 'tiny')}${escapeHtml(status.label)}</button>`,
      ).join('')}</div>
    </div>
    <div class="field">
      <span class="field-label">Prioridades</span>
      <div class="option-row">
        <button type="button" class="option-chip tone-neutral ${settings.filter.priorities.includes(null) ? 'is-selected' : ''}" data-action="toggle-filter-priority" data-value="" aria-pressed="${settings.filter.priorities.includes(null)}">${icon('flag', 'tiny')}Sin Prioridad</button>
        ${PRIORITIES.map(
          (priority) => `<button type="button" class="option-chip tone-${priority.tone} ${settings.filter.priorities.includes(priority.value) ? 'is-selected' : ''}" data-action="toggle-filter-priority" data-value="${priority.value}" aria-pressed="${settings.filter.priorities.includes(priority.value)}">${icon('flag', 'tiny')}${escapeHtml(priority.label)}</button>`,
        ).join('')}
      </div>
    </div>
    <button type="button" class="text-button" data-action="reset-filters">Quitar Filtros</button>`;
  return sheetShell(index, {
    title: 'Filtros',
    body,
    footer: '<button type="button" class="primary-button" data-action="close-sheet">Listo</button>',
  });
}

function renderConfirm(sheet, index) {
  const body = `<p class="confirm-message">${escapeHtml(sheet.message)}</p>`;
  const footer = `<button type="button" class="primary-button ${sheet.tone === 'danger' ? 'danger' : ''}" data-action="confirm-yes">${escapeHtml(sheet.confirmLabel || 'Aceptar')}</button>
    ${sheet.secondaryLabel ? `<button type="button" class="secondary-button" data-action="confirm-secondary">${escapeHtml(sheet.secondaryLabel)}</button>` : ''}
    <button type="button" class="text-button" data-action="confirm-no">Cancelar</button>`;
  return sheetShell(index, { title: sheet.title || 'Confirmar', body, footer });
}

function statusOptions(action, current) {
  return PROJECT_STATUSES.map(
    (status) => `<button type="button" class="option-chip tone-${status.tone} ${current === status.value ? 'is-selected' : ''}" data-action="${action}" data-value="${status.value}" aria-pressed="${current === status.value}">${icon(status.icon, 'tiny')}${escapeHtml(status.label)}</button>`,
  ).join('');
}

function priorityOptions(action, current) {
  const options = [{ value: '', label: 'Sin Prioridad', tone: 'neutral' }, ...PRIORITIES];
  return options
    .map(
      (option) => `<button type="button" class="option-chip tone-${option.tone} ${(current || '') === option.value ? 'is-selected' : ''}" data-action="${action}" data-value="${option.value}" aria-pressed="${(current || '') === option.value}">${icon('flag', 'tiny')}${escapeHtml(option.label)}</button>`,
    )
    .join('');
}

// Opciones de pickers

function projectPickerOptions(sheet) {
  if (sheet.excludeId) {
    return validProjectParents(sheet.excludeId, data.projects).map((project) => ({
      project,
      depth: projectDepth(project.id, data.projects),
    }));
  }
  return flattenProjects(data.projects).filter(
    (row) => !sheet.onlyParents || canHaveSubproject(row.project.id, data.projects),
  );
}

function listPickerOptions(sheet) {
  if (sheet.excludeId) {
    return validListParents(sheet.excludeId, data.lists).map((list) => ({
      list,
      depth: listDepth(list.id, data.lists),
    }));
  }
  const rows = flattenLists(data.lists);
  return sheet.onlyParents
    ? rows.filter((row) => canHaveSublist(row.list.id, data.lists))
    : rows;
}

// Eventos

function bindEvents() {
  document.addEventListener('click', onClick);
  document.addEventListener('submit', onSubmit);
  document.addEventListener('change', onChange);
  document.addEventListener('input', onInput);
  document.addEventListener('keydown', onKeyDown);
  window.addEventListener('hashchange', handleHashChange);
  window.addEventListener('pagehide', () => persist({ immediate: true }));
  document.addEventListener('pointerdown', onDragStart);
  document.addEventListener('pointermove', onDragMove, { passive: false });
  document.addEventListener('pointerup', onDragEnd);
  document.addEventListener('pointercancel', onDragCancel);

  const media = window.matchMedia('(prefers-color-scheme: dark)');
  if (media.addEventListener) {
    media.addEventListener('change', () => {
      if (settings.theme === 'system') applyTheme();
    });
  }

  importInput.addEventListener('change', onImportFile);
}

function onClick(event) {
  if (suppressClick) {
    event.preventDefault();
    event.stopPropagation();
    suppressClick = false;
    return;
  }
  const target = event.target.closest('[data-action]');
  if (!target) return;
  if (target.matches('input, select, textarea')) return;
  const action = target.dataset.action;
  handleAction(action, target, event);
}

function onSubmit(event) {
  const form = event.target;
  if (!(form instanceof HTMLFormElement)) return;

  if (form.matches('[data-sheet-form]')) {
    event.preventDefault();
    saveSheetForm(form.dataset.sheetForm);
    return;
  }
  if (form.matches('[data-add-task]')) {
    event.preventDefault();
    const input = form.querySelector('input[name="title"]');
    const projectId = form.dataset.project;
    try {
      const title = normalizeTitle(input.value);
      data.tasks.push({
        id: newId(),
        title,
        done: false,
        projectId,
        dueDate: null,
        description: '',
        priority: null,
        createdAt: nowIso(),
        updatedAt: nowIso(),
      });
      persist();
      input.value = '';
      render({ preserveScroll: true });
      focusInline('task', projectId);
    } catch (error) {
      showToast(error.message || 'No pudimos agregar la tarea.');
    }
    return;
  }
  if (form.matches('[data-add-item]')) {
    event.preventDefault();
    const input = form.querySelector('input[name="title"]');
    const list = findList(form.dataset.list);
    if (!list) return;
    try {
      const title = normalizeTitle(input.value, MAX_ITEM_TITLE_LENGTH);
      const items = list.items || (list.items = []);
      const position = items.reduce((max, item) => Math.max(max, item.position ?? -1), -1) + 1;
      items.push({ id: newId(), title, done: false, position });
      list.updatedAt = nowIso();
      persist();
      input.value = '';
      render({ preserveScroll: true });
      focusInline('item', list.id);
    } catch (error) {
      showToast(error.message || 'No pudimos agregar el ítem.');
    }
  }
}

function onChange(event) {
  const target = event.target;
  if (!(target instanceof HTMLElement)) return;
  if (target.matches('[data-action="toggle-show-done"]')) {
    settings.showDoneTasks = target.checked;
    saveSettings(settings);
    render({ preserveScroll: true });
    return;
  }
  if (target.matches('[data-action="toggle-show-done-items"]')) {
    settings.showDoneListItems = target.checked;
    saveSettings(settings);
    render({ preserveScroll: true });
    return;
  }
  if (target.matches('input[name="dueDate"]')) {
    const sheet = sheets[sheets.length - 1];
    if (sheet && sheet.type === 'task') {
      sheet.draft.dueDate = target.value || '';
      renderOverlay();
    }
    return;
  }
  if (target.matches('[data-action="item-done-field"]')) {
    const sheet = sheets[sheets.length - 1];
    if (sheet && sheet.type === 'item') sheet.draft.done = target.checked;
  }
}

function onInput(event) {
  const target = event.target;
  if (!(target instanceof HTMLInputElement) && !(target instanceof HTMLTextAreaElement)) return;
  if (!target.name) return;
  if (target.name === 'iconEmoji') {
    const sheet = sheets[sheets.length - 1];
    if (sheet && sheet.type === 'project') {
      sheet.draft.icon = target.value.trim() || null;
      updateIconSelection();
    }
    return;
  }
  const sheet = sheets[sheets.length - 1];
  if (!sheet || !sheet.draft) return;
  if (!(target.name in sheet.draft)) return;
  sheet.draft[target.name] = target.value;
}

function onKeyDown(event) {
  if (event.key !== 'Escape') return;
  if (sheets.length) {
    event.preventDefault();
    popSheet();
  }
}

// Acciones

function handleAction(action, element, event) {
  const id = element.dataset.id || null;
  switch (action) {
    case 'tab':
      navigate(element.dataset.tab);
      return;
    case 'back':
      goBack();
      return;
    case 'close-sheet':
      popSheet();
      return;
    case 'new-task':
      openNewTask(null);
      return;
    case 'start-add-task':
      ui.addingProjectId = element.dataset.project;
      render({ preserveScroll: true });
      focusInline('task', element.dataset.project);
      return;
    case 'add-task-here':
      openNewTask(element.dataset.project);
      return;
    case 'open-project':
      navigate('project', id);
      return;
    case 'open-list':
      navigate('list', id);
      return;
    case 'toggle-expand-project':
      toggleSet(ui.expandedProjects, id);
      render({ preserveScroll: true });
      return;
    case 'collapse-project':
      ui.expandedProjects.delete(id);
      render({ preserveScroll: true });
      return;
    case 'expand-all-projects':
      data.projects
        .filter((project) => !project.parentId)
        .forEach((project) => ui.expandedProjects.add(project.id));
      render({ preserveScroll: true });
      return;
    case 'collapse-all-projects':
      ui.expandedProjects.clear();
      render({ preserveScroll: true });
      return;
    case 'toggle-expand-list':
      toggleSet(ui.expandedLists, id);
      render({ preserveScroll: true });
      return;
    case 'collapse-list':
      ui.expandedLists.delete(id);
      render({ preserveScroll: true });
      return;
    case 'expand-all-lists':
      data.lists
        .filter((list) => !list.parentId)
        .forEach((list) => ui.expandedLists.add(list.id));
      render({ preserveScroll: true });
      return;
    case 'collapse-all-lists':
      ui.expandedLists.clear();
      render({ preserveScroll: true });
      return;
    case 'toggle-collapse-list':
      toggleSet(ui.collapsedLists, id);
      render({ preserveScroll: true });
      return;
    case 'toggle-task':
      toggleTask(id);
      return;
    case 'open-task':
      openTaskSheet(id);
      return;
    case 'new-project':
      openProjectSheet(null, null);
      return;
    case 'project-menu':
      pushSheet({ type: 'project-menu', projectId: id });
      return;
    case 'mark-project-done':
      setProjectStatus(id, 'Terminado');
      return;
    case 'new-list':
      openListSheet(null, null);
      return;
    case 'list-menu':
      pushSheet({ type: 'list-menu', listId: id });
      return;
    case 'toggle-item':
      toggleItem(id);
      return;
    case 'edit-item':
      openItemSheet(id);
      return;
    case 'add-item-focus': {
      const input = document.querySelector('.bottom-add input[name="title"]');
      if (input) {
        input.focus();
        input.scrollIntoView({ block: 'center', behavior: 'smooth' });
      }
      return;
    }
    case 'filter-open':
      pushSheet({ type: 'filters' });
      return;
    case 'filter-reset':
      resetFilters();
      return;
    case 'set-theme':
      settings.theme = element.dataset.theme;
      saveSettings(settings);
      applyTheme();
      render({ preserveScroll: true });
      return;
    case 'export-data':
      exportData();
      return;
    case 'import-data':
      importInput.click();
      return;
    case 'toast-action':
      if (toastAction) toastAction();
      hideToast();
      return;

    // Hojas de tarea, proyecto y lista

    case 'open-task-project':
      pushSheet({
        type: 'project-picker',
        title: 'Mover A Proyecto',
        purpose: 'task',
        currentId: currentDraft()?.projectId || null,
        help: 'La tarea se mueve con su historial y deja de aparecer en el proyecto anterior.',
      });
      return;
    case 'open-project-parent': {
      const sheet = sheets[sheets.length - 1];
      pushSheet({
        type: 'project-picker',
        title: 'Proyecto Padre',
        purpose: 'project-parent',
        currentId: sheet?.draft?.parentId || null,
        excludeId: sheet?.projectId || null,
        onlyParents: !sheet?.projectId,
        allowNone: true,
        noneLabel: 'Sin Proyecto Padre',
      });
      return;
    }
    case 'open-list-project': {
      const sheet = sheets[sheets.length - 1];
      pushSheet({
        type: 'project-picker',
        title: 'Vincular A Proyecto',
        purpose: 'list-project',
        currentId: sheet?.draft?.projectId || null,
        allowNone: true,
        noneLabel: 'Sin Proyecto',
      });
      return;
    }
    case 'open-list-parent': {
      const sheet = sheets[sheets.length - 1];
      pushSheet({
        type: 'list-picker',
        title: 'Lista Padre',
        purpose: 'list-parent',
        currentId: sheet?.draft?.parentId || null,
        excludeId: sheet?.listId || null,
        onlyParents: !sheet?.listId,
        allowNone: true,
        noneLabel: 'Lista Principal',
      });
      return;
    }
    case 'pick-icon': {
      const sheet = sheets[sheets.length - 1];
      if (sheet && sheet.type === 'project') {
        sheet.draft.icon = element.dataset.icon || null;
        const emoji = overlayRoot.querySelector('input[name="iconEmoji"]');
        if (emoji) emoji.value = '';
        updateIconSelection();
      }
      return;
    }
    case 'set-task-priority':
      setDraftValue('priority', element.dataset.value || null);
      return;
    case 'set-project-status-draft':
      setDraftValue('status', element.dataset.value);
      return;
    case 'set-project-priority-draft':
      setDraftValue('priority', element.dataset.value || null);
      return;
    case 'clear-task-date':
      setDraftValue('dueDate', '');
      return;
    case 'request-delete-task': {
      const taskId = sheets[sheets.length - 1]?.taskId || null;
      const task = taskId ? findTask(taskId) : null;
      openConfirm({
        title: 'Eliminar Tarea',
        message: task
          ? `¿Eliminar «${task.title}»? Esto no se puede deshacer.`
          : '¿Eliminar esta tarea? Esto no se puede deshacer.',
        confirmLabel: 'Eliminar',
        tone: 'danger',
        onConfirm: () => {
          data.tasks = data.tasks.filter((candidate) => candidate.id !== taskId);
          persist();
          closeAllSheets();
          render({ preserveScroll: true });
          showToast('Tarea eliminada.');
        },
      });
      return;
    }
    case 'request-delete-project': {
      const sheet = sheets[sheets.length - 1];
      requestDeleteProject(sheet?.projectId);
      return;
    }
    case 'request-delete-list': {
      const sheet = sheets[sheets.length - 1];
      requestDeleteList(sheet?.listId);
      return;
    }
    case 'request-delete-item': {
      const sheet = sheets[sheets.length - 1];
      const found = sheet?.itemId ? findItemById(sheet.itemId) : null;
      openConfirm({
        title: 'Eliminar Ítem',
        message: found
          ? `¿Eliminar «${found.item.title}»? Esto no se puede deshacer.`
          : '¿Eliminar este ítem?',
        confirmLabel: 'Eliminar',
        tone: 'danger',
        onConfirm: () => {
          const target = findItemById(sheet.itemId);
          if (target) {
            target.list.items = target.list.items.filter((item) => item.id !== sheet.itemId);
            target.list.updatedAt = nowIso();
          }
          persist();
          closeAllSheets();
          render({ preserveScroll: true });
          showToast('Ítem eliminado.');
        },
      });
      return;
    }

    // Menús

    case 'menu-edit-project':
      closeAllSheets();
      openProjectSheet(id, null);
      return;
    case 'menu-status-project':
      closeAllSheets();
      openStatusPicker(id);
      return;
    case 'menu-priority-project':
      closeAllSheets();
      openPriorityPicker(id);
      return;
    case 'menu-add-subproject':
      closeAllSheets();
      openProjectSheet(null, id);
      return;
    case 'menu-add-task':
      closeAllSheets();
      openNewTask(id);
      return;
    case 'menu-delete-project':
      closeAllSheets();
      requestDeleteProject(id);
      return;
    case 'menu-edit-list':
      closeAllSheets();
      openListSheet(id, null);
      return;
    case 'menu-add-sublist':
      closeAllSheets();
      openListSheet(null, id);
      return;
    case 'menu-add-item': {
      closeAllSheets();
      const input = document.querySelector('.bottom-add input[name="title"]');
      if (input) {
        input.focus();
        input.scrollIntoView({ block: 'center', behavior: 'smooth' });
      }
      return;
    }
    case 'menu-delete-list':
      closeAllSheets();
      requestDeleteList(id);
      return;

    // Pickers

    case 'choose-status':
      chooseStatus(element.dataset.value);
      return;
    case 'choose-priority':
      choosePriority(element.dataset.value || null);
      return;
    case 'choose-project':
      chooseProject(id);
      return;
    case 'choose-list':
      chooseList(id);
      return;
    case 'choose-icon':
      return;
    case 'toggle-filter-status':
      toggleFilterValue('statuses', element.dataset.value);
      return;
    case 'toggle-filter-priority':
      toggleFilterValue('priorities', element.dataset.value || null);
      return;
    case 'reset-filters':
      resetFilters();
      return;

    // Confirmación

    case 'confirm-yes': {
      const sheet = sheets[sheets.length - 1];
      if (sheet && sheet.type === 'confirm') {
        sheets.pop();
        renderOverlay();
        if (typeof sheet.onConfirm === 'function') sheet.onConfirm();
      }
      return;
    }
    case 'confirm-secondary': {
      const sheet = sheets[sheets.length - 1];
      if (sheet && typeof sheet.onSecondary === 'function') sheet.onSecondary();
      return;
    }
    case 'confirm-no':
      popSheet();
      return;
    default:
      return;
  }
}

// Abrir hojas

function openTaskSheet(taskId, fallbackProjectId = null) {
  if (!data.projects.length) {
    showToast('Primero creá un proyecto para poder agregar tareas.');
    openProjectSheet(null, null);
    return;
  }
  const task = taskId ? findTask(taskId) : null;
  if (taskId && !task) {
    showToast('Esa tarea ya no existe.');
    return;
  }
  pushSheet({
    type: 'task',
    taskId: task ? task.id : null,
    draft: {
      title: task?.title || '',
      description: task?.description || '',
      dueDate: task?.dueDate || '',
      priority: task?.priority || null,
      projectId: task?.projectId || fallbackProjectId || data.projects[0]?.id || '',
    },
    error: null,
  });
}

function openNewTask(projectId) {
  openTaskSheet(null, projectId);
}

function openProjectSheet(projectId, parentId) {
  const project = projectId ? findProject(projectId) : null;
  if (projectId && !project) {
    showToast('Ese proyecto ya no existe.');
    return;
  }
  pushSheet({
    type: 'project',
    projectId: project ? project.id : null,
    draft: {
      title: project?.title || '',
      status: project?.status || 'Activo',
      priority: project?.priority || null,
      icon: project?.icon || null,
      parentId: project ? project.parentId || null : parentId || null,
    },
    error: null,
  });
}

function openListSheet(listId, parentId) {
  const list = listId ? findList(listId) : null;
  if (listId && !list) {
    showToast('Esa lista ya no existe.');
    return;
  }
  const parent = parentId ? findList(parentId) : null;
  pushSheet({
    type: 'list',
    listId: list ? list.id : null,
    draft: {
      title: list?.title || '',
      projectId: list ? list.projectId || null : parent?.projectId || null,
      parentId: list ? list.parentId || null : parentId || null,
    },
    error: null,
  });
}

function openItemSheet(itemId) {
  const found = findItemById(itemId);
  if (!found) {
    showToast('Ese ítem ya no existe.');
    return;
  }
  pushSheet({
    type: 'item',
    itemId,
    listId: found.list.id,
    draft: { title: found.item.title, done: Boolean(found.item.done) },
    error: null,
  });
}

function openStatusPicker(projectId) {
  if (!findProject(projectId)) return;
  pushSheet({ type: 'status-picker', projectId });
}

function openPriorityPicker(projectId) {
  if (!findProject(projectId)) return;
  pushSheet({ type: 'priority-picker', projectId });
}

function openConfirm(options) {
  pushSheet({ type: 'confirm', tone: 'neutral', ...options });
}

// Guardar hojas

function saveSheetForm(kind) {
  const sheet = sheets[sheets.length - 1];
  if (!sheet) return;
  try {
    if (kind === 'task') saveTaskSheet(sheet);
    else if (kind === 'project') saveProjectSheet(sheet);
    else if (kind === 'list') saveListSheet(sheet);
    else if (kind === 'item') saveItemSheet(sheet);
  } catch (error) {
    sheet.error = error.message || 'No pudimos guardar.';
    renderOverlay();
  }
}

function saveTaskSheet(sheet) {
  const draft = sheet.draft;
  const title = normalizeTitle(draft.title);
  const description = normalizeDescription(draft.description);
  const dueDate = draft.dueDate ? String(draft.dueDate) : null;
  const priority = draft.priority || null;
  if (!draft.projectId || !findProject(draft.projectId)) {
    throw new DomainError('Elegí un proyecto para la tarea.');
  }
  const timestamp = nowIso();
  if (sheet.taskId) {
    const task = findTask(sheet.taskId);
    if (!task) throw new DomainError('Esa tarea ya no existe.');
    task.title = title;
    task.description = description;
    task.dueDate = dueDate;
    task.priority = priority;
    task.projectId = draft.projectId;
    task.updatedAt = timestamp;
    persist();
    closeAllSheets();
    render({ preserveScroll: true });
    showToast('Tarea guardada.');
    return;
  }
  data.tasks.push({
    id: newId(),
    title,
    done: false,
    projectId: draft.projectId,
    dueDate,
    description,
    priority,
    createdAt: timestamp,
    updatedAt: timestamp,
  });
  persist();
  closeAllSheets();
  ui.addingProjectId = null;
  render({ preserveScroll: true });
  showToast('Tarea agregada.');
}

function saveProjectSheet(sheet) {
  const draft = sheet.draft;
  const title = normalizeTitle(draft.title);
  const icon = normalizeIcon(draft.icon);
  if (!PROJECT_STATUSES.some((status) => status.value === draft.status)) {
    throw new DomainError('Elegí un estado válido.');
  }
  const priority = draft.priority || null;
  if (priority && !PRIORITIES.some((entry) => entry.value === priority)) {
    throw new DomainError('Elegí una prioridad válida.');
  }
  let parentId = draft.parentId || null;
  if (parentId && !findProject(parentId)) throw new DomainError('Ese proyecto padre ya no existe.');

  if (parentId) {
    if (sheet.projectId) {
      if (parentId === sheet.projectId) throw new DomainError('Un proyecto no puede ser su propio padre.');
      if (!validProjectParents(sheet.projectId, data.projects).some((project) => project.id === parentId)) {
        throw new DomainError('Ese proyecto no puede ser el padre: se pasarían de tres niveles.');
      }
    } else {
      const depth = projectDepth(parentId, data.projects);
      if (depth < 0 || depth >= MAX_SUBPROJECT_DEPTH) {
        throw new DomainError('No se pueden anidar más de tres niveles de subproyectos.');
      }
    }
  }

  const timestamp = nowIso();
  if (sheet.projectId) {
    const project = findProject(sheet.projectId);
    if (!project) throw new DomainError('Ese proyecto ya no existe.');
    project.title = title;
    project.status = draft.status;
    project.priority = priority;
    project.icon = icon;
    project.parentId = parentId;
    project.updatedAt = timestamp;
    persist();
    closeAllSheets();
    render({ preserveScroll: true });
    showToast('Proyecto guardado.');
    return;
  }

  data.projects.push({
    id: newId(),
    title,
    status: draft.status,
    parentId,
    priority,
    icon,
    createdAt: timestamp,
    updatedAt: timestamp,
  });
  persist();
  closeAllSheets();
  render({ preserveScroll: true });
  showToast('Proyecto creado.');
}

function saveListSheet(sheet) {
  const draft = sheet.draft;
  const title = normalizeTitle(draft.title);
  const projectId = draft.projectId || null;
  if (projectId && !findProject(projectId)) throw new DomainError('Ese proyecto ya no existe.');
  const parentId = draft.parentId || null;
  if (parentId && !findList(parentId)) throw new DomainError('Esa lista padre ya no existe.');

  if (parentId) {
    if (sheet.listId) {
      if (parentId === sheet.listId) throw new DomainError('Una lista no puede ser su propia padre.');
      if (!validListParents(sheet.listId, data.lists).some((list) => list.id === parentId)) {
        throw new DomainError('Esa lista no puede ser la padre: se pasarían de tres niveles.');
      }
    } else if (!canHaveSublist(parentId, data.lists)) {
      throw new DomainError('No se pueden anidar más de tres niveles de listas.');
    }
  }

  const timestamp = nowIso();
  if (sheet.listId) {
    const list = findList(sheet.listId);
    if (!list) throw new DomainError('Esa lista ya no existe.');
    if (list.parentId !== parentId) {
      list.parentId = parentId;
      list.position = nextChildPosition(parentId);
    }
    list.title = title;
    list.projectId = projectId;
    list.updatedAt = timestamp;
    persist();
    closeAllSheets();
    render({ preserveScroll: true });
    showToast('Lista guardada.');
    return;
  }

  data.lists.push({
    id: newId(),
    title,
    projectId,
    parentId,
    position: nextChildPosition(parentId),
    createdAt: timestamp,
    updatedAt: timestamp,
    items: [],
  });
  persist();
  closeAllSheets();
  render({ preserveScroll: true });
  showToast('Lista creada.');
}

function saveItemSheet(sheet) {
  const draft = sheet.draft;
  const title = normalizeTitle(draft.title, MAX_ITEM_TITLE_LENGTH);
  const found = findItemById(sheet.itemId);
  if (!found) throw new DomainError('Ese ítem ya no existe.');
  found.item.title = title;
  found.item.done = Boolean(draft.done);
  found.list.updatedAt = nowIso();
  persist();
  closeAllSheets();
  render({ preserveScroll: true });
  showToast('Ítem guardado.');
}

// Mutaciones

function toggleTask(taskId) {
  const task = findTask(taskId);
  if (!task) return;
  task.done = !task.done;
  task.updatedAt = nowIso();
  persist();
  render({ preserveScroll: true });
}

function toggleItem(itemId) {
  const found = findItemById(itemId);
  if (!found) return;
  found.item.done = !found.item.done;
  found.list.updatedAt = nowIso();
  persist();
  render({ preserveScroll: true });
}

function setProjectStatus(projectId, status) {
  const project = findProject(projectId);
  if (!project) return;
  project.status = status;
  project.updatedAt = nowIso();
  persist();
  closeAllSheets();
  render({ preserveScroll: true });
  const meta = PROJECT_STATUSES.find((entry) => entry.value === status);
  showToast(`Estado: ${meta ? meta.label : status}.`);
}

function setProjectPriority(projectId, priority) {
  const project = findProject(projectId);
  if (!project) return;
  project.priority = priority || null;
  project.updatedAt = nowIso();
  persist();
  closeAllSheets();
  render({ preserveScroll: true });
  const meta = priorityMeta(priority);
  showToast(meta ? `Prioridad: ${meta.label}.` : 'Prioridad quitada.');
}

function chooseStatus(value) {
  const sheet = sheets[sheets.length - 1];
  if (!sheet || sheet.type !== 'status-picker') return;
  setProjectStatus(sheet.projectId, value);
}

function choosePriority(value) {
  const sheet = sheets[sheets.length - 1];
  if (!sheet || sheet.type !== 'priority-picker') return;
  setProjectPriority(sheet.projectId, value);
}

function chooseProject(projectId) {
  const picker = sheets[sheets.length - 1];
  if (!picker || picker.type !== 'project-picker') return;
  sheets.pop();
  const parent = sheets[sheets.length - 1];
  if (parent && parent.draft) {
    if (picker.purpose === 'task') parent.draft.projectId = projectId;
    else if (picker.purpose === 'project-parent') parent.draft.parentId = projectId || null;
    else if (picker.purpose === 'list-project') parent.draft.projectId = projectId || null;
    parent.error = null;
  }
  renderOverlay();
}

function chooseList(listId) {
  const picker = sheets[sheets.length - 1];
  if (!picker || picker.type !== 'list-picker') return;
  sheets.pop();
  const parent = sheets[sheets.length - 1];
  if (parent && parent.draft && picker.purpose === 'list-parent') {
    parent.draft.parentId = listId || null;
    parent.error = null;
  }
  renderOverlay();
}

function requestDeleteProject(projectId) {
  const project = findProject(projectId);
  if (!project) return;
  const ids = subtreeIds(projectId, data.projects);
  const taskCount = data.tasks.filter((task) => ids.has(task.projectId)).length;
  const subprojects = ids.size - 1;
  const parts = [];
  if (subprojects > 0) parts.push(`${subprojects} ${subprojects === 1 ? 'subproyecto' : 'subproyectos'}`);
  if (taskCount > 0) parts.push(`${taskCount} ${taskCount === 1 ? 'tarea' : 'tareas'}`);
  const extra = parts.length ? ` También se van a borrar ${parts.join(' y ')}.` : '';
  openConfirm({
    title: 'Eliminar Proyecto',
    message: `¿Borrar «${project.title}»?${extra} Las listas vinculadas quedan sueltas. Esto no se puede deshacer.`,
    confirmLabel: 'Eliminar',
    tone: 'danger',
    onConfirm: () => {
      data.projects = data.projects.filter((candidate) => !ids.has(candidate.id));
      data.tasks = data.tasks.filter((task) => !ids.has(task.projectId));
      data.lists.forEach((list) => {
        if (list.projectId && ids.has(list.projectId)) {
          list.projectId = null;
          list.updatedAt = nowIso();
        }
      });
      persist();
      closeAllSheets();
      render({ preserveScroll: true });
      showToast('Proyecto eliminado.');
    },
  });
}

function requestDeleteList(listId) {
  const list = findList(listId);
  if (!list) return;
  const ids = listSubtree(listId);
  const itemCount = data.lists
    .filter((candidate) => ids.has(candidate.id))
    .reduce((sum, candidate) => sum + (candidate.items || []).length, 0);
  const extra = itemCount ? ` Se van a borrar ${itemCount} ${itemCount === 1 ? 'ítem' : 'ítems'}.` : '';
  openConfirm({
    title: 'Eliminar Lista',
    message: `¿Borrar «${list.title}»?${extra} Esto no se puede deshacer.`,
    confirmLabel: 'Eliminar',
    tone: 'danger',
    onConfirm: () => {
      data.lists = data.lists.filter((candidate) => !ids.has(candidate.id));
      persist();
      closeAllSheets();
      render({ preserveScroll: true });
      showToast('Lista eliminada.');
    },
  });
}

function listSubtree(rootId) {
  const result = new Set();
  const pending = [rootId];
  while (pending.length) {
    const current = pending.shift();
    if (result.has(current)) continue;
    result.add(current);
    data.lists
      .filter((list) => list.parentId === current)
      .forEach((child) => pending.push(child.id));
  }
  return result;
}

function nextChildPosition(parentId) {
  let max = -1;
  data.lists
    .filter((list) => list.parentId === parentId)
    .forEach((list) => {
      max = Math.max(max, list.position ?? 0);
    });
  if (parentId) {
    const parent = findList(parentId);
    (parent?.items || []).forEach((item) => {
      max = Math.max(max, item.position ?? 0);
    });
  }
  return max + 1;
}

// Filtros

function toggleFilterValue(field, value) {
  const list = settings.filter[field];
  const index = list.findIndex((entry) => entry === value);
  if (index >= 0) list.splice(index, 1);
  else list.push(value);
  saveSettings(settings);
  render({ preserveScroll: true });
  renderOverlay();
}

function resetFilters() {
  settings.filter = { statuses: [...DEFAULT_STATUSES], priorities: [...ALL_PRIORITIES] };
  saveSettings(settings);
  render({ preserveScroll: true });
  renderOverlay();
  showToast('Filtros quitados.');
}

// Import / export

function exportData() {
  try {
    const text = encodeBackup(data);
    const blob = new Blob([text], { type: 'application/json;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    const name = `list-master-${localDate()}.json`;
    anchor.href = url;
    anchor.download = name;
    anchor.rel = 'noopener';
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    setTimeout(() => URL.revokeObjectURL(url), 5000);
    showToast(`Listo, guardaste todo en ${name}.`);
  } catch (error) {
    console.error(error);
    showToast('No pudimos exportar. Probá de nuevo.');
  }
}

async function onImportFile() {
  const file = importInput.files && importInput.files[0];
  importInput.value = '';
  if (!file) return;
  let text;
  try {
    text = await file.text();
  } catch {
    showToast('No pudimos leer el archivo.');
    return;
  }
  let parsed;
  try {
    parsed = decodeBackup(text);
  } catch (error) {
    showToast(error.message || 'No pudimos importar. El archivo no parece un export de List Master.');
    return;
  }
  openConfirm({
    title: 'Importar JSON',
    message: `El archivo «${file.name}» tiene ${parsed.projects.length} ${parsed.projects.length === 1 ? 'proyecto' : 'proyectos'}, ${parsed.tasks.length} ${parsed.tasks.length === 1 ? 'tarea' : 'tareas'} y ${parsed.lists.length} ${parsed.lists.length === 1 ? 'lista' : 'listas'}. Importar reemplaza todo lo que tenés ahora.`,
    confirmLabel: 'Importar Igual',
    tone: 'danger',
    secondaryLabel: 'Exportar Primero',
    onSecondary: exportData,
    onConfirm: () => {
      data = parsed;
      persist({ immediate: true });
      ui.expandedProjects.clear();
      ui.expandedLists.clear();
      ui.collapsedLists.clear();
      closeAllSheets();
      navigate('pending');
      showToast('Importaste todo. Tus datos anteriores fueron reemplazados.');
    },
  });
}

// Tema y servicio

function normalizeSettings(raw) {
  const merged = cloneSettings(DEFAULT_SETTINGS);
  if (!raw || typeof raw !== 'object') return merged;
  if (['light', 'dark', 'system'].includes(raw.theme)) merged.theme = raw.theme;
  merged.showDoneTasks = Boolean(raw.showDoneTasks);
  merged.showDoneListItems = Boolean(raw.showDoneListItems);
  const filter = raw.filter && typeof raw.filter === 'object' ? raw.filter : {};
  if (Array.isArray(filter.statuses)) {
    merged.filter.statuses = filter.statuses.filter((status) =>
      PROJECT_STATUSES.some((entry) => entry.value === status),
    );
  }
  if (Array.isArray(filter.priorities)) {
    merged.filter.priorities = filter.priorities.filter((priority) => ALL_PRIORITIES.includes(priority));
  }
  return merged;
}

function cloneSettings(source) {
  return {
    theme: source.theme,
    showDoneTasks: source.showDoneTasks,
    showDoneListItems: source.showDoneListItems,
    filter: {
      statuses: [...source.filter.statuses],
      priorities: [...source.filter.priorities],
    },
  };
}

function applyTheme() {
  const resolved =
    settings.theme === 'system'
      ? window.matchMedia('(prefers-color-scheme: dark)').matches
        ? 'dark'
        : 'light'
      : settings.theme;
  document.documentElement.dataset.theme = resolved;
  const meta = document.querySelector('meta[name="theme-color"]');
  if (meta) meta.setAttribute('content', resolved === 'dark' ? '#0B0B0B' : '#F7F7F7');
}

function registerServiceWorker() {
  if (!('serviceWorker' in navigator)) return;
  if (location.protocol !== 'https:' && location.hostname !== 'localhost' && location.hostname !== '127.0.0.1') {
    return;
  }
  navigator.serviceWorker.register('sw.js').catch((error) => {
    console.warn('No se pudo registrar el modo offline.', error);
  });
}

// Persistencia y utilidades

function persist({ immediate = false } = {}) {
  if (saveTimer) {
    clearTimeout(saveTimer);
    saveTimer = null;
  }
  if (immediate) {
    saveData(data).catch(() => showToast('No pudimos guardar los datos en este iPhone.'));
    return;
  }
  saveTimer = setTimeout(() => {
    saveTimer = null;
    saveData(data).catch(() => showToast('No pudimos guardar los datos en este iPhone.'));
  }, 120);
}

function findProject(id) {
  return data.projects.find((project) => project.id === id) || null;
}

function findTask(id) {
  return data.tasks.find((task) => task.id === id) || null;
}

function findList(id) {
  return data.lists.find((list) => list.id === id) || null;
}

function findItemById(itemId) {
  for (const list of data.lists) {
    const item = (list.items || []).find((candidate) => candidate.id === itemId);
    if (item) return { list, item };
  }
  return null;
}

function currentDraft() {
  const sheet = sheets[sheets.length - 1];
  return sheet ? sheet.draft : null;
}

function toggleSet(set, value) {
  if (set.has(value)) set.delete(value);
  else set.add(value);
}

function setDraftValue(field, value) {
  const sheet = sheets[sheets.length - 1];
  if (!sheet || !sheet.draft) return;
  sheet.draft[field] = value;
  if (sheet.error) sheet.error = null;
  renderOverlay();
}

function updateIconSelection() {
  const sheet = sheets[sheets.length - 1];
  if (!sheet) return;
  const selected = sheet.draft.icon || '';
  overlayRoot.querySelectorAll('.icon-choice').forEach((button) => {
    const isSelected = (button.dataset.icon || '') === selected;
    button.classList.toggle('is-selected', isSelected);
    button.setAttribute('aria-pressed', isSelected ? 'true' : 'false');
  });
}

function focusInline(kind, ownerId) {
  requestAnimationFrame(() => {
    if (kind === 'task') {
      const form = document.querySelector(`[data-add-task][data-project="${cssEscape(ownerId)}"]`);
      form?.querySelector('input[name="title"]')?.focus();
    } else {
      const form = document.querySelector(`[data-add-item][data-list="${cssEscape(ownerId)}"]`);
      form?.querySelector('input[name="title"]')?.focus();
    }
  });
}

function cssEscape(value) {
  if (window.CSS && typeof window.CSS.escape === 'function') return window.CSS.escape(value);
  return String(value).replace(/["\\]/g, '\\$&');
}

function goBack() {
  if (route.name === 'project') navigate('projects');
  else if (route.name === 'list') navigate('lists');
  else if (history.length > 1) history.back();
  else navigate('pending');
}

function showToast(message, { actionLabel = null, onAction = null, tone = 'neutral' } = {}) {
  hideToast();
  if (toastClearTimer) {
    clearTimeout(toastClearTimer);
    toastClearTimer = null;
  }
  toastAction = typeof onAction === 'function' ? onAction : null;
  toastRoot.innerHTML = `<div class="toast tone-${tone}" role="status">
    <span>${escapeHtml(message)}</span>
    ${actionLabel ? `<button type="button" class="toast-action" data-action="toast-action">${escapeHtml(actionLabel)}</button>` : ''}
  </div>`;
  requestAnimationFrame(() => toastRoot.querySelector('.toast')?.classList.add('is-visible'));
  toastTimer = setTimeout(hideToast, actionLabel ? 6500 : 4200);
}

function hideToast() {
  if (toastTimer) {
    clearTimeout(toastTimer);
    toastTimer = null;
  }
  toastAction = null;
  const toast = toastRoot.querySelector('.toast');
  if (toast) toast.classList.remove('is-visible');
  if (toastClearTimer) clearTimeout(toastClearTimer);
  toastClearTimer = setTimeout(() => {
    if (!toastRoot.querySelector('.toast.is-visible')) toastRoot.innerHTML = '';
    toastClearTimer = null;
  }, 220);
}

function localDate() {
  const today = new Date();
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
}

// Arrastrar para ordenar ítems y sublistas

function onDragStart(event) {
  if (event.button !== undefined && event.button !== 0) return;
  const handle = event.target.closest('[data-drag-handle]');
  if (!handle) return;
  const row = handle.closest('[data-reorder-id]');
  if (!row) return;
  const container = row.closest('[data-reorder-list], .sublist-children');
  if (!container) return;
  const rows = directReorderRows(container);
  const index = rows.indexOf(row);
  if (index < 0) return;
  const ownerId =
    container.dataset.reorderList || container.previousElementSibling?.dataset.reorderId || null;
  if (!ownerId) return;
  event.preventDefault();
  const rect = row.getBoundingClientRect();
  dragState = {
    pointerId: event.pointerId,
    row,
    container,
    rows,
    index,
    ownerId,
    startY: event.clientY,
    offsetY: event.clientY - rect.top,
    target: null,
  };
  row.classList.add('is-dragging');
  row.style.setProperty('--drag-y', '0px');
  suppressClick = true;
  if (handle.setPointerCapture) {
    try {
      handle.setPointerCapture(event.pointerId);
    } catch {
      // Sin captura igual funciona con los listeners del documento.
    }
  }
}

function onDragMove(event) {
  if (!dragState || event.pointerId !== dragState.pointerId) return;
  event.preventDefault();
  const dy = event.clientY - dragState.startY;
  dragState.row.style.setProperty('--drag-y', `${dy}px`);
  const others = dragState.rows.filter((row) => row !== dragState.row);
  let target = 0;
  others.forEach((row) => {
    const rect = row.getBoundingClientRect();
    if (event.clientY > rect.top + rect.height / 2) target += 1;
  });
  dragState.target = Math.max(0, Math.min(others.length, target));
}

function onDragEnd(event) {
  if (!dragState || (event && event.pointerId !== dragState.pointerId)) return;
  const state = dragState;
  dragState = null;
  state.row.classList.remove('is-dragging');
  state.row.style.removeProperty('--drag-y');
  setTimeout(() => {
    suppressClick = false;
  }, 0);
  if (state.target === null) return;
  reorderListRows(state.ownerId, state.row.dataset.reorderId, state.target);
}

function onDragCancel() {
  if (!dragState) return;
  dragState.row.classList.remove('is-dragging');
  dragState.row.style.removeProperty('--drag-y');
  dragState = null;
  setTimeout(() => {
    suppressClick = false;
  }, 0);
}

function directReorderRows(container) {
  const rows = [];
  for (const child of container.children) {
    if (child.matches('[data-reorder-id]')) {
      rows.push(child);
    } else if (child.classList.contains('sublist-block')) {
      const row = child.querySelector('[data-reorder-id]');
      if (row) rows.push(row);
    }
  }
  return rows;
}

function reorderListRows(ownerId, movingId, targetIndex) {
  const owner = findList(ownerId);
  if (!owner) return;
  const rows = [];
  data.lists
    .filter((list) => list.parentId === ownerId)
    .forEach((list) => rows.push({ kind: 'list', id: list.id, position: list.position ?? 0, list }));
  (owner.items || []).forEach((item) =>
    rows.push({ kind: 'item', id: item.id, position: item.position ?? 0, item }),
  );
  rows.sort((a, b) => {
    if (a.position !== b.position) return a.position - b.position;
    if (a.kind !== b.kind) return a.kind === 'item' ? -1 : 1;
    return a.id < b.id ? -1 : a.id > b.id ? 1 : 0;
  });
  const current = rows.findIndex((row) => row.id === movingId);
  if (current < 0) return;
  const [moved] = rows.splice(current, 1);
  rows.splice(Math.max(0, Math.min(rows.length, targetIndex)), 0, moved);
  const timestamp = nowIso();
  rows.forEach((row, index) => {
    if (row.kind === 'item') row.item.position = index;
    else {
      row.list.position = index;
      row.list.updatedAt = timestamp;
    }
  });
  owner.updatedAt = timestamp;
  persist();
  render({ preserveScroll: true });
}

boot();
