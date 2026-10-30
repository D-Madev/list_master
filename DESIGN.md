# DESIGN.md — List Master

- **Estado:** aprobado e implementado en la versión 1.1.0. La identidad pasó a blanco, negro y escalas de grises.
- **Alcance:** proyectos, subproyectos, tareas y listas simples. Nada más.
- **Referencia visual:** `docs/referencias/tarjeta-progreso.png`.
- **Referencias de producto:** Linear (app), Notion (layout) y los ejemplos de sistemas de diseño de Figma: https://www.figma.com/resource-library/design-system-examples/
- **Familia:** la misma que `registro_sueno`, sin copiar su código.

Esto no es un mockup: es el criterio con el que se juzga cada pantalla antes de mirar si “funciona”. Si una instrucción del prompt es ambigua o fea, se interpreta a favor de la mejor experiencia posible dentro de esta identidad, y se señala antes de construir.

## Principio rector

**Primero la UX, después todo lo demás.** La app se usa con una mano, en la calle, sin conexión, con el celular a media luz o a pleno sol. El estándar es que se sienta cuidada: simple, legible, con profundidad y sin ruido. No es una app austera ni una app de demostración: es una herramienta personal que tiene que dar ganas de abrir.

Tres reglas que mandan sobre el resto:

1. **Nunca inventar datos.** Si no hay tareas, no hay 0%: hay “Sin tareas todavía”. Si no hay fecha, no hay fecha. Si no hay prioridad, no se muestra una prioridad falsa.
2. **Lo que se ve, se puede tocar.** Un círculo de check, una tarjeta, un chip: si parece interactivo, lo es. Si no lo es, no lo parece.
3. **La profundidad no es decoración.** Separa planos y ayuda a entender qué es una tarjeta, qué es una hoja y qué es el fondo. Nunca una tarjeta del mismo color que el fondo.

### Referencias y qué tomar de cada una

- **Linear (app):** densidad justa, jerarquía tipográfica clara, navegación rápida y microinteracciones sobrias. Nada de adornos: cada píxel ayuda a ubicarse.
- **Notion (layout):** estructura de página con bloques, propiedades con chips, árbol de contenido y la sensación de que el contenido manda sobre el chrome.
- **Figma — Design System Examples:** https://www.figma.com/resource-library/design-system-examples/ — patrones de tokens, componentes y documentación para mantener la identidad consistente.

No se copia ninguna interfaz: se toman criterios. La identidad sigue siendo la de `docs/referencias/tarjeta-progreso.png`, ahora en blanco, negro y grises.

## 1. Identidad visual

### 1.1 Tipografía

**Figtree variable, pesos 400 a 900.** Se reutiliza el `.ttf` de `registro_sueno` (`figtree_variable.ttf`, licencia OFL) o se descarga de Google Fonts. La app incluye la licencia.

- Títulos en **SemiBold (600)**, no en Black. La jerarquía se construye con tamaño y color, no con peso extremo.
- Cuerpo en **Regular (400)**; énfasis puntual en **Medium (500)**.
- Botones, chips y rótulos en **SemiBold (600)**.
- 800 y 900 quedan disponibles para una marca o un número heroico; no se usan en la interfaz del MVP.
- Tamaños en `sp`, nunca en `dp` fijos: la app respeta la escala de fuente del sistema.
- Interlineado generoso en cuerpo (1.45–1.5); ajustado en títulos (1.2–1.25).

| Rol | Tamaño / interlínea | Peso | Uso |
|---|---|---|---|
| Display | 28 / 34 | 700 | Bienvenida o número grande; poco frecuente |
| Headline | 24 / 30 | 600 | Título de pantalla (“Mis Proyectos”) |
| Title Large | 20 / 26 | 600 | Título de la tarjeta de proyecto |
| Title Medium | 17 / 24 | 600 | Título de sección o de lista |
| Body Large | 16 / 24 | 400 | Texto de detalle |
| Body | 15 / 22 | 400 | Tareas, ítems, párrafos |
| Label Large | 13 / 18 | 600 | Botones y chips |
| Label | 12 / 16 | 600 | Pastilla de progreso, metadatos |
| Caption | 11 / 15 | 500 | Solo texto no esencial; nunca una acción |

**Reglas de lectura**

- Ningún texto de cuerpo por debajo de 15 sp. Los 12 sp son para metadatos y chips, no para párrafos.
- Nada de mayúsculas sostenidas: la identidad usa Mayúscula Palabra Por Palabra, no `UPPERCASE`.
- Números tabulares si Figtree los soporta (`tnum`); si no, se acepta el ancho natural. La pastilla de progreso no debe “bailar” al cambiar de 9 a 10.
- Un título largo se corta a 2 líneas con elipsis en la tarjeta; en el detalle se ve completo.
- El texto de títulos y párrafos se puede seleccionar; los botones y chips, no.

### 1.2 Color

