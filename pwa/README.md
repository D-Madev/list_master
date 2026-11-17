# List Master PWA

List Master como aplicación web instalable en iPhone. Es un gemelo de la app Android:
proyectos con subproyectos y tareas, listas con sublistas, pendientes, prioridades,
fechas, temas claro/oscuro y export/import JSON compatible con Android.

No tiene backend, cuentas, analytics ni pedidos a servidores externos. El sitio solo
entrega archivos estáticos; los datos se guardan en el navegador del teléfono.

## Qué queda en el teléfono

- **IndexedDB** guarda proyectos, tareas y listas. Hay un espejo en `localStorage`
  como red de seguridad si IndexedDB falla.
- **localStorage** guarda el tema y las preferencias de vista.
- **No hay cuenta ni sincronización.** Cada instalación/dispositivo tiene sus datos.
- **Nadie del lado del hosting puede leer tus datos.** El servidor solo entrega HTML,
  CSS y JavaScript; no recibe el contenido de tus tareas.

El detalle completo está en [PRIVACY.md](PRIVACY.md).

## Instalar en iPhone

1. Abrí la URL de la PWA en **Safari** (no en Chrome ni en una app de mensajería).
2. Tocá **Compartir** y elegí **Agregar a pantalla de inicio**.
3. Abrí List Master desde el ícono nuevo.

> **Importante:** Safari en pestaña y la app instalada guardan datos por separado.
> Instalá primero y cargá los datos desde el ícono. Si ya cargaste algo en una pestaña,
> exportá ese JSON y luego importalo dentro de la app instalada.

### Compartir con otra persona

Pasale la URL y que la abra en Safari → **Compartir** → **Agregar a pantalla de inicio**.
Cada iPhone guarda sus propios datos: esa persona no ve tus proyectos ni vos los suyos.
No hay cuentas, invitaciones ni sincronización. Si querés que arranque con cierto
contenido, mandale un JSON exportado e indicá que lo importe desde **Ajustes**.

La URL no necesita login. Si el sitio está en GitHub Pages con repo privado, la app
igual queda accesible para quien tenga el enlace; lo privado es el repositorio, no el
sitio. Si necesitás restringir el acceso, usá Cloudflare Pages con Cloudflare Access.

La PWA necesita HTTPS para instalar el service worker. Cualquier hosting estático con
HTTPS sirve. Ver [DEPLOY.md](DEPLOY.md).

## Uso

- **Pendientes** es la pantalla de inicio: todas las tareas sin hacer, agrupadas por
  proyecto. Desde ahí se completan, editan, agregan y borran.
- **Proyectos**: subproyectos hasta tres niveles, estados y prioridad opcional, ícono
  propio o emoji. Se expanden en el lugar y hay filtros por estado y prioridad.
- **Listas**: listas y sublistas hasta tres niveles, vinculadas o no a un proyecto.
- **Ajustes**: tema claro/oscuro/sistema, exportar todo, importar JSON e instrucciones
  de instalación.

### Mover una tarea de proyecto

Abrí la tarea, tocá **Proyecto** y elegí otro. La tarea se mueve completa: no se
recrea ni pierde título, fecha, descripción, prioridad o estado.

### Diferencias con Android

La PWA agrega dos cosas que Android todavía no tiene:

- Cambiar el **proyecto padre** de un proyecto desde su formulario.
- Cambiar la **lista padre** de una lista desde su formulario.

Ambas operaciones validan ciclos y el límite de tres niveles.

## Respaldo JSON

**Ajustes → Exportar Todo** descarga `list-master-AAAA-MM-DD.json`. El formato es el
mismo de Android:

```json
{
  "version": 1,
  "aplicacion": "list_master",
  "exportado": "2026-11-16T12:00:00.000Z",
  "proyectos": [],
  "tareas": [],
  "listas": []
}
```

Los nombres de campo van en español (`titulo`, `proyecto_padre`, `proyecto_id`,
`lista_padre`, `hecha`, `orden`, …). Un export de Android se puede importar acá y
viceversa.

**Importar reemplaza todo** lo que tengas en ese momento; el diálogo lo confirma y
ofrece exportar primero. La importación se valida completa antes de aplicar nada.

Exportá seguido: si borrás la app, limpiás los datos del sitio o el sistema desaloja
almacenamiento, el JSON es tu único respaldo.

## Desarrollo local

No hay build ni dependencias. Alcanza con servir la carpeta:

```bash
cd pwa
python -m http.server 4173 --bind 127.0.0.1
```

Abrí <http://127.0.0.1:4173/>. El service worker solo se activa en `localhost` o HTTPS.

Pruebas del dominio y del formato de respaldo:

```bash
cd pwa
node --test
```

### Estructura

```
pwa/
  index.html            Shell, CSP, meta tags de iOS
  styles.css            Tokens de DESIGN.md, claro/oscuro, hojas y safe-area
  manifest.webmanifest  Instalación en pantalla de inicio
  sw.js                 Caché del shell para abrir sin conexión
  js/app.js             Estado, ruteo, hojas, acciones, import/export, tema
  js/views.js           Render de pantallas y componentes
  js/domain.js          Reglas puras de proyectos, tareas y listas
  js/backup.js          Formato JSON compatible con Android
  js/store.js           IndexedDB + espejo en localStorage
  js/icons.js           Íconos SVG inline y escape de HTML
  tests/                Pruebas con node --test
  tools/make_icons.py   Generación reproducible de íconos PNG
  fonts/                Figtree variable + licencia OFL
  icons/                Íconos de instalación
```

### Caché del service worker

El service worker sirve el shell con estrategia cache-first. Si editás CSS o JS y ves
cambios viejos, subí `CACHE_VERSION` en `pwa/sw.js` o, durante el desarrollo,
desregistrá el service worker y borrá las cachés desde DevTools.

## Publicación

El flujo completo —versión, tests, commit, push, APK y caché— está en el
[README raíz](../README.md#flujo-de-cada-cambio). Para la PWA, la parte crítica es subir
`CACHE_VERSION` en `sw.js` cada vez que cambie cualquier archivo del shell.

## Alcance

- Pensada para iPhone en orientación vertical; también funciona en escritorio.
- Los datos son por navegador/perfil. No hay sincronización entre dispositivos.
- Sin notificaciones push, sin segundo plano y sin acceso a contactos, cámara o
  ubicación.
- Si el sistema borra el almacenamiento del sitio, hace falta restaurar desde un JSON.

## Versión

La PWA acompaña la versión de Android: **1.8.0**.
