import assert from 'node:assert/strict';
import test from 'node:test';

import {
  DomainError,
  MAX_SUBLIST_DEPTH,
  MAX_SUBPROJECT_DEPTH,
  buildListRows,
  canHaveSublist,
  canHaveSubproject,
  compareTasks,
  listDepth,
  listProgress,
  listSubtreeIds,
  mergeReorder,
  newId,
  normalizeDescription,
  normalizeTitle,
  pendingCount,
  pendingGroups,
  progressFor,
  progressPercentage,
  projectDepth,
  sortTasks,
  sortedItems,
  validListParents,
  validProjectParents,
} from '../js/domain.js';

const AT = '2026-01-01T10:00:00.000Z';

function project(id, overrides = {}) {
  return {
    id,
    title: id,
    status: 'Activo',
    parentId: null,
    priority: null,
    icon: null,
    createdAt: AT,
    updatedAt: AT,
    ...overrides,
  };
}

function task(id, projectId, overrides = {}) {
  return {
    id,
    title: id,
    done: false,
    projectId,
    dueDate: null,
    description: '',
    priority: null,
    createdAt: AT,
    updatedAt: AT,
    ...overrides,
  };
}

function list(id, overrides = {}) {
  return {
    id,
    title: id,
    projectId: null,
    parentId: null,
    position: 0,
    createdAt: AT,
    updatedAt: AT,
    items: [],
    ...overrides,
  };
}

function item(id, position, overrides = {}) {
  return { id, title: id, done: false, position, ...overrides };
}

test('el progreso incluye subproyectos abiertos y excluye cancelados', () => {
  const projects = [
    project('a'),
    project('b', { parentId: 'a' }),
    project('c', { parentId: 'a', status: 'Cancelado' }),
    project('d', { parentId: 'c' }),
  ];
  const tasks = [
    task('t1', 'a'),
    task('t2', 'a', { done: true }),
    task('t3', 'b'),
    task('t4', 'c'),
    task('t5', 'd'),
  ];
  assert.deepEqual(progressFor('a', projects, tasks), { done: 1, total: 3 });
  assert.deepEqual(progressFor('c', projects, tasks), { done: 0, total: 0 });
});

test('el porcentaje se topa en 99 hasta terminar', () => {
  assert.equal(progressPercentage({ done: 0, total: 0 }), 0);
  assert.equal(progressPercentage({ done: 3, total: 4 }), 75);
  assert.equal(progressPercentage({ done: 99, total: 100 }), 99);
  assert.equal(progressPercentage({ done: 4, total: 4 }), 100);
});

test('las tareas se ordenan por prioridad, antigüedad e id', () => {
  const tasks = [
    task('z', 'p', { priority: null, createdAt: '2026-01-01T00:00:00.000Z' }),
    task('m', 'p', { priority: 'Media', createdAt: '2026-01-03T00:00:00.000Z' }),
    task('a', 'p', { priority: 'Alta', createdAt: '2026-01-02T00:00:00.000Z' }),
    task('b', 'p', { priority: 'Alta', createdAt: '2026-01-01T00:00:00.000Z' }),
  ];
  assert.deepEqual(
    sortTasks(tasks).map((entry) => entry.id),
    ['b', 'a', 'm', 'z'],
  );
  assert.ok(compareTasks(tasks[2], tasks[1]) < 0);
});

test('Pendientes agrupa por proyecto y descarta proyectos cerrados', () => {
  const projects = [
    project('a'),
    project('b', { parentId: 'a' }),
    project('c', { status: 'Terminado' }),
    project('d', { parentId: 'a', status: 'Cancelado' }),
  ];
  const tasks = [
    task('t1', 'a'),
    task('t2', 'a', { done: true }),
    task('t3', 'b'),
    task('t4', 'c'),
    task('t5', 'd'),
  ];
  const groups = pendingGroups(projects, tasks);
  assert.deepEqual(
    groups.map((group) => [group.project.id, group.depth, group.tasks.map((entry) => entry.id)]),
    [
      ['a', 0, ['t1']],
      ['b', 1, ['t3']],
    ],
  );
  assert.equal(pendingCount(groups), 2);
});

