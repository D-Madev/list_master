# 0003: Modelo de datos y decisiones abiertas

- **Estado:** aprobada e implementada. Las seis decisiones bloqueantes se adoptaron y están en el código y en los tests.
- **Fecha:** 29 de octubre de 2026.
- **Relación:** implementa el modelo de `0000-alcance.md` y el export que pide el prompt. No construye A.R.G.O.S.

## Lo que ya está decidido

- Proyectos con subproyectos anidados hasta **3 niveles** debajo de la raíz (4 nodos en una cadena).
- Tareas dentro de un proyecto, con fecha opcional.
- Listas simples, con o sin proyecto vinculado.
- Progreso **calculado**, nunca guardado a mano.
- IDs **UUID** estables.
- Datos locales, exportables a JSON, sin cuenta ni servidor.
- Nombres de campos del export **en español**; identificadores de código en inglés.

## Modelo conceptual

### Proyecto

| Campo | Tipo | Obligatorio | Notas |
|---|---|---|---|
| `id` | UUID | Sí | Estable; no se reutiliza |
| `titulo` | texto | Sí | 1–120 caracteres, sin espacios al borde |
| `estado` | enum | Sí | `Activo`, `Pausa`, `Terminado`, `Cancelado`, `Postergado` |
| `proyecto_padre` | UUID o `null` | No | `null` = proyecto raíz |
| `prioridad` | enum o `null` | No | **Propuesta:** `Alta`, `Media`, `Baja`; ver decisión 1 |
| `creado` | ISO-8601 UTC | Sí | Para orden estable y export |
| `actualizado` | ISO-8601 UTC | Sí | Último cambio de la fila del proyecto |

### Tarea

| Campo | Tipo | Obligatorio | Notas |
|---|---|---|---|
| `id` | UUID | Sí | Estable |
| `titulo` | texto | Sí | 1–120 caracteres |
| `hecha` | booleano | Sí | |
| `proyecto_id` | UUID | Sí | Debe existir |
| `fecha` | `YYYY-MM-DD` o `null` | No | Vencimiento, sin hora |
| `creado` | ISO-8601 UTC | Sí | Orden estable |
| `actualizado` | ISO-8601 UTC | Sí | Último cambio |

### Lista

| Campo | Tipo | Obligatorio | Notas |
|---|---|---|---|
| `id` | UUID | Sí | Estable |
| `titulo` | texto | Sí | 1–120 caracteres |
| `proyecto_id` | UUID o `null` | No | `null` = lista suelta |
| `creado` | ISO-8601 UTC | Sí | |
| `actualizado` | ISO-8601 UTC | Sí | También cambia al marcar un ítem |
| `items` | array | Sí | Puede ser vacío |

### Ítem de lista

| Campo | Tipo | Obligatorio | Notas |
|---|---|---|---|
| `id` | UUID | Sí | Estable |
| `titulo` | texto | Sí | 1–200 caracteres |
| `hecha` | booleano | Sí | |
| `orden` | entero | Sí | 0-based; conserva el orden al exportar/importar |

## Reglas de progreso

El progreso se calcula en el dominio, desde las tareas. **Nunca se escribe en la base.**

### Algoritmo

1. Partir del proyecto raíz.
2. Recorrer sus subproyectos hasta 3 niveles, sin ciclos.
3. Reunir todas las tareas cuyo `proyecto_id` esté en ese árbol.
4. `total = tareas.size`, `hechas = tareas.count { it.hecha }`.
5. Si `total == 0`: **sin progreso** (la UI dice “Sin tareas todavía”, no 0%).
6. Si `hechas == total`: **100%**.
7. Si no: `porcentaje = round(hechas * 100.0 / total)`, con tope en **99%**. Nunca 100% si queda una pendiente.

### Casos que importan

