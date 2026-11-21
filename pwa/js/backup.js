// Export/import compatible con la app Android de List Master.
// Los nombres de campo van en español y el formato es el mismo `version: 1`.

import {
  MAX_DESCRIPTION_LENGTH,
  MAX_ICON_LENGTH,
  MAX_ITEM_TITLE_LENGTH,
  MAX_LOCATION_LENGTH,
  MAX_SUBLIST_DEPTH,
  MAX_SUBPROJECT_DEPTH,
  MAX_TITLE_LENGTH,
  PRIORITIES,
  PROJECT_STATUSES,
  listDepth,
  projectDepth,
} from './domain.js';

export const BACKUP_VERSION = 1;
export const BACKUP_APPLICATION = 'list_master';

export class BackupError extends Error {}

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

export function encodeBackup(data, exportedAt = new Date().toISOString()) {
  const root = {
    version: BACKUP_VERSION,
    aplicacion: BACKUP_APPLICATION,
    exportado: exportedAt,
    proyectos: [...data.projects]
      .sort((a, b) => compareText(a.createdAt, b.createdAt) || compareText(a.id, b.id))
      .map((project) => ({
        id: project.id,
        titulo: project.title,
        estado: project.status,
        proyecto_padre: project.parentId ?? null,
        prioridad: project.priority ?? null,
        icono: project.icon ?? null,
        creado: project.createdAt,
        actualizado: project.updatedAt,
      })),
    tareas: [...data.tasks]
      .sort((a, b) => compareText(a.createdAt, b.createdAt) || compareText(a.id, b.id))
      .map((task) => {
        const item = {
          id: task.id,
          titulo: task.title,
          hecha: Boolean(task.done),
          proyecto_id: task.projectId,
          fecha: task.dueDate ?? null,
        };
        if (task.description) item.descripcion = task.description;
        if (task.priority) item.prioridad = task.priority;
        if (task.location) item.ubicacion = task.location;
        item.creado = task.createdAt;
        item.actualizado = task.updatedAt;
        return item;
      }),
    listas: [...data.lists]
      .sort((a, b) => compareText(a.createdAt, b.createdAt) || compareText(a.id, b.id))
      .map((list) => ({
        id: list.id,
        titulo: list.title,
        proyecto_id: list.projectId ?? null,
        lista_padre: list.parentId ?? null,
        orden: list.position ?? 0,
        creado: list.createdAt,
        actualizado: list.updatedAt,
        items: [...(list.items || [])]
          .sort((a, b) => a.position - b.position || compareText(a.id, b.id))
          .map((item) => ({
            id: item.id,
            titulo: item.title,
            hecha: Boolean(item.done),
            orden: item.position ?? 0,
          })),
      })),
  };
  return `${JSON.stringify(root, null, 2)}\n`;
}

export function decodeBackup(rawText) {
  const text = normalize(rawText);
  if (!text) throw new BackupError('El archivo está vacío.');

  let root;
  try {
    root = JSON.parse(text);
  } catch {
    throw new BackupError('El archivo no es un JSON válido.');
  }
  if (!root || typeof root !== 'object' || Array.isArray(root)) {
    throw new BackupError('El archivo no parece un export de List Master.');
  }
  if (root.aplicacion !== BACKUP_APPLICATION) {
    throw new BackupError('El archivo no parece un export de List Master.');
  }
  const version = optionalInt(root, 'version');
  if (version === undefined) throw new BackupError('Al archivo le falta la versión.');
  if (version > BACKUP_VERSION) {
    throw new BackupError('Este archivo es de una versión más nueva de List Master.');
  }
  if (version !== BACKUP_VERSION) {
    throw new BackupError('El archivo no parece un export de List Master.');
  }

  const ids = new Set();
  const projectObjects = requireArray(root, 'proyectos');
  const projects = projectObjects.map((element, index) => parseProject(element, index, ids));
  const projectIds = new Set(projects.map((project) => project.id));

  projects.forEach((project) => {
    if (project.parentId && !projectIds.has(project.parentId)) {
      throw new BackupError('Un subproyecto apunta a un proyecto que no existe.');
    }
    const depth = projectDepth(project.id, projects);
    if (depth < 0) throw new BackupError('Los proyectos tienen un ciclo o una relación inválida.');
    if (depth > MAX_SUBPROJECT_DEPTH) {
      throw new BackupError('Hay subproyectos anidados más de tres niveles.');
    }
  });

  const tasks = requireArray(root, 'tareas').map((element, index) =>
    parseTask(element, index, ids, projectIds),
  );

  const lists = requireArray(root, 'listas').map((element, index) =>
    parseList(element, index, ids, projectIds),
  );
  const listIds = new Set(lists.map((list) => list.id));
  lists.forEach((list) => {
    if (list.parentId && !listIds.has(list.parentId)) {
      throw new BackupError('Una sublista apunta a una lista que no existe.');
    }
    const depth = listDepth(list.id, lists);
    if (depth < 0) throw new BackupError('Las listas tienen un ciclo o una relación inválida.');
    if (depth > MAX_SUBLIST_DEPTH) {
      throw new BackupError('Hay listas anidadas más de tres niveles.');
    }
  });

  return { projects, tasks, lists };
}

