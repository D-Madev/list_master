// Render de las pantallas. Todo sale como HTML plano con atributos `data-*`;
// app.js escucha los eventos en un solo lugar.

import {
  canHaveSubproject,
  childListsOf,
  childrenOf,
  compareLists,
  flattenProjects,
  hasTasks,
  isClosed,
  isComplete,
  listProgress,
  pendingCount,
  pendingGroups,
  priorityMeta,
  progressFor,
  progressPercentage,
  sortTasks,
  sortedItems,
  statusMeta,
  subtreeIds,
} from './domain.js';
import { escapeHtml, icon, projectIconMarkup } from './icons.js';

export function renderScreen(ctx) {
  switch (ctx.route.name) {
    case 'pending':
      return renderPending(ctx);
    case 'projects':
      return renderProjects(ctx);
    case 'project':
      return renderProjectDetail(ctx);
    case 'lists':
      return renderLists(ctx);
    case 'list':
      return renderListDetail(ctx);
    case 'settings':
      return renderSettings(ctx);
    default:
      return renderPending(ctx);
  }
}

export function renderNav(route) {
  const tabs = [
    { key: 'pending', label: 'Pendientes', icon: 'check-list' },
    { key: 'projects', label: 'Proyectos', icon: 'folder' },
    { key: 'lists', label: 'Listas', icon: 'list' },
    { key: 'settings', label: 'Ajustes', icon: 'tune' },
  ];
  const active = route.name;
  return `<nav class="bottom-nav" aria-label="Navegación principal">${tabs
    .map(
      (tab) => `<button class="nav-item ${active === tab.key ? 'is-active' : ''}" data-action="tab" data-tab="${tab.key}" aria-current="${active === tab.key ? 'page' : 'false'}">${icon(tab.icon, 'nav-icon')}<span>${tab.label}</span></button>`,
    )
    .join('')}</nav>`;
}

export function needsNav(route) {
  return ['pending', 'projects', 'lists', 'settings'].includes(route.name);
}

// Pantallas

function renderPending(ctx) {
  const { data } = ctx;
  const groups = pendingGroups(data.projects, data.tasks);
  const count = pendingCount(groups);
  const projectCount = groups.filter((group) => group.tasks.length > 0).length;

  const blocks = [];
  for (const group of groups) {
    if (group.depth === 0) blocks.push([]);
    if (blocks.length) blocks[blocks.length - 1].push(group);
  }

  const body = count
    ? `<div class="stack">${blocks
        .map((block) => renderPendingBlock(block, ctx))
        .join('')}</div>`
    : `<div class="empty-state">${icon('check-list', 'empty-icon')}<h2>No tenés nada pendiente.</h2><p>Cuando haya tareas sin hacer, las vas a ver todas juntas acá, por proyecto.</p></div>`;

  return `<main class="screen has-fab">
    ${header({
      title: 'Pendientes',
      subtitle: count
        ? `${count} ${count === 1 ? 'tarea' : 'tareas'} en ${projectCount} ${projectCount === 1 ? 'proyecto' : 'proyectos'}.`
        : null,
    })}
    ${body}
    <button class="fab" data-action="new-task" aria-label="Nueva Tarea Pendiente">${icon('plus')}</button>
  </main>`;
}

function renderPendingBlock(block, ctx) {
  return `<section class="card pending-block">
    ${block
      .map((group) => {
        const indent = `style="--indent:${group.depth * 16}px"`;
        const project = group.project;
        const adding = ctx.ui.addingProjectId === project.id;
        const taskRows = group.tasks.map((task) => taskRow(task, ctx, { project })).join('');
        return `<div class="pending-group" ${indent}>
          <button class="group-header" data-action="open-project" data-id="${escapeHtml(project.id)}">
            <span class="group-icon">${projectIconMarkup(project)}</span>
            <span class="group-title">${escapeHtml(project.title)}</span>
            <span class="group-count">${group.tasks.length || ''}</span>
            ${icon('chevron-right', 'chevron muted')}
          </button>
          ${taskRows}
          ${
            adding
              ? `<form class="inline-add" data-add-task data-project="${escapeHtml(project.id)}">
                  <input class="inline-input" name="title" placeholder="Agregar tarea…" maxlength="120" autocomplete="off" autocapitalize="sentences" enterkeyhint="done" aria-label="Nueva tarea en ${escapeHtml(project.title)}">
                  <button class="inline-submit" type="submit">${icon('check')}</button>
                </form>`
              : `<button class="inline-add-trigger" data-action="start-add-task" data-project="${escapeHtml(project.id)}">${icon('plus', 'tiny')} Agregar tarea…</button>`
          }
        </div>`;
      })
      .join('')}
  </section>`;
}

