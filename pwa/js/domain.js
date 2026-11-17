// Reglas puras de List Master. Sin DOM, sin red y sin almacenamiento: así se
// pueden probar con Node y reutilizar desde la app y desde el import/export.

export const MAX_TITLE_LENGTH = 120;
export const MAX_ITEM_TITLE_LENGTH = 200;
export const MAX_SUBPROJECT_DEPTH = 3;
export const MAX_SUBLIST_DEPTH = 3;
export const MAX_ICON_LENGTH = 32;
export const MAX_DESCRIPTION_LENGTH = 2000;

export const PROJECT_STATUSES = [
  { value: 'Activo', label: 'Activo', tone: 'active', icon: 'status-active' },
  { value: 'Pausa', label: 'En Pausa', tone: 'neutral', icon: 'status-paused' },
  { value: 'Postergado', label: 'Postergado', tone: 'warning', icon: 'status-postponed' },
  { value: 'Terminado', label: 'Terminado', tone: 'success', icon: 'status-done' },
  { value: 'Cancelado', label: 'Cancelado', tone: 'danger', icon: 'status-cancelled' },
];

export const PRIORITIES = [
  { value: 'Alta', label: 'Alta', tone: 'danger' },
  { value: 'Media', label: 'Media', tone: 'warning' },
  { value: 'Baja', label: 'Baja', tone: 'neutral' },
];

export const CLOSED_STATUSES = new Set(['Terminado', 'Cancelado']);
export const DEFAULT_STATUSES = ['Activo', 'Pausa', 'Postergado'];
export const ALL_PRIORITIES = ['Alta', 'Media', 'Baja', null];

export function isClosed(project) {
  return CLOSED_STATUSES.has(project.status);
}

export function statusMeta(value) {
  return PROJECT_STATUSES.find((item) => item.value === value) || PROJECT_STATUSES[0];
}

export function priorityMeta(value) {
  return PRIORITIES.find((item) => item.value === value) || null;
}

export function priorityRank(priority) {
  if (priority === 'Alta') return 0;
  if (priority === 'Media') return 1;
  if (priority === 'Baja') return 2;
  return 3;
}

/** Alta primero; a igual prioridad, la más antigua; el id desempata. */
export function compareTasks(a, b) {
  const rank = priorityRank(a.priority) - priorityRank(b.priority);
  if (rank !== 0) return rank;
  const created = compareText(a.createdAt, b.createdAt);
  if (created !== 0) return created;
  return compareText(a.id, b.id);
}

export function sortTasks(tasks) {
  return [...tasks].sort(compareTasks);
}

/** Prioridad y antigüedad para proyectos hermanos. */
export function compareProjects(a, b) {
  const rank = priorityRank(a.priority) - priorityRank(b.priority);
  if (rank !== 0) return rank;
  const created = compareText(a.createdAt, b.createdAt);
  if (created !== 0) return created;
  return compareText(a.id, b.id);
}

export function compareLists(a, b) {
  if (a.position !== b.position) return a.position - b.position;
  const created = compareText(a.createdAt, b.createdAt);
  if (created !== 0) return created;
  return compareText(a.id, b.id);
}

function compareText(a = '', b = '') {
  return a < b ? -1 : a > b ? 1 : 0;
}

export function childrenOf(parentId, projects) {
  return projects.filter((project) => project.parentId === parentId).sort(compareProjects);
}

export function projectIndex(projects) {
  return new Map(projects.map((project) => [project.id, project]));
}

/** Nivel del proyecto: 0 raíz, 1..3 subproyectos. -1 si no existe o hay ciclo. */
export function projectDepth(projectId, projects) {
  const index = projectIndex(projects);
  let current = index.get(projectId);
  if (!current) return -1;
  let depth = 0;
  const visited = new Set([projectId]);
  while (true) {
    if (!current.parentId) return depth;
    const parent = index.get(current.parentId);
    if (!parent) return -1;
    if (visited.has(parent.id)) return -1;
    visited.add(parent.id);
    depth += 1;
    current = parent;
  }
}

export function canHaveSubproject(projectId, projects) {
  const depth = projectDepth(projectId, projects);
  return depth >= 0 && depth < MAX_SUBPROJECT_DEPTH;
}