La paleta es **monocroma**: negro, blanco y escalas de grises. Nada de azul ni de colores de estado. La jerarquía se construye con contraste, tamaño, iconos y texto, no con tono. **No se usa color dinámico (Material You)**: rompería la identidad.

#### Tema claro

| Token | Hex | Uso |
|---|---|---|
| `bg` | `#F7F7F7` | Fondo de pantalla |
| `surface` | `#FFFFFF` | Tarjetas, hojas, diálogos |
| `surfaceAlt` | `#EFEFEF` | Inputs, tiles de icono, pastilla de progreso |
| `surfaceSunken` | `#E2E2E2` | Riel de la barra, separadores |
| `text` | `#0B0B0B` | Títulos y cuerpo |
| `textSecondary` | `#565656` | Metadatos, texto de apoyo |
| `textTertiary` | `#6B6B6B` | Iconos en reposo, placeholders |
| `outline` | `#D6D6D6` | Bordes de 1 dp |
| `accent` | `#111111` | Acción primaria, progreso, foco |
| `onAccent` | `#FFFFFF` | Texto e iconos sobre `accent` |
| `accentSoft` | `#EAEAEA` | Fondo de chips activos |
| `success` | `#1C1C1C` | Terminado y progreso completo |
| `successSoft` | `#E3E3E3` | Apoyo de Terminado |
| `warning` | `#3F3F3F` | Postergado |
| `warningSoft` | `#EDEDED` | Chip Postergado |
| `danger` | `#000000` | Cancelado y acciones destructivas |
| `dangerSoft` | `#E6E6E6` | Chip Cancelado |
| `neutral` | `#5F5F5F` | Pausa |
| `neutralSoft` | `#F1F1F1` | Chip Pausa |

#### Tema oscuro

| Token | Hex | Uso |
|---|---|---|
| `bg` | `#0B0B0B` | Fondo de pantalla |
| `surface` | `#141414` | Tarjetas, hojas, diálogos |
| `surfaceAlt` | `#1C1C1C` | Inputs, tiles de icono, pastilla de progreso |
| `surfaceSunken` | `#0F0F0F` | Riel de la barra, separadores |
| `text` | `#F2F2F2` | Títulos y cuerpo |
| `textSecondary` | `#B3B3B3` | Metadatos, texto de apoyo |
| `textTertiary` | `#8F8F8F` | Iconos en reposo, placeholders |
| `outline` | `#2E2E2E` | Bordes de 1 dp |
| `accent` | `#F2F2F2` | Acción primaria, progreso, foco |
| `onAccent` | `#0B0B0B` | Texto e iconos sobre `accent` |
| `accentSoft` | `#262626` | Fondo de chips activos |
| `success` | `#FFFFFF` | Terminado y progreso completo |
| `successSoft` | `#242424` | Apoyo de Terminado |
| `warning` | `#D6D6D6` | Postergado |
| `warningSoft` | `#2A2A2A` | Chip Postergado |
| `danger` | `#FFFFFF` | Cancelado y acciones destructivas |
| `dangerSoft` | `#2C2C2C` | Chip Cancelado |
| `neutral` | `#A6A6A6` | Pausa |
| `neutralSoft` | `#232323` | Chip Pausa |

#### Reglas de uso

- El **acento** es para lo interactivo y para el progreso. No se pinta una tarjeta entera de acento salvo un estado seleccionado muy puntual.
- Sin color, los estados se distinguen por **icono + texto + tono**, y los dos estados fuertes se **invierten**: `Terminado` y prioridad `Alta` usan fondo `text` y contenido `surface` (chip negro en claro, chip blanco en oscuro). El resto usa fondos suaves.
- Los **colores de estado** viven en chips, iconos pequeños y barras. No tiñen el fondo de la tarjeta.
- Nunca se usa el tono como único portador de información: el chip siempre tiene icono + texto.
- El texto sobre fondos suaves debe cumplir contraste AA (4.5:1 para texto normal). Los pares de la tabla están elegidos para eso; si se cambia un tono, se vuelve a medir.
- En oscuro, el fondo es casi negro (`#0B0B0B`). Las tarjetas son más claras que el fondo. En claro, la tarjeta es blanca y el fondo es gris muy claro; la separación la hace la sombra + el borde.
- No hay degradados planos como decoración. El único gradiente permitido es el **brillo ambiental** del fondo oscuro (ver 1.3).

### 1.3 Profundidad, forma y luz

**La app tiene planos, no capas flotando porque sí.** Cada nivel tiene una razón:

| Nivel | Claro | Oscuro | Uso |
|---|---|---|---|
| 0 · Fondo | `bg` | `bg` | Pantalla |
| 1 · Tarjeta | `surface` + sombra suave + borde 1 dp | `surface` + borde 1 dp + brillo superior | Proyectos, listas |
| 2 · Hoja / diálogo | `surface` + sombra más alta | `surfaceAlt` + borde + scrim | Acciones, formularios |
| 3 · Menú | `surface` + sombra alta | `surfaceAlt` + borde | Menú “⋯” |