function renderProjects(ctx) {
  const { data, settings, ui } = ctx;
  const allRoots = data.projects.filter((project) => !project.parentId);
  const roots = allRoots
    .filter((project) => projectMatchesFilter(project, settings.filter))
    .sort((a, b) => compareProjectOrder(a, b));
  const active = roots.filter((project) => project.status === 'Activo');
  const paused = roots.filter((project) => project.status === 'Pausa' || project.status === 'Postergado');
  const closed = roots.filter((project) => isClosed(project));
  const activeCount = allRoots.filter((project) => project.status === 'Activo').length;
  const allExpanded = roots.length > 0 && roots.every((project) => ui.expandedProjects.has(project.id));
  const filterCount =
    (sameSet(settings.filter.statuses, ['Activo', 'Pausa', 'Postergado']) ? 0 : 1) +
    (settings.filter.priorities.length !== 4 ? 1 : 0);

  const controls = `<div class="toolbar">
    <button class="toolbar-link" data-action="filter-open">${filterCount ? `Filtros · ${filterCount}` : 'Filtros'}</button>
    <div class="toolbar-group">
      <label class="switch-label">Hechas <input type="checkbox" class="switch" data-action="toggle-show-done" ${settings.showDoneTasks ? 'checked' : ''}></label>
      <button class="toolbar-link" data-action="${allExpanded ? 'collapse-all-projects' : 'expand-all-projects'}">${allExpanded ? 'Colapsar Todo' : 'Expandir Todo'}</button>
    </div>
  </div>`;

  let body = '';
  if (!allRoots.length) {
    body = `<div class="empty-state">${icon('folder', 'empty-icon')}<h2>Todavía no tenés proyectos.</h2><p>Acá van a vivir tus proyectos, subproyectos y tareas.</p><button class="primary-button" data-action="new-project">Nuevo Proyecto</button></div>`;
  } else if (!roots.length) {
    body = `<div class="empty-state">${icon('folder', 'empty-icon')}<h2>Ningún proyecto coincide con el filtro.</h2><p>Probá con otros estados o prioridades.</p><button class="text-button" data-action="filter-reset">Quitar Filtros</button></div>`;
  } else {
    const section = (title, projects) =>
      projects.length
        ? `<h3 class="section-title">${title}</h3>${projects
            .map((project) => renderProjectCard(project, ctx))
            .join('')}`
        : '';
    body = `<div class="stack">${section('Activos', active)}${section('En Pausa y Postergados', paused)}${section('Terminados y Cancelados', closed)}</div>`;
  }

  return `<main class="screen has-fab">
    ${header({
      title: 'Mis Proyectos',
      subtitle: activeCount
        ? `Tenés ${activeCount} ${activeCount === 1 ? 'proyecto activo' : 'proyectos activos'}.`
        : null,
    })}
    ${controls}
    ${body}
    <button class="fab" data-action="new-project" aria-label="Nuevo Proyecto">${icon('plus')}</button>
  </main>`;
}