export function wouldCreateCycle(projectId, newParentId, projects) {
  if (!newParentId) return false;
  if (projectId === newParentId) return true;
  const index = projectIndex(projects);
  let current = index.get(newParentId);
  if (!current) return false;
  const visited = new Set([newParentId]);
  while (true) {
    if (current.id === projectId) return true;
    if (!current.parentId) return false;
    if (visited.has(current.parentId)) return true;
    visited.add(current.parentId);
    current = index.get(current.parentId);
    if (!current) return false;
  }
}

/** IDs del proyecto y todos sus descendientes. Incluye subárboles cancelados. */
export function subtreeIds(rootId, projects) {
  const index = projectIndex(projects);
  if (!index.has(rootId)) return new Set();
  const result = new Set();
  const pending = [rootId];
  while (pending.length) {
    const id = pending.shift();
    if (result.has(id)) continue;
    result.add(id);
    childrenOf(id, projects).forEach((child) => pending.push(child.id));
  }
  return result;
}

export function rootOf(projectId, projects) {
  const index = projectIndex(projects);
  let current = index.get(projectId);
  if (!current) return null;
  const visited = new Set([projectId]);
  while (true) {
    if (!current.parentId) return current;
    if (visited.has(current.parentId)) return null;
    current = index.get(current.parentId);
    if (!current) return null;
    visited.add(current.id);
  }
}

export function subtreeHeight(projectId, projects) {
  const byParent = groupBy(projects, (project) => project.parentId);
  function height(id, seen) {
    const children = (byParent.get(id) || []).filter((child) => !seen.has(child.id));
    if (!children.length) return 0;
    return 1 + Math.max(...children.map((child) => height(child.id, new Set(seen).add(id))));
  }
  return height(projectId, new Set());
}

/**
 * Padres posibles para mover un proyecto: fuera de su propio subárbol y sin
 * que el árbol movido pase de tres niveles.
 */
export function validProjectParents(projectId, projects) {
  if (!projects.some((project) => project.id === projectId)) return [];
  const own = subtreeIds(projectId, projects);
  const height = subtreeHeight(projectId, projects);
  return projects
    .filter((project) => !own.has(project.id))
    .filter((project) => projectDepth(project.id, projects) + 1 + height <= MAX_SUBPROJECT_DEPTH)
    .sort((a, b) => {
      const depth = projectDepth(a.id, projects) - projectDepth(b.id, projects);
      if (depth !== 0) return depth;
      return compareProjects(a, b);
    });
}

/** Ancestros desde la raíz hasta el padre inmediato. */
export function projectAncestors(projectId, projects) {
  const index = projectIndex(projects);
  const result = [];
  const visited = new Set([projectId]);
  let current = index.get(projectId);
  if (!current) return result;
  while (true) {
    if (!current.parentId) break;
    if (visited.has(current.parentId)) break;
    current = index.get(current.parentId);
    if (!current) break;
    visited.add(current.id);
    result.unshift(current);
  }
  return result;
}

/** Proyectos en orden de árbol, con su nivel. */
export function flattenProjects(projects, { includeClosed = true } = {}) {
  const byParent = groupBy(projects, (project) => project.parentId);
  const result = [];
  function visit(project, depth) {
    if (!includeClosed && isClosed(project)) return;
    result.push({ project, depth });
    (byParent.get(project.id) || [])
      .slice()
      .sort(compareProjects)
      .forEach((child) => visit(child, depth + 1));
  }
  (byParent.get(null) || [])
    .slice()
    .sort(compareProjects)
    .forEach((project) => visit(project, 0));
  const seen = new Set(result.map((row) => row.project.id));
  projects.filter((project) => !seen.has(project.id)).forEach((project) => {
    if (includeClosed || !isClosed(project)) result.push({ project, depth: 0 });
  });
  return result;
}