function parseProject(element, index, ids) {
  const object = requireObject(element, `El proyecto ${index + 1}`);
  const id = requireId(object, ids, `El proyecto ${index + 1}`);
  const title = requireTitle(object, `El proyecto ${index + 1}`, MAX_TITLE_LENGTH);
  const status = optionalString(object, 'estado');
  if (!PROJECT_STATUSES.some((item) => item.value === status)) {
    throw new BackupError(`El proyecto ${quote(title)} tiene un estado inválido.`);
  }
  const priority = optionalString(object, 'prioridad');
  if (priority !== null && !PRIORITIES.some((item) => item.value === priority)) {
    throw new BackupError(`El proyecto ${quote(title)} tiene una prioridad inválida.`);
  }
  const icon = optionalString(object, 'icono');
  if (icon !== null && icon.trim().length > MAX_ICON_LENGTH) {
    throw new BackupError(`El proyecto ${quote(title)} tiene un ícono demasiado largo.`);
  }
  const createdAt = requireInstant(object, 'creado', `El proyecto ${quote(title)}`);
  const updatedAt = optionalInstant(object, 'actualizado', 'actualizado') ?? createdAt;
  const parentId = optionalString(object, 'proyecto_padre');
  if (parentId !== null) validateUuid(parentId, `El proyecto ${quote(title)}`);
  return {
    id,
    title,
    status,
    parentId,
    priority,
    icon: icon && icon.trim() ? icon.trim() : null,
    createdAt,
    updatedAt,
  };
}

function parseTask(element, index, ids, projectIds) {
  const object = requireObject(element, `La tarea ${index + 1}`);
  const id = requireId(object, ids, `La tarea ${index + 1}`);
  const title = requireTitle(object, `La tarea ${index + 1}`, MAX_TITLE_LENGTH);
  const done = requireBoolean(object, 'hecha', `La tarea ${quote(title)}`);
  const projectId = optionalString(object, 'proyecto_id');
  if (!projectId) throw new BackupError(`La tarea ${quote(title)} no tiene proyecto.`);
  validateUuid(projectId, `La tarea ${quote(title)}`);
  if (!projectIds.has(projectId)) {
    throw new BackupError(`La tarea ${quote(title)} apunta a un proyecto que no existe.`);
  }
  const dueDate = optionalString(object, 'fecha');
  if (dueDate !== null && (!DATE_PATTERN.test(dueDate) || !isValidDate(dueDate))) {
    throw new BackupError(`La tarea ${quote(title)} tiene una fecha inválida.`);
  }
  const description = (optionalString(object, 'descripcion') ?? '').trim();
  if (description.length > MAX_DESCRIPTION_LENGTH) {
    throw new BackupError(`La tarea ${quote(title)} tiene una descripción demasiado larga.`);
  }
  const priority = optionalString(object, 'prioridad');
  if (priority !== null && !PRIORITIES.some((item) => item.value === priority)) {
    throw new BackupError(`La tarea ${quote(title)} tiene una prioridad inválida.`);
  }
  const location = (optionalString(object, 'ubicacion') ?? '').trim();
  if (location.length > MAX_LOCATION_LENGTH) {
    throw new BackupError(`La tarea ${quote(title)} tiene una ubicación demasiado larga.`);
  }
  const createdAt = requireInstant(object, 'creado', `La tarea ${quote(title)}`);
  const updatedAt = optionalInstant(object, 'actualizado', 'actualizado') ?? createdAt;
  return {
    id,
    title,
    done,
    projectId,
    dueDate,
    description,
    priority,
    location,
    createdAt,
    updatedAt,
  };
}