**Sombra en claro:** suave y ancha, nunca un borde duro. Equivalente visual: `0 4dp 16dp rgba(13,17,23,0.06)` + `0 1dp 3dp rgba(13,17,23,0.05)`. La tarjeta se despega del `#F6F8FB` sin ensuciar.

**Sombra en oscuro:** una sombra negra no se ve. La tarjeta se separa con:
- superficie más clara que el fondo (`#171E27` sobre `#0D1117`),
- borde de 1 dp `#2A333F`,
- un brillo muy sutil en el borde superior (`rgba(255,255,255,0.04)`),
- y, detrás del contenido de la pantalla, un **brillo ambiental** radial con el acento al 5–7%, que se apaga hacia abajo. Es luz, no un degradado de relleno.

**Regla dura:** si una tarjeta se confunde con el fondo, está mal. En claro, la sombra y el borde tienen que notarse; en oscuro, la superficie tiene que ser visiblemente más clara.

**Radios**

| Elemento | Radio |
|---|---|
| Tarjeta de proyecto / lista | 20 dp |
| Tile de icono | 14 dp |
| Input | 14 dp |
| Botón | 14 dp |
| FAB extendido | 18 dp |
| Hoja inferior | 24 dp (esquinas superiores) |
| Diálogo | 24 dp |
| Chip / pastilla | 999 dp (píldora) |

**Nunca** se anidan tarjetas con sombra dentro de tarjetas con sombra. Si hace falta un bloque interno, se usa `surfaceAlt` + un divisor de 1 dp, sin sombra.

### 1.4 Iconografía

- **Iconos de línea, no rellenos.** Trazo de 1.5–1.75 dp, terminaciones redondeadas.
- Tamaños: 16 dp dentro de chips, 20 dp en filas, 22–24 dp en encabezados y navegación.
- En reposo, 80% de opacidad; activo, 100% y color de acento.
- El único “relleno” permitido es el **círculo de check lleno** de una tarea hecha: es un indicador de estado, no un icono decorativo.
- Se usan Material Symbols Outlined o vectores propios; nunca un set mezclado.
- Iconos del MVP: `folder` (proyecto), `layers` (subproyecto), `check-circle` / `circle` (tarea), `flag` (prioridad), `hourglass` o `schedule` (estado), `list` (lista), `calendar` (fecha), `more-horiz` (menú), `plus`, `chevron-right`, `arrow-left`, `trash`, `pencil`, `download`, `upload`, `sun`, `moon`, `smartphone`.
- Todo icono interactivo tiene etiqueta accesible; todo icono decorativo tiene `contentDescription = null`.

### 1.5 Espaciado y ritmo

Base de **4 dp**. Escala: 4, 8, 12, 16, 20, 24, 32.

| Uso | Valor |
|---|---|
| Margen horizontal de pantalla | 20 dp |
| Padding interno de tarjeta | 16 dp (compacta) / 20 dp (detalle) |
| Separación entre tarjetas | 12 dp |
| Separación entre secciones | 24 dp |
| Gap entre icono y texto | 8–10 dp |
| Altura mínima de objetivo táctil | 48 dp |
| Separación mínima entre objetivos | 8 dp |
| Altura de barra inferior | 80 dp + insets |
| FAB | 56 dp, margen 20 dp |
| Barra de progreso | 5 dp de alto |

**Mobile first:** una columna, sin scroll horizontal, con el contenido principal en la mitad inferior de la pantalla cuando se pueda. Las acciones destructivas van en hojas inferiores, no en el borde superior inalcanzable.

**Tablet:** no se diseña para tablet en el MVP. Si la app se abre en una pantalla ancha, el contenido se centra con un ancho máximo de 640 dp; no se estira una tarjeta a lo ancho de un tablet.

## 2. Voz y copy

### 2.1 Rotulado

- **Mayúscula Palabra Por Palabra** en rótulos y etiquetas: “Mis Proyectos”, “En Curso”, “Sin Fecha”, “Viernes 30 de Octubre”.
- Artículos y preposiciones cortas (`de`, `del`, `el`, `la`, `los`, `las`, `y`, `en`, `por`, `para`) van en minúscula, salvo que abran el rótulo: “Viernes 30 de Octubre”, “En Curso”.
- Las **frases** van en oración normal, no en Title Case: “Todavía no tenés proyectos”, “Te quedan 2 tareas”.
- Nada de mayúsculas sostenidas ni de abreviaturas crípticas. Si el espacio obliga, se abrevia solo lo obvio (“Vie 30 Oct”), nunca el estado.

### 2.2 Segunda persona

La app le habla a una sola persona, de vos, y cuenta lo que pasó como se habla:

- “Tenés 3 proyectos activos.”
- “Hiciste 4 de 7.”
- “Te quedan 2 tareas para terminar.”
- “Marcá el proyecto como Terminado cuando estés listo.”
- “No pudimos leer el archivo. Fijate que sea un JSON exportado por List Master.”

