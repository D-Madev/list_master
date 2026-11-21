# 0004: Sesión Web desde el celular

- **Estado:** aprobada e implementada.
- **Fecha:** 20 de noviembre de 2026.
- **Relación:** extiende `0001-plataforma-y-stack.md` y `0003-modelo-de-datos.md` sin cambiar el modelo de datos. No introduce A.R.G.O.S. ni un backend.

## El problema

La PWA es un gemelo de la app Android, pero cada instalación guarda sus propios datos: la del iPhone y la de la computadora no se ven entre sí. Pasar un proyecto a la computadora obligaba a exportar y volver a importar un JSON cada vez.

Se pidió una forma de abrir la app en el navegador y trabajar sobre los mismos datos que el celular, en los dos sentidos, sin montar una base de datos hosteada ni una cuenta.

## La decisión

**El celular sirve la PWA y una API mínima dentro de la red local, por tiempo limitado.** Desde **Ajustes → Sesión Web** se elige una duración (15 min a 2 h) y la app muestra un enlace y un código de seis números. Al abrirlo en el navegador de la computadora —misma red Wi-Fi—, la PWA arranca en **modo remoto**: en vez de IndexedDB, lee y escribe contra el celular.

El celular sigue siendo la única base. No hay servidor en internet, no se sube nada a la nube y al cerrar la sesión no queda copia del lado de la computadora.

## Cómo funciona

### Servidor

`data/remote/RemoteHttpServer.kt` abre un `ServerSocket` en el puerto 8080 (o uno libre si está ocupado) y atiende cada conexión en una corrutina de IO.

- Sirve los archivos de la PWA desde los assets del APK. Se copian al compilar desde `pwa/` (`syncPwaAssets` en `app/build.gradle.kts`), así no hay dos versiones distintas del cliente web.
- Al servir `index.html` inyecta `<script type="module" src="/js/remote.js">`. La PWA estática de GitHub Pages nunca lo carga: el modo remoto existe solo cuando la página la sirve el celular.
- La CSP es `script-src 'self'`, por eso el modo remoto entra como archivo mismo-origen y no como script inline.

### API

| Ruta | Qué hace |
|---|---|
| `GET /api/session?code=...` | Verifica que el código siga activo |
| `GET /api/state?code=...` | Devuelve `{ version, datos }`, donde `datos` es el mismo JSON del respaldo |
| `POST /api/state?code=...` | Recibe `{ version, datos }`, valida y reemplaza todo |

Todas las rutas exigen el código de la sesión; sin él responden `403`. El JSON de datos es el del `BackupCodec`: la sesión web no inventa un formato nuevo ni un mapeo paralelo.

### Sincronización

Cada sesión lleva un contador de versión en memoria (`AtomicLong`).

- El navegador guarda con la versión que leyó. Si el celular cambió mientras tanto, el `POST` responde `409` y el navegador vuelve a leer en lugar de pisar cambios.
- El navegador consulta `/api/state` cada 2 segundos. Si la versión cambió, vuelve a pintar.
- Cuando el celular cambia algo, `AppViewModel` avisa a la sesión y esta sube la versión; el navegador se entera en el siguiente sondeo.
- Cuando la escritura viene del navegador, la app descuenta un aviso pendiente (`pendingEcho`) para no rebotar su propio cambio como si fuera local.

No hay merge por campo: el que llega segundo con una versión vieja relee. Para una app personal, con un solo celular y una sola computadora, alcanza y evita conflictos silenciosos.

### Seguridad y alcance

- La sesión escucha solo en la red local y vive mientras el proceso de la app esté vivo y no venza la duración elegida.
- El código de seis números se muestra en el celular y se puede copiar con el enlace.
- No hay HTTPS: es HTTP dentro de la LAN. El código evita que alguien que esté en la misma red entre por casualidad, no protege de un atacante en la red. Es una sesión corta y explícita, no un servicio expuesto.
- Como el origen no es seguro, el navegador no registra el service worker en modo remoto. Es intencional: no tiene sentido cachear una sesión temporal y confundirla con la PWA local.

## Alternativas descartadas

| Alternativa | Por qué no |
|---|---|
| Base de datos hosteada + cuenta | Contradice el local-first y agrega infraestructura, privacidad y costo |
| Túnel a internet (ngrok o similar) | Expone el celular fuera de la LAN y depende de un tercero |
| WebRTC o WebSocket entre pares | Más complejo para el mismo resultado y más piezas que mantener |
| Sincronización por archivo | Es lo que ya había: exportar e importar a mano |

## Límites conocidos

- El celular y la computadora tienen que estar en la misma red.
- La sesión se corta si Android mata el proceso de la app; no hay servicio en primer plano.
- El puerto 8080 puede estar ocupado; en ese caso se elige uno libre y el enlace muestra el puerto real.
- El modo remoto no funciona sin el celular: si se cae la conexión, la PWA muestra “Sin conexión con el celular” y reintenta.

## Archivos

- `app/src/main/java/ar/matias/listmaster/data/remote/RemoteHttpServer.kt`: servidor y API.
- `app/src/main/java/ar/matias/listmaster/data/remote/WebSession.kt`: duración, código, vencimiento y versión.
- `app/src/main/java/ar/matias/listmaster/ui/screens/SettingsScreen.kt`: bloque de la sesión.
- `app/src/main/java/ar/matias/listmaster/ui/AppViewModel.kt`: arranque, cierre y aviso de cambios locales.
- `pwa/js/remote.js`: puente del navegador, puerta de código y sondeo.
- `pwa/js/store.js`: `loadData`/`saveData` delegan en `LM_REMOTE` cuando existe.
- `pwa/js/app.js`: escucha los cambios remotos y evita registrar el service worker.