function renderProjectCard(project, ctx) {
  const { data, settings, ui } = ctx;
  const progress = progressFor(project.id, data.projects, data.tasks);
  const done = progress.done;
  const expanded = ui.expandedProjects.has(project.id);

  if (!expanded) {
    return `<button class="compact-row" data-action="toggle-expand-project" data-id="${escapeHtml(project.id)}">
      <span class="icon-tile ${project.parentId ? 'sub' : ''}">${projectIconMarkup(project)}</span>
      <span class="compact-main">
        <span class="compact-title-row">
          <span class="compact-title ${project.status === 'Cancelado' ? 'muted' : ''}">${escapeHtml(project.title)}</span>
          <span class="compact-count">${hasTasks(progress) ? `${done} de ${progress.total}` : 'Sin tareas'}</span>
        </span>
        ${progressBar(progress)}
      </span>
      ${icon('chevron-down', 'chevron muted')}
    </button>`;
  }

  const rows = projectTreeHtml(project, ctx, { includeRootChildren: true });
  const pending = Math.max(0, progress.total - done);
  const doneToggle =
    done > 0
      ? `<label class="toggle-row"><span>Ver Hechas (${done})</span><input type="checkbox" class="switch" data-action="toggle-show-done" ${settings.showDoneTasks ? 'checked' : ''}></label>`
      : '';

  return `<article class="card project-card">
    <div class="card-head">
      <button class="card-open" data-action="open-project" data-id="${escapeHtml(project.id)}">
        <span class="icon-tile ${project.parentId ? 'sub' : ''}">${projectIconMarkup(project)}</span>
        <span class="card-title">${escapeHtml(project.title)}</span>
      </button>
      <button class="icon-button" data-action="project-menu" data-id="${escapeHtml(project.id)}" aria-label="Más Opciones De ${escapeHtml(project.title)}">${icon('more')}</button>
    </div>
    ${progressPill(progress)}
    <div class="properties">
      <button class="property-row" data-action="project-menu" data-id="${escapeHtml(project.id)}">${icon('flag', 'property-icon')}<span>Prioridad</span>${priorityChip(project.priority)}</button>
      <button class="property-row" data-action="project-menu" data-id="${escapeHtml(project.id)}">${icon(statusMeta(project.status).icon, 'property-icon')}<span>Estado</span>${statusChip(project.status)}</button>
    </div>
    ${
      project.status === 'Terminado' && pending > 0
        ? `<p class="notice warning">Te quedan ${pending} tareas sin hacer.</p>`
        : ''
    }
    ${
      isComplete(progress) && project.status === 'Activo'
        ? `<div class="notice success"><span>Todo hecho. ¿Lo marcás como Terminado?</span><button class="notice-action" data-action="mark-project-done" data-id="${escapeHtml(project.id)}">Marcar Terminado</button></div>`
        : ''
    }
    ${doneToggle}
    ${
      rows
        ? `<div class="tree">${rows}</div>`
        : hasTasks(progress)
          ? `<p class="empty-inline">Ninguna tarea coincide con el filtro</p>`
          : ''
    }
    <div class="card-foot">
      <button class="text-button" data-action="open-project" data-id="${escapeHtml(project.id)}">Abrir Proyecto</button>
      <button class="text-button" data-action="collapse-project" data-id="${escapeHtml(project.id)}">Colapsar</button>
    </div>
  </article>`;
}

function renderProjectDetail(ctx) {
  const { data, settings, ui } = ctx;
  const project = data.projects.find((candidate) => candidate.id === ctx.route.id);
  if (!project) {
    return `<main class="screen">${detailHeader('Mis Proyectos')}<div class="empty-state">${icon('folder', 'empty-icon')}<h2>Ese proyecto ya no existe.</h2></div></main>`;
  }
  const progress = progressFor(project.id, data.projects, data.tasks);
  const pending = Math.max(0, progress.total - progress.done);
  const ancestors = projectAncestorsFor(project, data.projects);
  const rows = projectTreeHtml(project, ctx, { includeRootChildren: true });
  const doneToggle =
    progress.done > 0
      ? `<label class="toggle-row"><span>Ver Hechas (${progress.done})</span><input type="checkbox" class="switch" data-action="toggle-show-done" ${settings.showDoneTasks ? 'checked' : ''}></label>`
      : '';

  return `<main class="screen has-fab">
    ${detailHeader(
      `<span class="icon-tile ${project.parentId ? 'sub' : ''}">${projectIconMarkup(project)}</span>
       <span class="detail-title">${escapeHtml(project.title)}</span>
       <button class="icon-button" data-action="project-menu" data-id="${escapeHtml(project.id)}" aria-label="Más Opciones De ${escapeHtml(project.title)}">${icon('more')}</button>`,
    )}
    ${ancestors.length ? `<p class="breadcrumb">${ancestors.map((ancestor) => escapeHtml(ancestor.title)).join(' / ')}</p>` : ''}
    ${hasTasks(progress) ? progressPill(progress) : ''}
    <div class="properties card">
      <button class="property-row" data-action="project-menu" data-id="${escapeHtml(project.id)}">${icon('flag', 'property-icon')}<span>Prioridad</span>${priorityChip(project.priority)}</button>
      <button class="property-row" data-action="project-menu" data-id="${escapeHtml(project.id)}">${icon(statusMeta(project.status).icon, 'property-icon')}<span>Estado</span>${statusChip(project.status)}</button>
    </div>
    ${
      project.status === 'Terminado' && pending > 0
        ? `<p class="notice warning">Te quedan ${pending} tareas sin hacer.</p>`
        : ''
    }
    ${
      isComplete(progress) && project.status === 'Activo'
        ? `<div class="notice success"><span>Todo hecho. ¿Lo marcás como Terminado?</span><button class="notice-action" data-action="mark-project-done" data-id="${escapeHtml(project.id)}">Marcar Terminado</button></div>`
        : ''
    }
    ${doneToggle}
    ${
      rows
        ? `<div class="tree detail-tree">${rows}</div>`
        : `<div class="empty-state small">${icon('check-list', 'empty-icon')}<h2>Este proyecto no tiene tareas.</h2><p>Agregá la primera para empezar a medir el progreso.</p></div>`
    }
    <button class="fab" data-action="add-task-here" data-project="${escapeHtml(project.id)}" aria-label="Agregar Tarea">${icon('plus')}</button>
  </main>`;
}