No: “Proyectos activos: 3”, “Tareas completadas: 4/7”, “Error de importación”.

### 2.3 Estados y etiquetas

| Valor guardado | Etiqueta en la UI | Color | Icono |
|---|---|---|---|
| `Activo` | **Activo** | `accent` / `accentSoft` | `play` o `circle-dot` |
| `Pausa` | **En Pausa** | `neutral` / `neutralSoft` | `pause` |
| `Postergado` | **Postergado** | `warning` / `warningSoft` | `schedule` |
| `Terminado` | **Terminado** | `success` / `successSoft` | `check` |
| `Cancelado` | **Cancelado** | `danger` / `dangerSoft` | `x` |

**Prioridad:**

| Valor | Etiqueta | Color | Icono |
|---|---|---|---|
| `Alta` | **Alta** | `danger` / `dangerSoft` | `flag` |
| `Media` | **Media** | `warning` / `warningSoft` | `flag` |
| `Baja` | **Baja** | `neutral` / `neutralSoft` | `flag` |
| `null` | Sin chip | — | — |

La referencia muestra “Urgente”; en una app personal de tres niveles, “Alta” ocupa ese lugar. Si se prefiere “Urgente”, se cambia el vocabulario en `0003` y se mantiene el mismo color.

### 2.4 Progreso

- Pastilla: **“3 de 4”** y **“75%”**. Sin espacio antes del `%`, para que la pastilla no se estire.
- Sin tareas en todo el árbol: **“Sin tareas todavía”**, sin barra y sin porcentaje. No es 0%.
- Con tareas y ninguna hecha: **“0 de 4”** y **“0%”**. Eso sí es un dato real.
- Todo hecho: **“Todo hecho”** y **“100%”**. La barra pasa a `success`.
- Nunca 100% si queda una tarea pendiente: el porcentaje se redondea, pero se topa en 99% hasta que el total esté completo.
- Las listas simples **no** suman al progreso del proyecto. Su progreso es propio.

### 2.5 Fechas

- Fecha completa en detalle: “Viernes 30 de Octubre”.
- En tarjeta, si entra: “Vie 30 Oct”; si no, se omite la fecha, no se inventa.
- Sin fecha: “Sin Fecha” en el detalle, nunca “Hoy”.
- Vencimiento: solo fecha, sin hora. No hay recordatorios ni calendario en el MVP.

### 2.6 Errores y confirmaciones

- **Borrar proyecto:** “¿Borrar «Diseño Web»? También se van a borrar sus subproyectos y tareas. Las listas vinculadas quedan sueltas. Esto no se puede deshacer.”
- **Borrar lista:** “¿Borrar «Compras»? Se van a borrar sus ítems. Esto no se puede deshacer.”
- **Importar:** “Importar reemplaza todos tus datos actuales. ¿Querés exportar primero?”
- **Exportar bien:** “Listo, guardaste todo en `list-master-2026-10-29.json`.”
- **Exportar mal:** “No pudimos exportar. Probá elegir otra carpeta.”
- **Importar mal:** “No pudimos importar. El archivo no parece un export de List Master o está dañado. Tus datos actuales no se tocaron.”

## 3. Componentes

### 3.1 Tarjeta de proyecto (la pieza central)

```
┌──────────────────────────────────────────┐
│  [icono]  Diseño Web                 [⋯] │
│  (✓) 3 de 4   ▓▓▓▓▓▓▓░░░   75%          │
│   ├─ ✓ Diseño De Tokens                   │
│   ├─ ✓ Sistema De Color                   │
│   ├─ ✓ Sistema De Tipografía              │
│   └─ ○ Documentación                      │
│  ⚑  Prioridad        [ Alta        ⌄ ]   │
│  ⏳  Estado           [ Activo      ⌄ ]   │
└──────────────────────────────────────────┘
```

**Anatomía**

1. **Encabezado:** tile de icono de 40–44 dp (`surfaceAlt`, radio 14 dp, icono de línea de 22 dp), título a 20 sp SemiBold, menú “⋯” a la derecha en un objetivo de 48 dp.
2. **Pastilla de progreso:** una sola píldora con icono de check, “3 de 4”, barra y “75%”. Fondo `surfaceAlt`; en oscuro, un punto más claro que la tarjeta.
3. **Árbol:** conectores de 1 dp en `outline`, con una vertical que baja desde la pastilla y una horizontal de 10 dp hacia cada fila. Las tareas hechas llevan círculo lleno de acento con el check en `onAccent` (blanco en claro, casi negro en oscuro); las pendientes, círculo vacío de 1.5 dp.
4. **Chips:** fila de Prioridad y fila de Estado, cada una con icono + rótulo a la izquierda y chip a la derecha. El chip tiene fondo suave, texto del color fuerte, icono y un chevron de 12 dp.
5. **Sin avatares.** La app es personal. Esa fila no se reemplaza por datos inventados: la tarjeta termina en los chips.

