import assert from 'node:assert/strict';
import test from 'node:test';

import { BackupError, decodeBackup, encodeBackup } from '../js/backup.js';

const AT = '2026-01-01T10:00:00.000Z';
const ids = {
  root: crypto.randomUUID(),
  child: crypto.randomUUID(),
  other: crypto.randomUUID(),
  taskRoot: crypto.randomUUID(),
  taskChild: crypto.randomUUID(),
  list: crypto.randomUUID(),
  itemOne: crypto.randomUUID(),
  itemTwo: crypto.randomUUID(),
};

function fixture() {
  return {
    projects: [
      {
        id: ids.root,
        title: 'Casa',
        status: 'Activo',
        parentId: null,
        priority: 'Alta',
        icon: 'home',
        createdAt: AT,
        updatedAt: AT,
      },
      {
        id: ids.child,
        title: 'Cocina',
        status: 'Pausa',
        parentId: ids.root,
        priority: null,
        icon: null,
        createdAt: AT,
        updatedAt: AT,
      },
      {
        id: ids.other,
        title: 'Trabajo',
        status: 'Terminado',
        parentId: null,
        priority: 'Media',
        icon: '💼',
        createdAt: AT,
        updatedAt: AT,
      },
    ],
    tasks: [
      {
        id: ids.taskRoot,
        title: 'Comprar pintura',
        done: false,
        projectId: ids.root,
        dueDate: '2026-02-01',
        description: 'Color blanco',
        location: 'Ferretería de la esquina',
        priority: 'Alta',
        createdAt: AT,
        updatedAt: AT,
      },
      {
        id: ids.taskChild,
        title: 'Limpiar',
        done: true,
        projectId: ids.child,
        dueDate: null,
        description: '',
        location: '',
        priority: null,
        createdAt: AT,
        updatedAt: AT,
      },
    ],
    lists: [
      {
        id: ids.list,
        title: 'Compras',
        projectId: ids.root,
        parentId: null,
        position: 2,
        createdAt: AT,
        updatedAt: AT,
        items: [
          { id: ids.itemOne, title: 'Leche', done: false, position: 0 },
          { id: ids.itemTwo, title: 'Pan', done: true, position: 1 },
        ],
      },
    ],
  };
}

test('el export usa el formato de la app Android', () => {
  const parsed = JSON.parse(encodeBackup(fixture()));
  assert.equal(parsed.version, 1);
  assert.equal(parsed.aplicacion, 'list_master');
  assert.ok(!Number.isNaN(Date.parse(parsed.exportado)));
  const encodedTask = parsed.tareas.find((task) => task.id === ids.taskRoot);
  const encodedDone = parsed.tareas.find((task) => task.id === ids.taskChild);
  assert.deepEqual(Object.keys(parsed.proyectos[0]), [
    'id',
    'titulo',
    'estado',
    'proyecto_padre',
    'prioridad',
    'icono',
    'creado',
    'actualizado',
  ]);
  assert.deepEqual(Object.keys(encodedTask), [
    'id',
    'titulo',
    'hecha',
    'proyecto_id',
    'fecha',
    'descripcion',
    'prioridad',
    'ubicacion',
    'creado',
    'actualizado',
  ]);
  assert.equal(parsed.listas[0].orden, 2);
  assert.equal(parsed.listas[0].items[0].titulo, 'Leche');
  assert.equal(encodedDone.hecha, true);
});

test('encode/decode conserva proyectos, tareas y listas', () => {
  const original = fixture();
  const decoded = decodeBackup(encodeBackup(original));
  assert.equal(decoded.projects.length, 3);
  assert.equal(decoded.tasks.length, 2);
  assert.equal(decoded.lists.length, 1);

  const root = decoded.projects.find((project) => project.id === ids.root);
  assert.equal(root.title, 'Casa');
  assert.equal(root.priority, 'Alta');
  assert.equal(root.icon, 'home');

  const child = decoded.projects.find((project) => project.id === ids.child);
  assert.equal(child.parentId, ids.root);
  assert.equal(child.status, 'Pausa');

  const doneTask = decoded.tasks.find((task) => task.id === ids.taskChild);
  assert.equal(doneTask.done, true);
  assert.equal(doneTask.dueDate, null);
  assert.equal(doneTask.location, '');

  const rootTask = decoded.tasks.find((task) => task.id === ids.taskRoot);
  assert.equal(rootTask.location, 'Ferretería de la esquina');

  const list = decoded.lists[0];
  assert.equal(list.projectId, ids.root);
  assert.deepEqual(
    list.items.map((item) => [item.title, item.done, item.position]),
    [
      ['Leche', false, 0],
      ['Pan', true, 1],
    ],
  );
});