function renderLists(ctx) {
  const { data, settings } = ctx;
  const roots = data.lists.filter((list) => !list.parentId).sort(compareLists);
  const allExpanded = roots.length > 0 && roots.every((list) => ctx.ui.expandedLists.has(list.id));
  const body = roots.length
    ? `<div class="stack">${roots.map((list) => renderListCard(list, ctx)).join('')}</div>`
    : `<div class="empty-state">${icon('list', 'empty-icon')}<h2>Todavía no tenés listas.</h2><p>Armá una para las compras, las películas o lo que se te ocurra.</p><button class="primary-button" data-action="new-list">Nueva Lista</button></div>`;

  return `<main class="screen has-fab">
    ${header({ title: 'Mis Listas' })}
    ${
      roots.length
        ? `<div class="toolbar"><span></span><button class="toolbar-link" data-action="${allExpanded ? 'collapse-all-lists' : 'expand-all-lists'}">${allExpanded ? 'Colapsar Todo' : 'Expandir Todo'}</button></div>`
        : ''
    }
    ${body}
    <button class="fab" data-action="new-list" aria-label="Nueva Lista">${icon('plus')}</button>
  </main>`;
}

function renderListCard(list, ctx) {
  const { data, settings, ui } = ctx;
  const expanded = ui.expandedLists.has(list.id);
  const progress = listProgress(list, data.lists);
  const linkedProject = list.projectId
    ? data.projects.find((project) => project.id === list.projectId)
    : null;

  if (!expanded) {
    return `<button class="compact-row" data-action="toggle-expand-list" data-id="${escapeHtml(list.id)}">
      <span class="icon-tile sub">${icon('list')}</span>
      <span class="compact-main">
        <span class="compact-title-row">
          <span class="compact-title">${escapeHtml(list.title)}</span>
          <span class="compact-count">${progress.total ? `${progress.done} de ${progress.total}` : 'Sin ítems'}</span>
        </span>
        ${listProgressBar(progress)}
      </span>
      ${icon('chevron-down', 'chevron muted')}
    </button>`;
  }

  const directItems = sortedItems(list).filter((item) => settings.showDoneListItems || !item.done);
  const sublists = childListsOf(list.id, data.lists);
  const preview = [
    ...directItems.map((item) =>
      itemRow(item, {
        reorder: false,
        showHandle: false,
      }),
    ),
    ...sublists.map((sub) => {
      const subProgress = listProgress(sub, data.lists);
      return `<button class="sublist-row" data-action="open-list" data-id="${escapeHtml(sub.id)}">
        <span class="sublist-icon">${icon('list')}</span>
        <span class="sublist-title">${escapeHtml(sub.title)}</span>
        <span class="muted small">${subProgress.total ? `${subProgress.done} de ${subProgress.total}` : 'Sin ítems'}</span>
        ${icon('chevron-right', 'chevron muted')}
      </button>`;
    }),
  ];
  const visible = preview.slice(0, 6);
  const remaining = preview.length - visible.length;

  return `<article class="card list-card">
    <div class="card-head">
      <button class="card-open" data-action="open-list" data-id="${escapeHtml(list.id)}">
        <span class="icon-tile sub">${icon('list')}</span>
        <span class="card-title">${escapeHtml(list.title)}</span>
      </button>
      <button class="icon-button" data-action="list-menu" data-id="${escapeHtml(list.id)}" aria-label="Más Opciones De ${escapeHtml(list.title)}">${icon('more')}</button>
    </div>
    ${listProgressBar(progress)}
    ${linkedProject ? `<button class="link-chip" data-action="open-project" data-id="${escapeHtml(linkedProject.id)}">${icon('folder', 'tiny')} En «${escapeHtml(linkedProject.title)}»</button>` : ''}
    ${visible.length ? `<div class="list-preview">${visible.join('')}</div>` : `<p class="empty-inline">Todavía no tenés ítems.</p>`}
    ${remaining > 0 ? `<button class="text-button" data-action="open-list" data-id="${escapeHtml(list.id)}">+${remaining} Más</button>` : ''}
    <div class="card-foot">
      <button class="text-button" data-action="open-list" data-id="${escapeHtml(list.id)}">Ver Detalle</button>
      <button class="text-button" data-action="collapse-list" data-id="${escapeHtml(list.id)}">Colapsar</button>
    </div>
  </article>`;
}