**Comportamiento**

- Tocar la tarjeta (fuera del check y del menú) abre el detalle del proyecto.
- Tocar el círculo de check marca/desmarca la tarea sin abrir el detalle; el objetivo táctil es de 48 dp aunque el círculo se vea de 20 dp.
- Tocar el menú abre una hoja inferior con: Editar, Cambiar Estado, Agregar Subproyecto, Agregar Tarea, Eliminar.
- Tocar un chip de Estado o Prioridad abre la hoja de selección correspondiente.
- La tarjeta se hunde apenas al presionar (`scale 0.995`, 120 ms) y vuelve; no se anima el alto.
- Estado `Terminado`: la tarjeta no desaparece de la lista; se agrupa abajo, con la barra en `success` y el chip verde. Estado `Cancelado`: se agrupa abajo, con el título en `textSecondary` y el chip rojo suave; no se baja la opacidad de toda la tarjeta (rompería el contraste) y se oculta la barra de progreso.
- Título de 2 líneas máximo; si no entra, elipsis. El detalle muestra el título completo.

**Qué se muestra en el árbol de la tarjeta**

La tarjeta muestra **un nivel**: primero las tareas directas del proyecto, después una fila por subproyecto con su propio “2 de 5”. No expande las tareas de los subproyectos en la tarjeta: eso haría la tarjeta interminable. El detalle sí muestra el árbol completo hasta 3 niveles de subproyectos (raíz + 3).

- Máximo 4 filas visibles. Si hay más: “+3 Más”, que abre el detalle.
- Orden natural de creación, no “pendientes primero”: el árbol no salta de lugar cuando marcás algo. El detalle puede ofrecer un filtro “Pendientes” más adelante, pero no en el MVP.
- Si el proyecto no tiene ni tareas ni subproyectos: la pastilla dice “Sin tareas todavía” y el árbol no se dibuja. En su lugar, una línea: “Agregá una tarea para empezar a medir el progreso.”

### 3.2 Pastilla de progreso

- Alto 32 dp, radio píldora, padding horizontal 10 dp.
- Icono de check de 16 dp, texto “3 de 4” en 12 sp SemiBold, barra de 56–80 dp (flexible), texto “75%” en 12 sp SemiBold.
- Barra: riel `surfaceSunken`, relleno `accent`, 5 dp de alto, radio píldora. A 100%, el relleno pasa a `success`.
- Animación: el ancho de la barra va de un valor al otro en 300 ms `ease-out`; el cambio de color a `success` es un crossfade de 200 ms. El número cambia sin contar dígito por dígito.
- En escala de fuente grande, la pastilla puede pasar a dos líneas: texto arriba, barra abajo. No se recorta.
- Accesibilidad: “3 de 4 tareas hechas, 75 por ciento”.

### 3.3 Árbol de tareas y subproyectos

- Indentación de 16 dp por nivel; el conector vertical se mantiene alineado con el padre.
- Fila de 32–36 dp de alto; check de 20 dp; texto de 15 sp; gap de 10 dp.
- Tarea hecha: check lleno `accent`, texto `textSecondary`; **sin tachado**. El tachado ensucia la tarjeta y no aporta: el check ya dice todo. Cuando el proyecto está Terminado y no queda ninguna pendiente, el check y la barra pasan a `success`.
- Tarea pendiente: círculo de 1.5 dp `outline`, texto `text`.
- Subproyecto: icono `layers` de 18 dp, título, y una mini pastilla “2 de 5” a la derecha. Tocar la fila abre el subproyecto.
- Fecha: si existe, va debajo del título en 12 sp `textSecondary`, con icono de calendario de 12 dp. Si no, no se muestra nada.
- Los conectores se dibujan con `Canvas`/`drawBehind`, no con caracteres `├─`.
- En el detalle, el árbol completo puede ser largo: se permite scroll; las filas mantienen el conector. Si hay 3 niveles de subproyectos, el nivel 3 se ve con 48 dp de indentación (16 dp por nivel) y el texto no se corta.

### 3.4 Chips de prioridad y estado

- Alto 28 dp, radio píldora, padding horizontal 8 dp, icono 14 dp, texto 12 sp SemiBold, chevron 12 dp.
- Fondo suave (`*Soft`), texto fuerte, sin borde. En oscuro, el fondo suave no se aclara demasiado: el chip tiene que seguir leyéndose como una píldora, no como una alarma.
- Objetivo táctil de 48 dp: se logra con padding vertical/`minimumInteractiveComponentSize`, no agrandando el chip visual.
- La fila de prioridad se **omite** si el proyecto no tiene prioridad. En el detalle se muestra “Sin Prioridad” con una acción para asignarla.
- La fila de estado siempre está.
- Si el texto del chip no entra (fuente grande), el chip crece a lo ancho y la fila pasa a dos líneas; nunca se recorta el estado.

### 3.5 Tarjeta y detalle de lista simple