test('decode acepta un archivo con BOM y texto alrededor', () => {
  const text = `\uFEFF  ${encodeBackup(fixture())}  `;
  const decoded = decodeBackup(text);
  assert.equal(decoded.projects.length, 3);
});

test('decode rechaza archivos que no son de List Master', () => {
  assert.throws(() => decodeBackup(''), BackupError);
  assert.throws(() => decodeBackup('no es json'), BackupError);
  assert.throws(() => decodeBackup('[]'), BackupError);
  assert.throws(
    () => decodeBackup(JSON.stringify({ version: 1, aplicacion: 'otra', proyectos: [], tareas: [], listas: [] })),
    /no parece un export/,
  );
});

test('decode rechaza versiones más nuevas', () => {
  const root = JSON.parse(encodeBackup(fixture()));
  root.version = 2;
  assert.throws(() => decodeBackup(JSON.stringify(root)), /versión más nueva/);
});

test('decode valida referencias, ids repetidos y fechas', () => {
  const unknownProject = JSON.parse(encodeBackup(fixture()));
  unknownProject.tareas[0].proyecto_id = crypto.randomUUID();
  assert.throws(() => decodeBackup(JSON.stringify(unknownProject)), /proyecto que no existe/);

  const duplicate = JSON.parse(encodeBackup(fixture()));
  duplicate.tareas[0].id = ids.root;
  assert.throws(() => decodeBackup(JSON.stringify(duplicate)), /repetido/);

  const badDate = JSON.parse(encodeBackup(fixture()));
  badDate.tareas[0].fecha = '2026-02-30';
  assert.throws(() => decodeBackup(JSON.stringify(badDate)), /fecha inválida/);

  const badUuid = JSON.parse(encodeBackup(fixture()));
  badUuid.proyectos[0].id = 'no-es-uuid';
  assert.throws(() => decodeBackup(JSON.stringify(badUuid)), /UUID válido/);

  const badParent = JSON.parse(encodeBackup(fixture()));
  const childProject = badParent.proyectos.find((entry) => entry.id === ids.child);
  childProject.proyecto_padre = crypto.randomUUID();
  assert.throws(() => decodeBackup(JSON.stringify(badParent)), /proyecto que no existe/);

  const longLocation = JSON.parse(encodeBackup(fixture()));
  longLocation.tareas[0].ubicacion = 'x'.repeat(201);
  assert.throws(() => decodeBackup(JSON.stringify(longLocation)), /ubicación demasiado larga/);
});

test('decode rechaza listas con ciclos o demasiada profundidad', () => {
  const base = JSON.parse(encodeBackup(fixture()));
  base.listas[0].lista_padre = ids.list;
  assert.throws(() => decodeBackup(JSON.stringify(base)), /ciclo|relación inválida/);

  const deep = JSON.parse(encodeBackup(fixture()));
  const chain = Array.from({ length: 5 }, () => crypto.randomUUID());
  deep.listas = chain.map((id, index) => ({
    id,
    titulo: `Lista ${index + 1}`,
    proyecto_id: null,
    lista_padre: index === 0 ? null : chain[index - 1],
    orden: index,
    creado: AT,
    actualizado: AT,
    items: [],
  }));
  assert.throws(() => decodeBackup(JSON.stringify(deep)), /tres niveles/);
});

test('decode aplica valores por defecto honestos', () => {
  const raw = JSON.parse(encodeBackup(fixture()));
  const rawTask = raw.tareas.find((entry) => entry.id === ids.taskRoot);
  delete rawTask.descripcion;
  delete rawTask.prioridad;
  delete rawTask.ubicacion;
  delete rawTask.fecha;
  delete raw.listas[0].orden;
  delete raw.listas[0].items[0].orden;
  const decoded = decodeBackup(JSON.stringify(raw));
  const task = decoded.tasks.find((entry) => entry.id === ids.taskRoot);
  assert.equal(task.description, '');
  assert.equal(task.priority, null);
  assert.equal(task.location, '');
  assert.equal(task.dueDate, null);
  assert.equal(decoded.lists[0].position, 0);
  assert.equal(decoded.lists[0].items[0].position, 0);
});