function renderListDetail(ctx) {
  const { data, settings, ui } = ctx;
  const list = data.lists.find((candidate) => candidate.id === ctx.route.id);
  if (!list) {
    return `<main class="screen">${detailHeader('Mis Listas')}<div class="empty-state">${icon('list', 'empty-icon')}<h2>Esa lista ya no existe.</h2></div></main>`;
  }
  const linkedProject = list.projectId
    ? data.projects.find((project) => project.id === list.projectId)
    : null;
  const directItems = sortedItems(list);
  const sublists = childListsOf(list.id, data.lists);
  const rows = buildDetailRows(list, data.lists, directItems, sublists, settings, ui);
  const progress = listProgress(list, data.lists);

  return `<main class="screen has-fab">
    ${detailHeader(
      `<span class="icon-tile sub">${icon('list')}</span>
       <span class="detail-title">${escapeHtml(list.title)}</span>
       <button class="icon-button" data-action="list-menu" data-id="${escapeHtml(list.id)}" aria-label="Más Opciones De ${escapeHtml(list.title)}">${icon('more')}</button>`,
    )}
    ${linkedProject ? `<button class="link-chip" data-action="open-project" data-id="${escapeHtml(linkedProject.id)}">${icon('folder', 'tiny')} En «${escapeHtml(linkedProject.title)}»</button>` : ''}
    <div class="list-summary">${listProgressBar(progress)}<span class="list-count">${progress.total ? `${progress.done} de ${progress.total}` : 'Sin ítems'}</span></div>
    ${
      progress.done > 0
        ? `<label class="toggle-row"><span>Ver Hechos (${progress.done})</span><input type="checkbox" class="switch" data-action="toggle-show-done-items" ${settings.showDoneListItems ? 'checked' : ''}></label>`
        : ''
    }
    ${rows ? `<div class="list-rows" data-reorder-list="${escapeHtml(list.id)}">${rows}</div>` : `<div class="empty-state small">${icon('check-list', 'empty-icon')}<h2>Todavía no tenés ítems.</h2><p>Agregá el primero abajo.</p></div>`}
    <form class="inline-add bottom-add" data-add-item data-list="${escapeHtml(list.id)}">
      <input class="inline-input" name="title" placeholder="Agregar ítem…" maxlength="200" autocomplete="off" enterkeyhint="done" aria-label="Agregar ítem a ${escapeHtml(list.title)}">
      <button class="inline-submit" type="submit" aria-label="Agregar">${icon('plus')}</button>
    </form>
    <button class="fab" data-action="add-item-focus" aria-label="Agregar Ítem">${icon('plus')}</button>
  </main>`;
}

function buildDetailRows(list, lists, directItems, sublists, settings, ui) {
  const visibleItems = directItems.filter((item) => settings.showDoneListItems || !item.done);
  const visibleSublists = sublists;
  const rowObjects = [
    ...visibleItems.map((item) => ({ kind: 'item', position: item.position, item })),
    ...visibleSublists.map((sub) => ({ kind: 'list', position: sub.position, list: sub })),
  ].sort((a, b) => {
    if (a.position !== b.position) return a.position - b.position;
    if (a.kind !== b.kind) return a.kind === 'item' ? -1 : 1;
    return 0;
  });

  return rowObjects
    .map((row) => {
      if (row.kind === 'item') {
        return itemRow(row.item, { reorder: !row.item.done, showHandle: !row.item.done });
      }
      const sub = row.list;
      const subProgress = listProgress(sub, lists);
      const collapsed = ui.collapsedLists.has(sub.id);
      const childRows = collapsed
        ? ''
        : buildDetailRows(
            { ...sub, items: sub.items || [] },
            lists,
            sortedItems(sub),
            childListsOf(sub.id, lists),
            settings,
            ui,
          );
      return `<div class="sublist-block">
        <div class="sublist-row" data-reorder-id="${escapeHtml(sub.id)}">
          <button class="sublist-toggle" data-action="toggle-collapse-list" data-id="${escapeHtml(sub.id)}" aria-label="${collapsed ? 'Expandir' : 'Colapsar'} ${escapeHtml(sub.title)}">${icon('chevron-down', `chevron ${collapsed ? 'collapsed' : ''}`)}</button>
          <button class="sublist-inner" data-action="open-list" data-id="${escapeHtml(sub.id)}">
            <span class="sublist-icon">${icon('list')}</span>
            <span class="sublist-title">${escapeHtml(sub.title)}</span>
            <span class="muted small">${subProgress.total ? `${subProgress.done} de ${subProgress.total}` : 'Sin ítems'}</span>
          </button>
          <span class="drag-handle" data-drag-handle aria-label="Mover ${escapeHtml(sub.title)}">${icon('drag', 'tiny')}</span>
        </div>
        ${childRows ? `<div class="sublist-children">${childRows}</div>` : ''}
      </div>`;
    })
    .join('');
}