| Caso | Resultado |
|---|---|
| Proyecto sin tareas ni subproyectos | Sin progreso; “Sin tareas todavía” |
| 3 de 4 tareas | “3 de 4”, 75% |
| Tareas en un subproyecto | Suman al padre |
| Tarea en el tercer nivel de subproyectos | Suma al proyecto raíz |
| Subproyecto `Terminado` con tareas pendientes | Sus pendientes siguen contando; el detalle avisa |
| Subproyecto `Cancelado` | Se excluye todo su subárbol del progreso del padre |
| Lista vinculada | **No** suma al progreso del proyecto |
| Proyecto con 0 tareas pero un subproyecto terminado sin tareas | Sin progreso; no se inventa 1 de 1 |
| 199 de 200 | 99%, no 100% |
| 0 de 4 | “0 de 4”, 0% (dato real) |

La exclusión de subárboles cancelados está implementada en `ProgressCalculator` y cubierta por tests. Ver decisión 3.

## Profundidad y ciclos

- **Nivel 0:** proyecto raíz (`proyecto_padre = null`).
- **Nivel 1:** subproyecto de un raíz.
- **Nivel 2:** subproyecto de un nivel 1.
- **Nivel 3:** subproyecto de un nivel 2; es el más profundo permitido.
- **Nivel 4:** prohibido. La UI oculta “Agregar Subproyecto” en el nivel 3 y el dominio rechaza la operación.
- Un proyecto no puede ser padre de sí mismo ni de un descendiente (ciclo).
- En el MVP, el `proyecto_padre` se elige **al crear** y no se puede mover después. Mover un proyecto cambiaría la profundidad y podría crear ciclos; si hace falta, es una decisión nueva.
- El detalle muestra un breadcrumb (“Mis Proyectos / Diseño Web / Identidad”) para que el nivel 3 de subproyectos no se pierda.

**Interpretación confirmada:** el prompt dice “subproyectos hasta 3 niveles de profundidad”. Se interpreta como **3 niveles de subproyectos debajo de la raíz** (raíz + 3 = 4 nodos en una cadena), no como 3 nodos totales. Está validado en el dominio, en el repositorio y en el import.

## Estados y transiciones

- Se permite cambiar entre los cinco estados, sin máquina de estados rígida.
- **“Cerrar” un proyecto = estado `Terminado`.** No hay un flag de “archivado” separado.
- Marcar `Terminado` con tareas pendientes está permitido, pero la app avisa: “Te quedan 2 tareas sin hacer.” **No** se marcan solas.
- Llegar a 100% **no** cambia el estado a `Terminado`. La app sugiere: “Todo hecho. ¿Lo marcás como Terminado?”
- `Cancelado` es terminal en la práctica, pero se puede reabrir. Su subárbol no suma al padre.
- `Pausa` y `Postergado` siguen sumando al progreso: el trabajo está pendiente, no abandonado.

## Borrado y vínculos

- **Borrar proyecto:** borra en cascada sus subproyectos y todas sus tareas. Las listas vinculadas **no** se borran: quedan sueltas (`proyecto_id = null`). Se confirma con el texto exacto de `DESIGN.md`.
- **Borrar tarea:** hard delete. *Propuesta:* confirmación simple para todas, para no agregar un sistema de undo en el MVP.
- **Borrar lista:** borra sus ítems. Confirmación.
- **Borrar ítem:** hard delete; sin confirmación (es un ítem, no una estructura).
- No hay papelera ni “soft delete” en el MVP. El respaldo JSON es la red de seguridad.

## Export

### Forma

```json
{
  "version": 1,
  "aplicacion": "list_master",
  "exportado": "2026-10-29T17:32:00Z",
  "proyectos": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "titulo": "Diseño Web",
      "estado": "Activo",
      "proyecto_padre": null,
      "prioridad": "Alta",
      "creado": "2026-10-20T12:15:00Z",
      "actualizado": "2026-10-29T17:30:00Z"
    }
  ],
  "tareas": [
    {
      "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      "titulo": "Definir la paleta",
      "hecha": true,
      "proyecto_id": "550e8400-e29b-41d4-a716-446655440000",
      "fecha": "2026-11-05",
      "creado": "2026-10-21T09:00:00Z",
      "actualizado": "2026-10-29T17:30:00Z"
    }
  ],
  "listas": [
    {
      "id": "9b2f4c1e-3a6d-4b8e-9f21-5c7d0a1b2e3f",
      "titulo": "Compras",
      "proyecto_id": null,
      "creado": "2026-10-22T10:00:00Z",
      "actualizado": "2026-10-28T18:00:00Z",
      "items": [
        {
          "id": "c1a2b3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d",
          "titulo": "Leche",
          "hecha": false,
          "orden": 0
        }
      ]
    }
  ]
}
```

