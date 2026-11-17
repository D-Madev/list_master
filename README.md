# List Master

Gestor de proyectos y listas para el celular: proyectos con subproyectos y tareas, y listas simples, en un solo lugar.

Es una app Android propia. Los datos quedan en el celular, sin cuenta ni servidor. Más adelante se integra con A.R.G.O.S, el sistema mayor del segundo cerebro.

## Estado

**Versión 1.8.0.** La app está completa según el alcance acordado:

- Proyectos con subproyectos hasta tres niveles, estados y prioridad opcional, con ícono propio o emoji.
- Listado de proyectos y listas en registros compactos que se despliegan en el lugar, con “Expandir Todo” / “Colapsar Todo”.
- Tareas con fecha opcional, descripción y prioridad (solo se ven y editan al abrir la tarea); dentro de cada proyecto y en Pendientes las más importantes van arriba.
- Una tarea se puede mover a otro proyecto desde su detalle, sin perder título, fecha, descripción, prioridad ni estado.
- Pendientes es la pantalla de inicio.
- Pestaña **Pendientes**: todas las tareas sin hacer en una sola lista, agrupadas por proyecto y subproyecto; desde ahí se completan, editan, agregan (con “+” por proyecto, una tras otra) y borran.
- Dentro de un proyecto, las pendientes van juntas y las hechas se ocultan con un interruptor (queda guardado).
- Proyectos ordenados solos por prioridad (Alta, Media, Baja, sin prioridad) y filtro por estado y prioridad que queda guardado.
- Los subproyectos se pueden pasar a otro padre, siempre dentro de su mismo proyecto principal.
- Listas simples, vinculadas o no a un proyecto; las tareas hechas se ocultan con un interruptor y la vista previa muestra primero las pendientes.
- Progreso calculado sobre el árbol de proyectos, sin contar listas.
- Export e import JSON con nombres de campo en español, validado y atómico.
- Tema claro, oscuro o del sistema, guardado en el celular.
- Base monocroma (blanco, negro y grises) con color semántico en prioridad, estado y progreso; detalle fiel a la tarjeta de referencia.
- Sin cuenta, sin nube, sin analytics, sin permisos de red ni de almacenamiento.

El APK listo para instalar se genera en la raíz del proyecto como **`ListMaster.apk`**. No se versiona: `.gitignore` excluye `*.apk`. Si alguna vez hace falta adjuntarlo al repo, se agrega a mano con `git add -f ListMaster.apk`.

El archivo **`list-master-import.json`** es un respaldo listo para importar: trae los proyectos en marcha del vault, el recordatorio personal y la lista del MCU en orden cronológico. Se importa desde **Ajustes → Importar JSON** y reemplaza todo el contenido actual.

## Stack

| Parte | Tecnología |
| --- | --- |
| Android | Kotlin 2.0.21, Jetpack Compose (BOM 2024.09.03), Material 3, Room 2.6.1 con KSP, Gson 2.10.1, minSdk 28, target/compileSdk 34, JDK 17, Gradle 8.9, AGP 8.5.2 |
| PWA | HTML + CSS + JavaScript ES modules, sin framework ni build. IndexedDB con espejo en `localStorage`, service worker cache-first, Web App Manifest y Figtree variable (OFL) |
| Pruebas | JUnit 4 + Robolectric + `kotlinx-coroutines-test` en Android; `node:test` en la PWA |
| Publicación | APK Android firmado con la clave de debug; PWA estática en GitHub Pages mediante `.github/workflows/pages.yml` |

No hay backend, cuentas, analytics ni CDN. La PWA no tiene dependencias: se sirve tal como está.

## Cómo está pensada

**Una app, dos clientes.** Android es el cliente nativo y `pwa/` es un gemelo web para iPhone. Comparten el mismo modelo de datos, las mismas reglas y el mismo formato de respaldo JSON, así que un export de Android se importa en la PWA y viceversa.

**Local-first.** No hay servidor que reciba datos. Android guarda en Room; la PWA en IndexedDB con espejo en `localStorage`. El hosting solo entrega archivos estáticos. La privacidad depende de que el dato nunca salga del dispositivo, no de una cuenta.

**Separación por capas.** En Android: dominio puro (`domain/`), persistencia (`data/`, Room), UI (`ui/`, Compose) y respaldo (`BackupCodec`). En la PWA: `domain.js` (reglas puras), `store.js` (persistencia), `views.js` + `app.js` (UI y acciones) y `backup.js` (JSON). Las reglas importantes —progreso, árboles, ciclos, profundidad, validación— viven en el dominio y se prueban sin UI.