/** Progreso calculado: los subárboles cancelados no cuentan. */
export function progressFor(rootId, projects, tasks) {
  const root = projects.find((project) => project.id === rootId);
  if (!root || root.status === 'Cancelado') return { done: 0, total: 0 };
  const included = new Set();
  const pending = [root];
  while (pending.length) {
    const project = pending.shift();
    if (project.status === 'Cancelado') continue;
    if (included.has(project.id)) continue;
    included.add(project.id);
    childrenOf(project.id, projects).forEach((child) => pending.push(child));
  }
  const scoped = tasks.filter((task) => included.has(task.projectId));
  const done = scoped.filter((task) => task.done).length;
  const total = scoped.length;
  return { done, total };
}

export function progressPercentage(progress) {
  if (!progress.total) return 0;
  if (progress.done === progress.total) return 100;
  return Math.min(99, Math.round((progress.done * 100) / progress.total));
}

export function hasTasks(progress) {
  return progress.total > 0;
}

export function isComplete(progress) {
  return progress.total > 0 && progress.done === progress.total;
}

/** Un grupo de Pendientes: proyecto y solo sus tareas propias sin hacer. */
export function pendingGroups(projects, tasks) {
  const childrenByParent = groupBy(projects, (project) => project.parentId);
  const pendingByProject = groupBy(
    tasks.filter((task) => !task.done),
    (task) => task.projectId,
  );
  const result = [];

  function visit(project, depth, visited) {
    if (isClosed(project)) return false;
    const mark = result.length;
    const own = sortTasks(pendingByProject.get(project.id) || []);
    result.push({ project, depth, tasks: own });

    let childHadPending = false;
    (childrenByParent.get(project.id) || [])
      .filter((child) => !visited.has(child.id))
      .sort(compareProjects)
      .forEach((child) => {
        if (visit(child, depth + 1, new Set(visited).add(project.id))) {
          childHadPending = true;
        }
      });

    if (!own.length && !childHadPending) {
      result.length = mark;
      return false;
    }
    return true;
  }

  (childrenByParent.get(null) || []).sort(compareProjects).forEach((project) => {
    visit(project, 0, new Set());
  });
  return result;
}

export function pendingCount(groups) {
  return groups.reduce((sum, group) => sum + group.tasks.length, 0);
}

// Listas

export function listIndex(lists) {
  return new Map(lists.map((list) => [list.id, list]));
}

export function childListsOf(parentId, lists) {
  return lists.filter((list) => list.parentId === parentId).sort(compareLists);
}

export function listDepth(listId, lists) {
  const index = listIndex(lists);
  let current = index.get(listId);
  if (!current) return -1;
  let depth = 0;
  const visited = new Set([listId]);
  while (true) {
    if (!current.parentId) return depth;
    const parent = index.get(current.parentId);
    if (!parent) return -1;
    if (visited.has(parent.id)) return -1;
    visited.add(parent.id);
    depth += 1;
    current = parent;
  }
}

export function canHaveSublist(listId, lists) {
  const depth = listDepth(listId, lists);
  return depth >= 0 && depth < MAX_SUBLIST_DEPTH;
}

export function listSubtreeIds(rootId, lists) {
  if (!lists.some((list) => list.id === rootId)) return new Set();
  const result = new Set();
  const pending = [rootId];
  while (pending.length) {
    const id = pending.shift();
    if (result.has(id)) continue;
    result.add(id);
    childListsOf(id, lists).forEach((child) => pending.push(child.id));
  }
  return result;
}

export function listSubtreeHeight(listId, lists) {
  const byParent = groupBy(lists, (list) => list.parentId);
  function height(id, seen) {
    const children = (byParent.get(id) || []).filter((child) => !seen.has(child.id));
    if (!children.length) return 0;
    return 1 + Math.max(...children.map((child) => height(child.id, new Set(seen).add(id))));
  }
  return height(listId, new Set());
}

export function validListParents(listId, lists) {
  if (!lists.some((list) => list.id === listId)) return [];
  const own = listSubtreeIds(listId, lists);
  const height = listSubtreeHeight(listId, lists);
  return lists
    .filter((list) => !own.has(list.id))
    .filter((list) => listDepth(list.id, lists) + 1 + height <= MAX_SUBLIST_DEPTH)
    .sort((a, b) => {
      const depth = listDepth(a.id, lists) - listDepth(b.id, lists);
      if (depth !== 0) return depth;
      return compareLists(a, b);
    });
}