- Tarjeta más simple que la de proyecto: icono `list`, título, “4 de 7” y una barra fina; si está vinculada, un chip pequeño “En «Proyecto»”.
- Sin chips de prioridad ni estado: una lista simple no tiene estado de proyecto.
- Detalle: título, vínculo (si existe), ítems con check, campo para agregar al final (“Agregar Ítem”), menú con Editar, Vincular/Desvincular y Eliminar.
- Marcar un ítem actualiza “4 de 7” con la misma animación de la barra; no hay celebración.
- Las listas sueltas viven en la pestaña Listas. Las vinculadas también, con su chip; no se duplican dentro del proyecto.

### 3.6 Botones, FAB y navegación

- **Primario:** relleno `accent`, texto `onAccent`, alto 48 dp, radio 14 dp, 13 sp SemiBold. Presionado: `scale 0.97` en 120 ms.
- **Secundario:** fondo `accentSoft`, texto `accent`, misma altura.
- **Texto:** solo texto `accent`; para acciones terciarias.
- **Destructivo:** texto `danger`, siempre con confirmación. No se usa un botón rojo lleno para borrar en la tarjeta; sí en el diálogo de confirmación.
- **FAB:** extendido en las pantallas de lista (“Nuevo Proyecto”, “Nueva Lista”), con icono `plus` y texto; 56 dp de alto, radio 18 dp, sombra. En el detalle, FAB circular de 56 dp para “Agregar Tarea” o “Agregar Ítem”.
- **Barra inferior:** 3 destinos: Proyectos (`folder`), Listas (`list`), Ajustes (`tune`). Alto 80 dp + inset; fondo `surface`, borde superior de 1 dp `outline`; activo con píldora `accentSoft` detrás del icono y texto `accent`; inactivo `textTertiary`. Sin haptics al navegar.
- **Detalle:** se oculta la barra inferior; el botón de atrás está en la barra superior y el gesto de atrás del sistema funciona. No se intercepta.
- **Barra superior:** título 24 sp SemiBold, fondo `bg` (no transparente sobre contenido que se desplaza), acciones a la derecha. En detalle: atrás, título y menú “⋯”.
- **Snackbar:** para confirmaciones transitorias (“Listo, guardaste todo”), con acción “Ver” solo si hay algo que ver. No se usa un toast para una decisión.

### 3.7 Estados vacíos

- Centrados, ancho máximo 320 dp, icono de línea de 40 dp al 40% de opacidad, título 20 sp SemiBold, cuerpo 15 sp `textSecondary`, una acción primaria.
- **Proyectos:** “Todavía no tenés proyectos.” / “Creá el primero y va a aparecer acá.” / “Nuevo Proyecto”.
- **Listas:** “Todavía no tenés listas.” / “Armá una para las compras, las películas o lo que se te ocurra.” / “Nueva Lista”.
- **Tareas:** “Este proyecto no tiene tareas.” / “Agregá la primera para empezar a medir el progreso.” / “Agregar Tarea”.
- Nunca se rellena un estado vacío con proyectos de ejemplo, avatares o datos falsos.

### 3.8 Formularios, hojas y diálogos

- **Crear/editar proyecto:** pantalla completa; campos: Título (obligatorio, autofocus), Estado (por defecto Activo), Prioridad (opcional), Proyecto Padre (solo si se crea un subproyecto; se muestra read-only si viene del detalle). Sin descripción: no está en el modelo.
- **Crear/editar tarea:** hoja inferior alta; Título (obligatorio), Fecha (opcional, con “Sin Fecha” y una × para limpiar). Sin prioridad por tarea: la prioridad es del proyecto en el MVP.
- **Crear/editar lista:** pantalla completa; Título (obligatorio), Proyecto vinculado (opcional, selector simple sin búsqueda en el MVP).
- **Campo de texto:** alto 48 dp, fondo `surface`, borde 1 dp `outline`, radio 14 dp; foco con borde `accent` de 2 dp; label arriba en 12 sp SemiBold `textSecondary`; error debajo en 12 sp `danger`.
- **Teclado:** el botón de guardar queda visible por encima del teclado; el campo activo no queda tapado. Acción “Listo”/“Siguiente” en el teclado.
- **Selector de estado/prioridad:** hoja inferior con una fila por opción, icono + etiqueta + check en la seleccionada. Tocar una opción la aplica y cierra; no hay botón “Guardar” extra.
- **Fecha:** date picker de Material 3, solo fecha; al elegir, se muestra “Viernes 30 de Octubre” y una × para limpiar.
- **Diálogo de borrado:** título con el nombre entre comillas angulares, cuerpo con la consecuencia exacta, botón destructivo y “Cancelar”. No se borra con un swipe en el MVP.
- **Límites de texto:** proyecto/tarea/lista 120 caracteres; ítem de lista 200. Se valida al guardar; no se trunca en silencio.

## 4. Pantallas del MVP

### 4.1 Proyectos (inicio)