**Decisiones que no se negocian:**

- El progreso se calcula, nunca se guarda.
- Proyectos y listas anidan hasta tres niveles; se validan ciclos y profundidad.
- Los IDs son UUID estables, también en el respaldo.
- Importar valida todo antes de reemplazar: es atómico.
- Los datos son personales: sin cuenta, sin nube, sin analytics.
- La UI usa una base monocroma con color semántico en prioridad, estado y progreso; Figtree; tema claro, oscuro o del sistema.
- En iPhone, la pestaña de Safari y la app instalada tienen almacenamientos separados; el JSON es el puente entre ambos.

**Flujo de uso.** Pendientes es la pantalla de inicio. Los proyectos y las listas se muestran como filas compactas que se expanden en el lugar. Las tareas se editan en una hoja, y desde ahí se pueden mover de proyecto sin recrearlas. Las listas no suman al progreso del proyecto.

## Cómo instalar

Copiá `ListMaster.apk` al celular y abrilo. Android va a pedir permiso para instalar apps de esa fuente; después se instala como cualquier APK. No requiere Android Studio ni conexión.

También se puede instalar por cable con `adb`:

```bash
/d/src/tools/android-sdk/platform-tools/adb.exe install -r ListMaster.apk
```

## Versión web (iPhone)

En `pwa/` hay un gemelo web instalable en iPhone, con las mismas funciones y el mismo
formato de respaldo JSON. No tiene backend ni cuentas: los datos quedan en el navegador
del teléfono. Se instala desde Safari con **Compartir → Agregar a pantalla de inicio**.

- [Cómo usar e instalar la PWA](pwa/README.md)
- [Cómo desplegarla](pwa/DEPLOY.md)
- [Privacidad](pwa/PRIVACY.md)

## Cómo compilar

No hace falta Android Studio: alcanza con el JDK 17, Gradle 8.9 y el SDK de Android que ya están en esta PC.

```bash
JAVA_HOME=/d/src/tools/jdk-17.0.20.1+1 \
/d/src/tools/gradle-8.9/bin/gradle.bat assembleDebug
```

El APK de debug queda en `app/build/outputs/apk/debug/app-debug.apk`.

Para generar el APK de release (firmado con la clave de debug, porque la app no va a Play Store):

```bash
JAVA_HOME=/d/src/tools/jdk-17.0.20.1+1 \
/d/src/tools/gradle-8.9/bin/gradle.bat assembleRelease
```

El resultado queda en `app/build/outputs/apk/release/app-release.apk`.

