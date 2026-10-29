# 0002: Metodología de trabajo

- **Estado:** propuesta, esperando aprobación junto con el stack.
- **Fecha:** 29 de octubre de 2026.
- **Decide:** cómo se corta, se prueba y se commitea el trabajo de la fase 1.

## Principios

1. **Cortes verticales pequeños.** Cada corte entrega algo que se puede ver, tocar o probar por sí solo. Si un corte no se puede explicar en una frase, es demasiado grande.
2. **Primero la UX, después todo lo demás.** Ningún corte se da por terminado porque “compila”: se instala, se usa y se mira en claro y en oscuro.
3. **La lógica de progreso y el export/import se prueban siempre.** Son las dos piezas que, si fallan, rompen la promesa de la app: progreso honesto y datos que sobreviven.
4. **Un commit por tarea terminada.** No hay commits de “a medias”, ni de “después arreglo”. El commit es el cierre del corte, no un guardado intermedio.
5. **Lo que no está en el alcance no se agrega.** Si aparece una idea nueva, se anota en `docs/decisiones/` y se decide, no se cuela en el corte.

## Un corte, una tarea, un commit

Un corte está terminado cuando:

- [ ] Cumple sus criterios de aceptación.
- [ ] La app compila y se instala con el toolchain de esta PC.
- [ ] Los tests de la lógica tocada pasan.
- [ ] Se probó a mano en el celular o emulador: claro, oscuro y seguir al sistema.
- [ ] Se revisó con escala de fuente 1.3 y 2.0; nada se corta ni se superpone.
- [ ] Los objetivos táctiles son de 48 dp como mínimo.
- [ ] No se agregó nada fuera del alcance.
- [ ] Si hubo una decisión nueva, quedó anotada en `docs/decisiones/`.
- [ ] El commit tiene mensaje en español, en imperativo.

Si un corte no llega a cumplir todo, **no se commitea**: se sigue trabajando o se parte en dos. Si hay que abandonar la sesión a mitad de camino, se deja el árbol sucio o una rama descartable; al retomar, se termina y se squashea en un solo commit.

## Plan de cortes de la fase 1

El orden no es casual: primero el esqueleto que compila, después el dominio y la persistencia, después las pantallas, y al final el export/import, que necesita que todas las formas de datos ya existan.

| # | Corte | Resultado visible | Criterios de aceptación | Tests |
|---|---|---|---|---|
| 1 | **Esqueleto** | App que abre en una pantalla vacía con el tema y la navegación base | Gradle compila e instala; Figtree carga; tema claro/oscuro/sistema; barra inferior con Proyectos, Listas y Ajustes | Build + smoke test manual |
| 2 | **Dominio y progreso** | Nada visible todavía: modelos y cálculo de progreso | Proyecto, tarea, lista e ítem con UUID; estado y prioridad; máximo 3 niveles de subproyectos; progreso calculado, nunca guardado | JUnit: progreso, árbol, vacíos, 100%, listas excluidas, ciclos |
| 3 | **Persistencia** | Los datos sobreviven a cerrar y abrir la app | Room con entidades, DAOs, claves foráneas e índices; esquema exportado; repositorio con Flows | Robolectric: DAOs, cascadas, listas desvinculadas, orden |
| 4 | **Proyectos** | Lista de proyectos y alta/edición | Crear, editar, cambiar estado, borrar con confirmación; estado vacío; persistencia | Dominio + manual |
| 5 | **Subproyectos** | Árbol de hasta 3 niveles de subproyectos | Elegir padre al crear; no permitir un cuarto nivel; borrar padre arrastra hijos; breadcrumb en detalle | JUnit: profundidad, ciclos, borrado en cascada |
| 6 | **Tareas** | Tareas dentro de un proyecto | Crear, marcar/desmarcar, fecha opcional y borrar; haptic al marcar; el progreso se actualiza | JUnit: progreso con tareas; manual de fecha |
| 7 | **Tarjeta de progreso** | La tarjeta de la referencia en la lista | Encabezado, pastilla “3 de 4” + barra + %, árbol con conectores, chips, sin avatares; se ve bien en claro y oscuro | Manual visual + casos borde |
| 8 | **Listas simples** | Pestaña de listas y detalle | Crear/editar/borrar listas; ítems; vincular/desvincular a un proyecto; las listas no suman al progreso del proyecto | JUnit: ítems, vínculo, export futuro |
| 9 | **Exportar e importar** | Ajustes con respaldo JSON | Exportar todo a un archivo elegido por el usuario; importar ese mismo archivo; validar antes de escribir; reemplazo atómico | JUnit: round-trip, archivo inválido, versión, IDs duplicados, huérfanos, profundidad, ciclos, Unicode |
| 10 | **Ajustes** | Tema y respaldo en un solo lugar | Selector de tema persistente; entrada a exportar/importar; versión de la app; sin cuenta ni nube | Manual: reinicio conserva el tema |
| 11 | **Pulido** | La app se siente terminada | Haptics, animaciones con propósito, estados vacíos, accesibilidad, textos largos, escala de fuente, gesto de atrás | Checklist de `DESIGN.md` + manual |

### Por qué este orden