- Barra superior “Mis Proyectos”.
- Línea de estado: “Tenés 3 proyectos activos.” Si no hay: se omite y se muestra el estado vacío.
- Secciones: **Activos** primero; **En Pausa / Postergados** después; **Terminados / Cancelados** al final, colapsados con “Ver Terminados y Cancelados (4)”.
- Tarjetas de proyecto, separadas 12 dp.
- FAB extendido “Nuevo Proyecto” abajo a la derecha.
- Sin filtros ni búsqueda en el MVP: las secciones alcanzan para una app personal.

### 4.2 Detalle de proyecto

- Barra superior: atrás, título, menú “⋯”.
- Encabezado: icono, título completo, pastilla de progreso, chips de prioridad/estado.
- Árbol completo: subproyectos y tareas, hasta 3 niveles de subproyectos, con conectores.
- Botones “Agregar Tarea” y “Agregar Subproyecto” (este último oculto en el tercer nivel de subproyectos).
- Si el proyecto está Terminado y quedan tareas pendientes: aviso honesto “Te quedan 2 tareas sin hacer.” No se marcan solas.
- Si el progreso llega a 100% pero el estado sigue Activo: “Todo hecho. ¿Lo marcás como Terminado?” con una acción; no se cambia el estado solo.
- Menú: Editar, Cambiar Estado, Agregar Subproyecto, Agregar Tarea, Eliminar.

### 4.3 Listas

- Barra superior “Mis Listas”.
- Listas sueltas y vinculadas en una sola lista, con chip de vínculo cuando corresponde.
- FAB extendido “Nueva Lista”.
- Estado vacío propio.

### 4.4 Detalle de lista

- Barra superior: atrás, título, menú.
- “4 de 7” y barra fina; chip de vínculo si tiene proyecto.
- Ítems con check; agregar al final; editar el título de la lista desde el menú.
- Eliminar con confirmación.

### 4.5 Ajustes

- **Tema:** Claro / Oscuro / Seguir Al Sistema, con una vista previa mínima (no un selector frío). Se guarda y se aplica al instante.
- **Tus Datos:** Exportar Todo (JSON), Importar JSON. Texto de ayuda: “Tus datos quedan en este celular. Exportá cuando quieras un respaldo.”
- **Acerca De:** nombre, versión, “Hecho Para Vos”. Sin cuenta, sin analytics, sin nube.
- No hay “Sincronizar”, “Compartir” ni “Iniciar Sesión”.

## 5. Movimiento y haptics

### 5.1 Movimiento

| Acción | Qué se mueve | Duración | Curva |
|---|---|---|---|
| Marcar/desmarcar tarea | check `0.9 → 1`, relleno, barra | 180 ms / 300 ms | `ease-out` |
| Cambio de estado | chip (color + texto) | 200 ms | `ease` |
| Proyecto a Terminado | barra a `success`, chip | 250 ms | `ease-out` |
| Abrir hoja inferior | translateY + fade | 250 ms | `ease-out` |
| Cerrar hoja | fade + translateY | 180 ms | `ease-out` |
| Navegación a detalle | fade-through / shared axis | 220 ms | estándar |
| Volver | reverso | 200 ms | estándar |
| Presión de botón | `scale 0.97` | 120 ms | `ease-out` |
| Agregar ítem | fade + 8 dp de abajo hacia arriba | 180 ms | `ease-out` |
| Cambio de pestaña | **sin animación** | — | — |

Curvas: `ease-out` = `cubic-bezier(0.23, 1, 0.32, 1)`; `ease` = `cubic-bezier(0.4, 0, 0.2, 1)`; `estándar` = `cubic-bezier(0.2, 0, 0, 1)`. En Compose se implementan con `CubicBezierEasing`, no con las curvas por defecto.

Reglas:

- Nada de animaciones de más de 300 ms en interacción.
- No se anima `height`, `padding` ni `width` de layouts; solo transformaciones, opacidad y el ancho de la barra de progreso (que es una barra fina y no reordena la pantalla).
- No hay animación en acciones de teclado ni en navegación repetida.
- **Reduced motion:** si el sistema tiene “Quitar animaciones”, se reemplaza por crossfade de 120 ms o corte instantáneo; nunca se elimina el feedback de estado (el check y el color cambian igual).
- No hay confeti, no hay rebotes exagerados, no hay animaciones de “celebración” al completar. La recompensa es el estado claro y el haptic.

### 5.2 Haptics

| Acción | Haptic | Nota |
|---|---|---|
| Marcar/desmarcar tarea | `Confirm` (API 30+) / `LongPress` (fallback) | Un solo tick |
| Proyecto marcado Terminado | `Confirm` | Solo cuando el usuario lo marca |
| Borrado confirmado | `Reject` (API 30+) / `LongPress` | No en el diálogo, al confirmar |
| Cambio de estado a Cancelado | `Reject` | Es una decisión terminal |
| Navegación, tabs, scroll, abrir menú | **Ninguno** | El movimiento no es un gesto significativo |
| Escribir, elegir fecha | **Ninguno** | El teclado ya da feedback |