### Reglas

- UTF-8 sin BOM, 2 espacios de indentación, salto de línea final.
- Todos los campos presentes; los opcionales van en `null` (no se omiten).
- `version` entero; hoy `1`.
- `aplicacion` = `"list_master"`.
- `exportado` en ISO-8601 UTC (`Z`).
- Orden determinista: proyectos, tareas y listas por `creado` y luego `id`; ítems por `orden` y luego `id`. Así el mismo estado produce el mismo archivo y se puede comparar.
- Nombre de archivo: `list-master-YYYY-MM-DD.json` (fecha local).
- Se guarda con Storage Access Framework (`ACTION_CREATE_DOCUMENT`), sin permiso de almacenamiento.
- No se exportan ajustes de tema ni rutas internas: son datos de la app, no del usuario.

## Import

### Reglas

- Leer UTF-8; si hay BOM, se ignora.
- Validar **todo** antes de escribir:
  - JSON válido.
  - `version` = 1. Si es mayor: “Este archivo es de una versión más nueva de List Master.”
  - `aplicacion` = `"list_master"`.
  - Campos requeridos presentes y del tipo correcto.
  - Enums válidos (`estado`, `prioridad`).
  - UUID válidos y únicos en todo el archivo.
  - `proyecto_padre` existe o es `null`.
  - `proyecto_id` de tareas existe.
  - `proyecto_id` de listas existe o es `null`.
  - Sin ciclos.
  - Máximo 3 niveles de subproyectos (profundidad total 4).
  - Títulos no vacíos y dentro del límite.
  - Fechas `YYYY-MM-DD` válidas.
- Campos desconocidos: se ignoran (compatibilidad futura).
- `actualizado` faltante: se usa `creado` (no se inventa una fecha nueva).
- `orden` faltante en un ítem: se usa el índice del array.
- **Reemplazo total, atómico:** se borra todo y se inserta lo importado en una sola transacción. Si algo falla, no se toca nada.
- Confirmación previa: “Importar reemplaza todos tus datos actuales. ¿Querés exportar primero?”
- Un archivo con arrays vacíos es válido: importarlo deja la app vacía (se avisa).
- No hay merge ni resolución de conflictos en el MVP.

## Persistencia

- Room, base `list_master.db`.
- Tablas: `projects`, `tasks`, `lists`, `list_items` (nombres de código en inglés).
- Enums como `TEXT` con los valores españoles, no ordinales: si se reordena un enum, no se corrompen datos.
- Claves foráneas:
  - `projects.parent_id → projects.id` `ON DELETE CASCADE`.
  - `tasks.project_id → projects.id` `ON DELETE CASCADE`.
  - `lists.project_id → projects.id` `ON DELETE SET NULL`.
  - `list_items.list_id → lists.id` `ON DELETE CASCADE`.
- Índices en `parent_id`, `project_id`, `list_id` y `position`.
- Fechas de creación/actualización como `TEXT` ISO-8601 UTC; `fecha` como `TEXT` `YYYY-MM-DD`.
- Los IDs se generan con UUID v4; al importar se acepta cualquier UUID válido (v1–v5), para no cerrar la puerta a A.R.G.O.S.
- Esquema de Room exportado a `app/schemas/` y commiteado.
- El progreso no tiene columna: se calcula en un caso de uso puro.
- Los repositorios exponen `Flow`; la UI observa. Para el volumen de una app personal, se carga todo el árbol y se calcula en memoria. No hay paginación.
- No hay sincronización, ni conflictos, ni relojes distribuidos. El único escritor es la app.

## Decisiones adoptadas

### Bloqueantes (aprobadas)