- El **corte 1** valida el toolchain antes de invertir en producto. Si la build falla, se descubre con una pantalla vacía, no con la tarjeta llena de lógica.
- Los **cortes 2 y 3** son la base: sin progreso ni persistencia, las pantallas serían maquetas. No son “horizontales” porque cada uno tiene tests y un contrato claro.
- Los **cortes 4 a 8** son verticales: cada uno agrega una capacidad que se puede usar.
- El **corte 9** va después de que existan todas las formas de datos. Exportar antes sería exportar un modelo que todavía cambia.
- El **corte 11** no es “opcional”: es donde se corrige lo que los cortes anteriores dejaron rústico. Sin este corte, la app funciona pero no se siente cuidada.

## Tests

### Lógica de progreso (JUnit, sin Android)

Casos mínimos:

- Proyecto sin tareas ni subproyectos: **sin progreso**, no “0%”. La UI dice “Sin tareas todavía”.
- Proyecto con 3 de 4 tareas: “3 de 4”, 75%.
- Tareas dentro de subproyectos: suman al proyecto padre.
- Tres niveles de subproyectos: una tarea en el nivel 3 suma al proyecto raíz.
- Subproyecto terminado con tareas pendientes: sus pendientes siguen contando.
- Listas vinculadas: **no** suman al progreso del proyecto.
- 100%: solo cuando no queda ninguna tarea pendiente en todo el árbol.
- 99,6%: se muestra 99%, nunca 100% si falta una tarea.
- Ciclo o padre inexistente: se rechaza, no se calcula.
- Un cuarto nivel de subproyectos: se rechaza.

La regla exacta de cálculo vive en `docs/decisiones/0003-modelo-de-datos.md`; si cambia, cambian los tests y el documento en el mismo commit.

### Exportar/importar (JUnit, sin Android)

- Round-trip: exportar → importar → exportar produce el mismo contenido (mismos IDs, mismos campos, mismo orden).
- Nombres de campos en español, versión del formato y UTF-8.
- Archivo vacío: exporta e importa datos vacíos (arrays vacíos) sin romper.
- Archivo inválido: JSON roto, versión desconocida, campo requerido faltante, ID duplicado, `proyecto_id` huérfano, ciclo, cuarto nivel de subproyectos.
- Un import inválido **no deja datos a medias**: los datos anteriores quedan intactos.
- Títulos largos, emoji y acentos sobreviven.
- Campos desconocidos: se ignoran (compatibilidad futura), no rompen.
- Fechas: `YYYY-MM-DD` para vencimientos; ISO-8601 para creado/actualizado.

### Persistencia (Robolectric)

- DAOs: alta, edición, baja y consulta.
- Borrar proyecto: arrastra subproyectos y tareas; las listas vinculadas **no** se borran, quedan sueltas.
- Orden estable de listas e ítems después de reiniciar.
- Migraciones: el esquema de Room se exporta y se commitea; si cambia, se escribe y se prueba la migración.

### UI

- Tests de Compose solo para flujos críticos (marcar tarea, crear proyecto, importar). No se testea cada píxel.
- El resto es checklist manual con capturas en claro y oscuro, fuente 1.0 y 2.0.

## Commits

- **Un commit por corte terminado.** No se commitean archivos sueltos “para no perderlos”.
- **Mensaje en español, en imperativo**, como en los otros repos:
  - `Agrega el cálculo de progreso con tests`
  - `Guarda proyectos y tareas en Room`
  - `Diseña la tarjeta de progreso`
  - `Exporta e importa todos los datos en JSON`
  - `Corrige el progreso de los subproyectos vacíos`
- Sin prefijos tipo `feat:` ni `fix:`: el repo no los usa.
- El commit incluye código, tests y documentación del corte. No se mezclan cortes.
- Antes de commitear: `git diff --check`, build, tests y una mirada al diff. Nunca se commitean APKs, `local.properties`, keystores ni capturas temporales.
- No se commitea si la build o los tests fallan. Si el corte es grande, se parte; no se “commitea igual y se arregla después”.

## Ramas y versiones

- Se trabaja sobre `main`; es un repo personal, sin PR.
- Si un corte es riesgoso (por ejemplo, una migración de Room), se puede usar una rama corta y volver a `main` con un solo commit al terminar.
- Al cerrar el MVP se etiqueta `v0.1.0` y se actualiza el README con el estado real.
- No hay releases automáticas ni CI en la fase 1. La build es local.

## Datos y respaldo

- Antes de una migración de Room, se exporta un JSON y se guarda fuera del celular.
- El esquema de Room se commitea (`app/schemas/`); sin eso no hay migraciones confiables.
- El import es atómico: valida todo, y recién entonces reemplaza. Si algo falla, no toca los datos actuales.
- El export es manual y explícito. No hay backup automático, nube ni sincronización en el MVP.

## Decisiones nuevas

Si aparece algo que este documento o el prompt no cubren —por ejemplo, qué pasa si se borra un proyecto con listas vinculadas—, no se resuelve en silencio:

1. Se anota en `docs/decisiones/0003-modelo-de-datos.md` (o en un `0004` si es otro tema).
2. Se propone una opción y se marca como “a confirmar”.
3. Se implementa solo después de que la decisión esté aprobada o de que el usuario haya dicho “dale”.

## Definición de terminado del MVP

El MVP está terminado cuando se pueden cumplir todos los criterios de aceptación del prompt en un celular real, sin conexión, con datos que sobreviven a un reinicio, exportando e importando el mismo archivo, en claro y en oscuro, y con los tests de progreso y export/import en verde. La tarjeta se parece a la referencia y no muestra datos inventados.