Los haptics respetan el ajuste del sistema. Si el dispositivo no los soporta, no pasa nada: la app no depende de ellos para comunicar.

## 6. Accesibilidad

- **Objetivos táctiles:** 48 × 48 dp mínimo, con 8 dp de separación. El check visual puede ser de 20 dp, pero su área es de 48 dp.
- **Contraste:** AA en texto normal y en chips. Los pares de color de 1.2 están pensados para eso; si se cambia un tono, se mide de nuevo.
- **Escala de fuente:** probar 1.0, 1.3 y 2.0. Nada de alturas fijas para contenedores de texto; los chips y la pastilla de progreso pueden pasar a dos líneas.
- **TalkBack:** la tarjeta se anuncia como “Proyecto Diseño Web, 3 de 4 tareas hechas, 75 por ciento, Activo”; cada check como “Tarea Documentación, pendiente” / “hecha”; el menú como “Más Opciones De Diseño Web”.
- **Sin color como única información:** los chips tienen icono + texto; el estado no se distingue solo por verde/rojo.
- **Foco:** orden lógico encabezado → progreso → árbol → chips → acciones. El foco visible usa `accent`.
- **Gesto de atrás:** siempre funciona; no se intercepta. Las hojas se cierran con atrás.
- **Movimiento reducido:** ver 5.1.
- **Texto largo:** títulos a 2 líneas con elipsis; el detalle muestra completo. Nada de scroll horizontal.
- **Idioma:** español (es-AR) en el MVP. No hay selector de idioma; los textos no se concatenan a mano con fragmentos que impidan traducir después.

## 7. Datos honestos

- Un proyecto sin tareas dice “Sin tareas todavía”, no “0%”.
- Un proyecto con 3 de 4 dice “3 de 4” y “75%”; no se redondea a “Todo hecho”.
- Las listas no inflan el progreso del proyecto.
- Una tarea sin fecha no muestra “Hoy”.
- Un proyecto sin prioridad no muestra una prioridad inventada.
- No hay avatares, ni “compartido con”, ni “sincronizado”, ni “última conexión”: no existen en esta app.
- Si el export falla, se dice; no se muestra un snackbar verde de éxito.
- Si el import falla, no se toca nada y se explica por qué.

## 8. Checklist antes de dar una pantalla por terminada

- [ ] ¿Se ve igual de cuidada en claro, en oscuro y siguiendo al sistema?
- [ ] ¿La tarjeta se despega del fondo (sombra/borde/superficie), sin confundirse con él?
- [ ] ¿Figtree carga y los títulos están en SemiBold, no en Black?
- [ ] ¿Los rótulos usan Mayúscula Palabra Por Palabra y las frases hablan en segunda persona?
- [ ] ¿El progreso es honesto: incluye subproyectos no cancelados, excluye listas, no muestra 100% con pendientes y no inventa 0% cuando no hay tareas?
- [ ] ¿El árbol tiene conectores alineados y no pasa de 3 niveles de subproyectos?
- [ ] ¿Los chips tienen icono + texto y no dependen solo del color?
- [ ] ¿Los objetivos táctiles son de 48 dp y la pantalla funciona con fuente 1.3 y 2.0?
- [ ] ¿Los haptics están solo en acciones significativas?
- [ ] ¿Las animaciones duran menos de 300 ms, comunican un cambio de estado y respetan reduced motion?
- [ ] ¿Los estados vacíos son honestos y tienen una sola acción clara?
- [ ] ¿No hay avatares, filtros, calendario, login, nube ni nada fuera del alcance?
- [ ] ¿Un título largo, un emoji o un campo vacío no rompen el layout?

## 9. Decisiones abiertas

Las que el prompt no cubre y que hay que confirmar antes de la fase 1 están en `docs/decisiones/0003-modelo-de-datos.md`. Las principales:

- **Prioridad:** el modelo propuesto no la tiene y la tarjeta la pide. Se propone `Alta / Media / Baja` opcional; la referencia “Urgente” se mapea a Alta.
- **“Cerrar” un proyecto:** se propone que sea sinónimo de estado `Terminado`, no un archivo aparte.
- **Progreso con subproyectos vacíos:** se propone contar solo tareas; un subproyecto sin tareas no suma. Si no hay ninguna tarea en el árbol, “Sin tareas todavía”.
- **Borrar un proyecto con listas:** se propone que las listas sobrevivan y queden sueltas.
- **Profundidad:** raíz = nivel 0; subproyectos hasta nivel 3 (4 nodos en una cadena); el botón “Agregar Subproyecto” desaparece en el tercer nivel.
- **Export/import:** se propone reemplazo total, validado y atómico, con confirmación y opción de exportar antes.

Si alguna de estas no se aprueba, se cambia el documento y después el código. No se resuelve en silencio.