function itemRow(item, options = {}) {
  const { reorder = false, showHandle = false } = options;
  const handle = showHandle
    ? `<span class="drag-handle" data-drag-handle aria-label="Mover ${escapeHtml(item.title)}">${icon('drag', 'tiny')}</span>`
    : '<span class="drag-spacer"></span>';
  return `<div class="item-row ${item.done ? 'is-done' : ''}" data-reorder-id="${escapeHtml(item.id)}">
    <button class="task-check ${item.done ? 'is-checked' : ''}" data-action="toggle-item" data-id="${escapeHtml(item.id)}" aria-label="${escapeHtml(item.title)}, ${item.done ? 'hecho' : 'pendiente'}">${item.done ? icon('check', 'check-mark') : ''}</button>
    <button class="item-title" data-action="edit-item" data-id="${escapeHtml(item.id)}">${escapeHtml(item.title)}</button>
    ${reorder ? handle : '<span class="drag-spacer"></span>'}
  </div>`;
}

function renderSettings(ctx) {
  const { settings } = ctx;
  const themes = [
    { value: 'light', label: 'Claro', icon: 'sun' },
    { value: 'dark', label: 'Oscuro', icon: 'moon' },
    { value: 'system', label: 'Seguir al Sistema', icon: 'phone' },
  ];
  return `<main class="screen settings-screen">
    ${header({ title: 'Ajustes' })}
    <section class="settings-group">
      <h3 class="section-title">Tema</h3>
      <div class="segmented" role="group" aria-label="Tema">
        ${themes
          .map(
            (theme) => `<button class="segmented-item ${settings.theme === theme.value ? 'is-active' : ''}" data-action="set-theme" data-theme="${theme.value}">${icon(theme.icon, 'tiny')}<span>${theme.label}</span></button>`,
          )
          .join('')}
      </div>
      <p class="help">Se aplica al instante y queda guardado en este iPhone.</p>
    </section>

    <section class="settings-group">
      <h3 class="section-title">Tus Datos</h3>
      <div class="settings-card">
        <button class="settings-row" data-action="export-data">${icon('download', 'settings-icon')}<span class="settings-main"><strong>Exportar Todo</strong><small>Guardá un archivo JSON con todo.</small></span>${icon('chevron-right', 'chevron muted')}</button>
        <button class="settings-row" data-action="import-data">${icon('upload', 'settings-icon')}<span class="settings-main"><strong>Importar JSON</strong><small>Reemplazá todo con un archivo exportado antes.</small></span>${icon('chevron-right', 'chevron muted')}</button>
      </div>
    </section>

    <section class="settings-group">
      <h3 class="section-title">Privacidad</h3>
      <div class="privacy-card">
        ${icon('shield', 'privacy-icon')}
        <div>
          <strong>Tus datos quedan en este iPhone.</strong>
          <p>List Master no tiene cuenta, nube, analytics ni servidor. Las tareas se guardan en el almacenamiento local del navegador y nunca se envían a ningún lado.</p>
          <p>El sitio solo entrega los archivos de la app. No puede leer lo que escribís. Igual, hacé un export de vez en cuando: si borrás la app o el sistema limpia los datos, el archivo es tu respaldo.</p>
        </div>
      </div>
    </section>

    <section class="settings-group">
      <h3 class="section-title">Instalar en iPhone</h3>
      <div class="settings-card install-card">
        <ol>
          <li>Abrí esta dirección en <strong>Safari</strong>.</li>
          <li>Tocá <strong>Compartir</strong> y después <strong>Agregar a Inicio</strong>.</li>
          <li>Usala siempre desde el ícono: ahí los datos viven separados de una pestaña normal.</li>
        </ol>
        <p class="help">Si ya cargaste datos en una pestaña de Safari, exportalos ahí e importalos dentro del ícono.</p>
      </div>
    </section>

    <section class="settings-group about">
      <h3 class="section-title">Acerca De</h3>
      <div class="settings-card">
        <div class="settings-row static"><span class="settings-main"><strong>List Master</strong><small>Versión ${ctx.version} · PWA</small></span></div>
        <div class="settings-row static"><span class="settings-main"><strong>Hecho Para Vos</strong><small>Sin cuenta, sin nube, sin analytics.</small></span></div>
      </div>
    </section>
  </main>`;
}