test('la profundidad de proyectos respeta el límite de tres niveles', () => {
  const projects = [
    project('root'),
    project('child', { parentId: 'root' }),
    project('grandchild', { parentId: 'child' }),
    project('other'),
  ];
  assert.equal(projectDepth('root', projects), 0);
  assert.equal(projectDepth('child', projects), 1);
  assert.equal(projectDepth('grandchild', projects), 2);
  assert.equal(projectDepth('missing', projects), -1);
  assert.equal(canHaveSubproject('grandchild', projects), true);
  assert.equal(canHaveSubproject('child', projects), true);
  assert.equal(projectDepth('grandchild', projects), MAX_SUBPROJECT_DEPTH - 1);

  const deeper = [...projects, project('great', { parentId: 'grandchild' })];
  assert.equal(canHaveSubproject('grandchild', deeper), true);
  assert.equal(canHaveSubproject('great', deeper), false);
  assert.deepEqual(validProjectParents('root', projects).map((entry) => entry.id), ['other']);
  assert.deepEqual(validProjectParents('other', projects).map((entry) => entry.id), [
    'root',
    'child',
    'grandchild',
  ]);
});

test('no se puede mover un proyecto dentro de su propio subárbol', () => {
  const projects = [
    project('a'),
    project('b', { parentId: 'a' }),
    project('c', { parentId: 'b' }),
    project('d'),
  ];
  const valid = validProjectParents('a', projects);
  assert.deepEqual(valid.map((entry) => entry.id), ['d']);
  assert.ok(!valid.some((entry) => ['b', 'c'].includes(entry.id)));
});

test('las listas calculan profundidad, subárbol y progreso', () => {
  const lists = [
    list('root', { items: [item('i1', 0), item('i2', 1, { done: true })] }),
    list('child', { parentId: 'root', items: [item('i3', 0)] }),
    list('grandchild', { parentId: 'child', items: [item('i4', 0, { done: true })] }),
    list('other'),
  ];
  assert.equal(listDepth('grandchild', lists), 2);
  assert.equal(listDepth('other', lists), 0);
  assert.equal(canHaveSublist('grandchild', lists), true);
  assert.equal(canHaveSublist('other', lists), true);
  assert.deepEqual([...listSubtreeIds('root', lists)].sort(), ['child', 'grandchild', 'root']);
  assert.deepEqual(listProgress(lists[0], lists), { done: 2, total: 4 });
  assert.deepEqual(validListParents('root', lists).map((entry) => entry.id), ['other']);
  assert.equal(MAX_SUBLIST_DEPTH, 3);
});

test('las filas de una lista intercalan ítems y sublistas por posición', () => {
  const lists = [
    list('root', {
      items: [item('a', 0), item('b', 2), item('done', 1, { done: true })],
    }),
    list('sub', { parentId: 'root', position: 1 }),
  ];
  const rows = buildListRows(lists[0], lists);
  assert.deepEqual(
    rows.map((row) => (row.kind === 'item' ? row.item.id : row.list.id)),
    ['a', 'done', 'sub', 'b'],
  );
  const collapsed = buildListRows(lists[0], lists, new Set(['sub']));
  assert.ok(!collapsed.some((row) => row.kind === 'list'));
  assert.deepEqual(
    sortedItems(lists[0]).map((entry) => entry.id),
    ['a', 'b', 'done'],
  );
});

test('al reordenar los hechos conservan su lugar y los pendientes se permutan', () => {
  const all = ['a', 'hecha', 'b', 'c'];
  const draggable = ['a', 'b', 'c'];
  assert.deepEqual(mergeReorder(all, draggable, ['b', 'a', 'c']), ['b', 'hecha', 'a', 'c']);
  assert.deepEqual(mergeReorder(all, draggable, ['b', 'c', 'a']), ['b', 'hecha', 'c', 'a']);
});

test('los títulos y la descripción se validan', () => {
  assert.equal(normalizeTitle('  Hola  '), 'Hola');
  assert.throws(() => normalizeTitle('   '), DomainError);
  assert.throws(() => normalizeTitle('x'.repeat(121)), DomainError);
  assert.equal(normalizeDescription('  detalle  '), 'detalle');
  assert.throws(() => normalizeDescription('x'.repeat(2001)), DomainError);
});

test('newId genera UUID válidos', () => {
  const pattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;
  for (let index = 0; index < 10; index += 1) {
    assert.match(newId(), pattern);
  }
});