Después copialo a la raíz como `ListMaster.apk`, que es el archivo que se instala en el celular (no se versiona; ver [Flujo de cada cambio](#flujo-de-cada-cambio)).

## Tests

```bash
JAVA_HOME=/d/src/tools/jdk-17.0.20.1+1 \
/d/src/tools/gradle-8.9/bin/gradle.bat testDebugUnitTest
```

Cubren el cálculo de progreso, las reglas del árbol, la validación del export/import (incluidos archivos dañados, ciclos, profundidad e ids repetidos) y un smoke test de la navegación con Robolectric.

La PWA tiene su propia suite, sin dependencias:

```bash
cd pwa
node --test
```

Cubre las mismas reglas de dominio y la compatibilidad del JSON con Android.

## Respaldo

El único respaldo es el export manual desde **Ajustes → Exportar Todo**. Genera un JSON con proyectos, tareas y listas. **Importar JSON** reemplaza todo el contenido después de validar el archivo; la app pide confirmación y ofrece exportar primero.

El respaldo automático de Android está desactivado (`allowBackup="false"`) para que los datos no salgan del celular.

## Flujo de cada cambio

Regla: **un cambio terminado se versiona, se prueba, se commitea, se pushea y se verifica en el destino**. No hay commits “para guardar” ni versiones a medias.

### 1. Subir la versión

| Qué cambió | Dónde | Qué hacer |
| --- | --- | --- |
| Android | `app/build.gradle.kts` | `versionCode` +1 y `versionName` nuevo |
| PWA, cambio de producto | `pwa/js/app.js` y `pwa/package.json` | Misma `APP_VERSION` / `version` que Android |
| Shell de la PWA | `pwa/sw.js` | Subir `CACHE_VERSION` (por ejemplo `1.8.1-r1`) |
| Solo documentación | — | No se toca la versión |

Reglas prácticas:

- Si el cambio afecta el producto en ambos clientes, Android y PWA comparten la misma versión.
- Si es un arreglo **solo de la PWA** (CSS, caché, un texto), se mantiene `APP_VERSION` y se sube `CACHE_VERSION`. No hace falta volver a compilar el APK.
- Si es **solo Android**, se sube la versión de Android y se vuelve a compilar el APK; la PWA no cambia.
- Cualquier cambio en `styles.css`, `js/*.js`, íconos o fuentes de la PWA **obliga** a subir `CACHE_VERSION`, o el service worker sigue sirviendo la versión vieja.

### 2. Probar

Ejecutá solo lo que corresponda a la parte que tocaste:

```bash
# Android: tests y APK de release
JAVA_HOME=/d/src/tools/jdk-17.0.20.1+1 \
/d/src/tools/gradle-8.9/bin/gradle.bat testDebugUnitTest

JAVA_HOME=/d/src/tools/jdk-17.0.20.1+1 \
/d/src/tools/gradle-8.9/bin/gradle.bat assembleRelease

# Copiar el APK a la raíz (no se versiona)
cp app/build/outputs/apk/release/app-release.apk ListMaster.apk
```

```bash
# PWA: tests y chequeo de sintaxis
cd pwa
node --test
for f in js/*.js sw.js; do node --check "$f" || exit 1; done
```

Antes de commitear, probar a mano lo que se tocó:

- Android: claro, oscuro, seguir al sistema, escala de fuente y un flujo real en el celular o emulador.
- PWA: abrir en Safari, instalar en pantalla de inicio, probar sin conexión y hacer un round-trip de export/import.
- Si el cambio toca datos o respaldo, probar también con un JSON de Android.

### 3. Commit y push

- Un commit por cambio terminado, en español y en imperativo: `Sube la version a 1.8.1: ...`.
- El commit incluye código, tests, versión y documentación en el mismo corte.
- Nunca se commitea `ListMaster.apk` (está ignorado); el APK se regenera desde el código.

```bash
git add -A
git commit -m "Sube la version a 1.8.1: describe el cambio"
git push origin main
```

### 4. Publicar y verificar

- **PWA:** el push a `main` dispara `.github/workflows/pages.yml` y publica `pwa/` en
  <https://d-madev.github.io/list_master/>. Verificar en **Actions** que el deploy terminó
  en verde y abrir la URL en una ventana privada.
- **Android:** el APK queda en `ListMaster.apk`; se copia al celular e instala con
  `adb install -r ListMaster.apk` o abriéndolo desde el teléfono.
- **Caché de la PWA:** si después del deploy se ve una versión vieja, confirmar que
  `CACHE_VERSION` cambió, recargar sin caché y, en el iPhone, cerrar y volver a abrir la
  app instalada. GitHub Pages tarda unos minutos en propagar; no alcanza con recargar
  una sola vez.

### 5. Cerrar

El cambio está terminado cuando:

- [ ] Los tests de la parte tocada pasan.
- [ ] La versión correcta quedó actualizada.
- [ ] El APK se regeneró si cambió Android.
- [ ] `CACHE_VERSION` subió si cambió el shell de la PWA.
- [ ] Se probó a mano en claro y oscuro.
- [ ] El commit está pusheado a `main`.
- [ ] El deploy de la PWA está en verde y la URL abre.
- [ ] Si hubo una decisión nueva, quedó anotada en `docs/decisiones/`.

## Documentación

- [Alcance y decisiones](docs/decisiones/0000-alcance.md): qué entra, qué no, y qué está abierto.
- [Plataforma y stack](docs/decisiones/0001-plataforma-y-stack.md): la tecnología elegida y por qué.
- [Metodología](docs/decisiones/0002-metodologia.md): cortes verticales, tests y commits.
- [Modelo de datos](docs/decisiones/0003-modelo-de-datos.md): entidades, progreso, export/import y decisiones adoptadas.
- [Diseño](DESIGN.md): identidad visual, componentes, pantallas y criterios de UX/UI.
- [Prompt de inicio](docs/prompt-inicial.md): el prompt con el que arranca cada sesión de trabajo.
- [Tarjeta de progreso](docs/referencias/tarjeta-progreso.png): referencia visual de la tarjeta de proyecto.
- [Historial](docs/historial/propuesta-inicial-ia.md): la propuesta inicial de IA, sin revisar. No es el plan vigente.