| # | Decisión | Propuesta | Por qué | Si no se aprueba |
|---|---|---|---|---|
| 1 | **Prioridad** | Agregar `prioridad` opcional al proyecto: `Alta`, `Media`, `Baja`; sin prioridad = sin chip | La tarjeta de referencia muestra Prioridad y el modelo no la tenía | Se elimina el chip de prioridad; la tarjeta queda solo con Estado |
| 2 | **“Cerrar”** | Sinónimo de estado `Terminado`; no hay archivo aparte | Evita dos conceptos para lo mismo | Se agrega un flag `archivado` y una acción extra |
| 3 | **Progreso y cancelados** | Excluir del progreso del padre todo subárbol `Cancelado` | Un proyecto cancelado no debería dejar tareas pendientes para siempre | Cuenta como pendiente; el padre nunca llega a 100% |
| 4 | **Borrar proyecto con listas** | Las listas sobreviven y quedan sueltas | Una lista de compras no pertenece al proyecto | Se borran en cascada (más destructivo) |
| 5 | **Profundidad** | Raíz = 0; máximo 3 niveles de subproyectos (4 nodos); padre inmutable en el MVP | Simple, predecible, sin ciclos | Se permite mover proyectos y hay que validar ciclos al editar |
| 6 | **Import** | Reemplazo total, validado y atómico, con confirmación | Es el flujo más simple y seguro para un archivo completo | Merge con conflictos: mucho más complejo |

### Menores (adoptadas)

| # | Decisión | Propuesta |
|---|---|---|
| 7 | **Campos extra** | `actualizado` en proyecto/tarea/lista; `creado` en tarea/lista; `orden` en ítem. Justifican orden estable, export determinista y futura sincronización |
| 8 | **Paquete** | `ar.matias.listmaster` |
| 9 | **Icono de proyecto** | Icono fijo de carpeta/capas según profundidad; sin selector de iconos en el MVP |
| 10 | **Árbol en la tarjeta** | Un nivel: primero las tareas del proyecto, después filas de subproyectos con su mini progreso. El detalle muestra el árbol completo |
| 11 | **Prioridad en tareas** | No existe en el MVP; la prioridad es del proyecto |
| 12 | **Listas en subproyectos** | Se pueden vincular a cualquier nodo del árbol, no solo a raíces |
| 13 | **Fechas** | Solo vencimiento (`YYYY-MM-DD`), sin hora, sin recordatorios |
| 14 | **Tema** | Se guarda con SharedPreferences; claro/oscuro/sistema; sin color dinámico. Se usó SharedPreferences porque `androidx.datastore-preferences` no estaba en la caché local y la regla era no descargar dependencias nuevas |
| 15 | **Archivos** | Storage Access Framework; sin permisos de almacenamiento; sin “compartir” |
| 16 | **Idioma** | Español es-AR solamente; textos centralizados para traducir después |
| 17 | **Respaldo automático** | No en el MVP; export manual y explícito |
| 18 | **Orden de proyectos** | Secciones por estado (activos, pausa/postergados, terminados/cancelados); sin orden manual |
| 19 | **Límites de texto** | Proyecto/tarea/lista 120; ítem 200; se valida, no se trunca en silencio |
| 20 | **“Urgente” vs “Alta”** | `Alta` ocupa el lugar de “Urgente” de la referencia; mismo color rojo suave |
| 21 | **Respaldo del sistema** | `allowBackup="false"`: el respaldo automático de Android subiría datos a la nube. El único respaldo es el export manual |

## Lo que no se agrega

- Notas de sesiones, volcado de ideas, finanzas, lectura, anillo de tiempo, calendario/agenda.
- Login, cuenta, nube, sincronización, compartir, analytics.
- Prioridad por tarea, etiquetas, adjuntos, subtareas de una tarea, dependencias, recordatorios.
- Búsqueda global, filtros avanzados, papelera, historial de cambios.
- Integración con A.R.G.O.S. Solo se deja el formato listo: IDs, timestamps y JSON estable.

Si alguna de estas aparece durante la fase 1, se anota acá y se decide; no se implementa “por si acaso”.