export function flattenLists(lists) {
  const result = [];
  function visit(parentId, depth) {
    childListsOf(parentId, lists).forEach((list) => {
      result.push({ list, depth });
      visit(list.id, depth + 1);
    });
  }
  visit(null, 0);
  const seen = new Set(result.map((row) => row.list.id));
  lists.filter((list) => !seen.has(list.id)).forEach((list) => result.push({ list, depth: 0 }));
  return result;
}

/** Ítems pendientes primero y, a igual estado, por posición. */
export function sortedItems(list) {
  return [...(list.items || [])].sort((a, b) => {
    if (a.done !== b.done) return a.done ? 1 : -1;
    if (a.position !== b.position) return a.position - b.position;
    return compareText(a.id, b.id);
  });
}

/** Filas del detalle: ítems y sublistas intercalados por posición. */
export function buildListRows(list, lists, collapsedIds = new Set()) {
  const items = (list.items || []).map((item) => ({ kind: 'item', position: item.position, item }));
  const sublists = childListsOf(list.id, lists).map((sub) => ({
    kind: 'list',
    position: sub.position,
    list: sub,
  }));
  const rows = [...items, ...sublists].sort((a, b) => {
    if (a.position !== b.position) return a.position - b.position;
    if (a.kind !== b.kind) return a.kind === 'item' ? -1 : 1;
    const aId = a.kind === 'item' ? a.item.id : a.list.id;
    const bId = b.kind === 'item' ? b.item.id : b.list.id;
    return compareText(aId, bId);
  });
  if (!collapsedIds || !collapsedIds.size) return rows;
  return rows.filter((row) => row.kind === 'item' || !collapsedIds.has(row.list.id));
}

export function listProgress(list, lists) {
  let done = 0;
  let total = 0;
  const ids = listSubtreeIds(list.id, lists);
  // Los ítems de sublistas también cuentan para el progreso de la lista madre.
  lists.filter((candidate) => ids.has(candidate.id)).forEach((candidate) => {
    total += (candidate.items || []).length;
    done += (candidate.items || []).filter((item) => item.done).length;
  });
  return { done, total };
}

// Utilidades

export function groupBy(items, keyFn) {
  const map = new Map();
  items.forEach((item) => {
    const key = keyFn(item);
    if (!map.has(key)) map.set(key, []);
    map.get(key).push(item);
  });
  return map;
}

export function quote(title) {
  return `«${title}»`;
}

export function normalizeTitle(raw, max = MAX_TITLE_LENGTH) {
  const clean = String(raw ?? '').trim();
  if (!clean) throw new DomainError('El título no puede estar vacío.');
  if (clean.length > max) throw new DomainError(`El título no puede superar los ${max} caracteres.`);
  return clean;
}

export function normalizeDescription(raw) {
  const clean = String(raw ?? '').trim();
  if (clean.length > MAX_DESCRIPTION_LENGTH) {
    throw new DomainError(`La descripción no puede superar los ${MAX_DESCRIPTION_LENGTH} caracteres.`);
  }
  return clean;
}

export function normalizeIcon(raw) {
  const clean = String(raw ?? '').trim();
  if (clean.length > MAX_ICON_LENGTH) {
    throw new DomainError(`El ícono no puede superar los ${MAX_ICON_LENGTH} caracteres.`);
  }
  return clean || null;
}

export function newId() {
  if (globalThis.crypto && typeof globalThis.crypto.randomUUID === 'function') {
    return globalThis.crypto.randomUUID();
  }
  const bytes = new Uint8Array(16);
  if (globalThis.crypto && globalThis.crypto.getRandomValues) {
    globalThis.crypto.getRandomValues(bytes);
  } else {
    for (let i = 0; i < bytes.length; i += 1) bytes[i] = Math.floor(Math.random() * 256);
  }
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = [...bytes].map((byte) => byte.toString(16).padStart(2, '0'));
  return `${hex.slice(0, 4).join('')}-${hex.slice(4, 6).join('')}-${hex.slice(6, 8).join('')}-${hex.slice(8, 10).join('')}-${hex.slice(10, 16).join('')}`;
}

export function nowIso() {
  return new Date().toISOString();
}

export class DomainError extends Error {}