function parseList(element, index, ids, projectIds) {
  const object = requireObject(element, `La lista ${index + 1}`);
  const id = requireId(object, ids, `La lista ${index + 1}`);
  const title = requireTitle(object, `La lista ${index + 1}`, MAX_TITLE_LENGTH);
  const projectId = optionalString(object, 'proyecto_id');
  if (projectId !== null) {
    validateUuid(projectId, `La lista ${quote(title)}`);
    if (!projectIds.has(projectId)) {
      throw new BackupError(`La lista ${quote(title)} apunta a un proyecto que no existe.`);
    }
  }
  const parentId = optionalString(object, 'lista_padre');
  if (parentId !== null) validateUuid(parentId, `La lista ${quote(title)}`);
  const createdAt = requireInstant(object, 'creado', `La lista ${quote(title)}`);
  const updatedAt = optionalInstant(object, 'actualizado', 'actualizado') ?? createdAt;
  const position = optionalInt(object, 'orden') ?? index;
  const items = requireArray(object, 'items').map((itemElement, itemIndex) => {
    const itemObject = requireObject(itemElement, `El ítem ${itemIndex + 1} de ${quote(title)}`);
    const itemId = requireId(itemObject, ids, `El ítem ${itemIndex + 1} de ${quote(title)}`);
    const itemTitle = requireTitle(
      itemObject,
      `El ítem ${itemIndex + 1} de ${quote(title)}`,
      MAX_ITEM_TITLE_LENGTH,
    );
    return {
      id: itemId,
      title: itemTitle,
      done: requireBoolean(itemObject, 'hecha', `El ítem ${quote(itemTitle)}`),
      position: optionalInt(itemObject, 'orden') ?? itemIndex,
    };
  });
  return {
    id,
    title,
    projectId,
    parentId,
    position,
    createdAt,
    updatedAt,
    items,
  };
}

// Helpers de validación

function normalize(rawText) {
  const candidate = String(rawText ?? '').replace(/^\uFEFF/, '').trim();
  if (candidate.startsWith('{')) return candidate;
  const start = candidate.indexOf('{');
  const end = candidate.lastIndexOf('}');
  return start >= 0 && end > start ? candidate.slice(start, end + 1) : candidate;
}

function quote(value) {
  return `«${value}»`;
}

function requireObject(value, label) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new BackupError(`${label} está dañado.`);
  }
  return value;
}

function requireArray(object, name) {
  const value = object[name];
  if (!Array.isArray(value)) throw new BackupError(`Al archivo le falta la lista «${name}».`);
  return value;
}

function optionalString(object, name) {
  const value = object[name];
  if (value === undefined || value === null) return null;
  if (typeof value !== 'string') throw new BackupError(`El campo «${name}» tiene un tipo inválido.`);
  return value;
}

function optionalInt(object, name) {
  const value = object[name];
  if (value === undefined || value === null) return undefined;
  if (typeof value !== 'number' || !Number.isInteger(value)) {
    throw new BackupError(`El campo «${name}» tiene un tipo inválido.`);
  }
  return value;
}

function requireBoolean(object, name, label) {
  const value = object[name];
  if (typeof value !== 'boolean') {
    throw new BackupError(`Al ${label} le falta el campo «${name}».`);
  }
  return value;
}

function requireTitle(object, label, maxLength) {
  const raw = optionalString(object, 'titulo');
  if (raw === null) throw new BackupError(`Al ${label} le falta el título.`);
  const clean = raw.trim();
  if (!clean) throw new BackupError(`${label} tiene el título vacío.`);
  if (clean.length > maxLength) {
    throw new BackupError(`${label} supera los ${maxLength} caracteres.`);
  }
  return clean;
}

function requireId(object, ids, label) {
  const id = optionalString(object, 'id');
  if (!id) throw new BackupError(`Al ${label} le falta el id.`);
  validateUuid(id, label);
  if (ids.has(id)) throw new BackupError(`El id «${id}» está repetido en el archivo.`);
  ids.add(id);
  return id;
}

function requireInstant(object, name, label) {
  const value = optionalInstant(object, name, name);
  if (value === null) throw new BackupError(`Al ${label} le falta la fecha «${name}».`);
  return value;
}

function optionalInstant(object, name, label) {
  const raw = optionalString(object, name);
  if (raw === null) return null;
  const date = new Date(raw);
  if (Number.isNaN(date.getTime())) {
    throw new BackupError(`El campo «${label}» no es una fecha ISO-8601 válida.`);
  }
  return raw;
}

function validateUuid(value, label) {
  if (!UUID_PATTERN.test(value)) {
    throw new BackupError(`${label} tiene un id que no es un UUID válido.`);
  }
}

function isValidDate(value) {
  const [year, month, day] = value.split('-').map(Number);
  const date = new Date(Date.UTC(year, month - 1, day));
  return (
    date.getUTCFullYear() === year &&
    date.getUTCMonth() === month - 1 &&
    date.getUTCDate() === day
  );
}

function compareText(a = '', b = '') {
  return a < b ? -1 : a > b ? 1 : 0;
}