// Componentes

export function header({ title, subtitle = null }) {
  return `<header class="page-header"><h1>${escapeHtml(title)}</h1>${subtitle ? `<p class="subtitle">${escapeHtml(subtitle)}</p>` : ''}</header>`;
}

function detailHeader(content) {
  return `<header class="detail-header"><button class="icon-button" data-action="back" aria-label="Volver">${icon('back')}</button>${content}</header>`;
}

export function progressPill(progress) {
  if (!hasTasks(progress)) {
    return `<div class="progress-pill is-empty"><span>Sin tareas todavía</span></div>`;
  }
  const percentage = progressPercentage(progress);
  return `<div class="progress-pill ${percentage === 100 ? 'is-complete' : ''}">
    <div class="progress-text"><strong>${progress.done} de ${progress.total}</strong><span>${percentage}%</span></div>
    <div class="progress-track"><div class="progress-fill" style="width:${percentage}%"></div></div>
  </div>`;
}

function progressBar(progress) {
  if (!hasTasks(progress)) return `<span class="thin-bar"><span></span></span>`;
  return `<span class="thin-bar"><span style="width:${progressPercentage(progress)}%"></span></span>`;
}

function listProgressBar(progress) {
  const percentage = progress.total ? Math.round((progress.done * 100) / progress.total) : 0;
  return `<span class="thin-bar list-bar"><span style="width:${percentage}%"></span></span>`;
}

export function statusChip(status) {
  const meta = statusMeta(status);
  return `<span class="chip chip-status tone-${meta.tone}">${icon(meta.icon, 'chip-icon')}${escapeHtml(meta.label)}</span>`;
}

export function priorityChip(priority) {
  const meta = priorityMeta(priority);
  if (!meta) return `<span class="chip chip-priority tone-neutral">${icon('flag', 'chip-icon')}Sin Prioridad</span>`;
  return `<span class="chip chip-priority tone-${meta.tone}">${icon('flag', 'chip-icon')}${escapeHtml(meta.label)}</span>`;
}

function taskRow(task, ctx, { project }) {
  const due = task.dueDate ? formatShortDate(task.dueDate) : null;
  const overdue = task.dueDate && !task.done && isOverdue(task.dueDate);
  return `<div class="task-row ${task.done ? 'is-done' : ''}">
    <button class="task-check ${task.done ? 'is-checked' : ''}" data-action="toggle-task" data-id="${escapeHtml(task.id)}" aria-label="${escapeHtml(task.title)}, ${task.done ? 'hecha' : 'pendiente'}">${task.done ? icon('check', 'check-mark') : ''}</button>
    <button class="task-main" data-action="open-task" data-id="${escapeHtml(task.id)}" data-project="${escapeHtml(project.id)}">
      <span class="task-title">${escapeHtml(task.title)}</span>
      ${
        due || task.priority
          ? `<span class="task-meta">${task.priority ? `<span class="priority-dot tone-${priorityMeta(task.priority).tone}"></span>` : ''}${due ? `<span class="${overdue ? 'overdue' : ''}">${icon('calendar', 'tau')}${due}</span>` : ''}</span>`
          : ''
      }
    </button>
  </div>`;
}

function projectTreeHtml(project, ctx, { includeRootChildren }) {
  const { data, settings } = ctx;
  const tasksByProject = new Map();
  data.tasks.forEach((task) => {
    if (!tasksByProject.has(task.projectId)) tasksByProject.set(task.projectId, []);
    tasksByProject.get(task.projectId).push(task);
  });

  function renderLevel(current, depth) {
    const ownTasks = sortTasks(tasksByProject.get(current.id) || []).filter(
      (task) => settings.showDoneTasks || !task.done,
    );
    const subprojects = childrenOf(current.id, data.projects);
    const items = [
      ...ownTasks.map((task) => ({ kind: 'task', task })),
      ...subprojects.map((subproject) => ({ kind: 'project', project: subproject })),
    ];
    if (!items.length) return '';
    return items
      .map((item) => {
        if (item.kind === 'task') {
          return `<div class="tree-task" style="--depth:${depth}">${taskRow(item.task, ctx, { project: current })}</div>`;
        }
        const sub = item.project;
        const subProgress = progressFor(sub.id, data.projects, data.tasks);
        return `<div class="tree-project" style="--depth:${depth}">
          <button class="subproject-row" data-action="open-project" data-id="${escapeHtml(sub.id)}">
            <span class="tree-branch"></span>
            <span class="icon-tile small ${sub.parentId ? 'sub' : ''}">${projectIconMarkup(sub)}</span>
            <span class="subproject-title ${sub.status === 'Cancelado' ? 'muted' : ''}">${escapeHtml(sub.title)}</span>
            <span class="subproject-count">${hasTasks(subProgress) ? `${subProgress.done} de ${subProgress.total}` : 'Sin tareas'}</span>
            ${icon('chevron-right', 'chevron muted')}
          </button>
          <div class="subproject-children" style="--depth:${depth + 1}">${renderLevel(sub, depth + 1)}</div>
        </div>`;
      })
      .join('');
  }

  return includeRootChildren ? renderLevel(project, 0) : '';
}

function projectAncestorsFor(project, projects) {
  const index = new Map(projects.map((candidate) => [candidate.id, candidate]));
  const result = [];
  let current = index.get(project.id);
  const visited = new Set([project.id]);
  while (current && current.parentId) {
    if (visited.has(current.parentId)) break;
    current = index.get(current.parentId);
    if (!current) break;
    visited.add(current.id);
    result.unshift(current);
  }
  return result;
}

function projectMatchesFilter(project, filter) {
  const statuses = filter.statuses || [];
  const priorities = filter.priorities || [];
  return statuses.includes(project.status) && priorities.includes(project.priority ?? null);
}

function compareProjectOrder(a, b) {
  const rank = (priority) => (priority === 'Alta' ? 0 : priority === 'Media' ? 1 : priority === 'Baja' ? 2 : 3);
  const diff = rank(a.priority) - rank(b.priority);
  if (diff !== 0) return diff;
  if (a.createdAt !== b.createdAt) return a.createdAt < b.createdAt ? -1 : 1;
  return a.id < b.id ? -1 : 1;
}

function sameSet(values, expected) {
  if (!Array.isArray(values) || values.length !== expected.length) return false;
  return expected.every((value) => values.includes(value));
}

export function formatShortDate(isoDate) {
  const [year, month, day] = isoDate.split('-').map(Number);
  const date = new Date(year, month - 1, day);
  const text = new Intl.DateTimeFormat('es-AR', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
  }).format(date);
  return capitalize(text.replaceAll('.', ''));
}

export function formatLongDate(isoDate) {
  const [year, month, day] = isoDate.split('-').map(Number);
  const date = new Date(year, month - 1, day);
  const text = new Intl.DateTimeFormat('es-AR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  }).format(date);
  return capitalize(text);
}

export function isOverdue(isoDate) {
  const today = new Date();
  const local = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
  return isoDate < local;
}

function capitalize(text) {
  return text.charAt(0).toUpperCase() + text.slice(1);
}

export function projectOptionsForPicker(projects, currentId = null) {
  return flattenProjects(projects).filter((row) => row.project.id !== currentId);
}

export function listOptionsForPicker(lists, currentId = null) {
  const result = [];
  const walk = (parentId, depth) => {
    childListsOf(parentId, lists).forEach((list) => {
      if (list.id !== currentId) {
        result.push({ list, depth });
        walk(list.id, depth + 1);
      }
    });
  };
  walk(null, 0);
  return result;
}

export function listDepthSafe(list, lists) {
  // Se usa en el editor para decidir si todavía puede tener sublistas.
  return list ? canHaveSubprojectSafe(list.id, lists) : true;
}

function canHaveSubprojectSafe(listId, lists) {
  const index = new Map(lists.map((list) => [list.id, list]));
  let current = index.get(listId);
  if (!current) return false;
  let depth = 0;
  const visited = new Set([listId]);
  while (current.parentId) {
    if (visited.has(current.parentId)) return false;
    current = index.get(current.parentId);
    if (!current) return false;
    visited.add(current.id);
    depth += 1;
  }
  return depth < 3;
}

export { canHaveSubproject, subtreeIds, isClosed };
