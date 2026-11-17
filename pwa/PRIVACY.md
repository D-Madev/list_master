# Privacidad

List Master PWA está diseñada para que tus proyectos, tareas y listas no salgan de tu
teléfono.

## Resumen

- No hay cuentas, registro ni inicio de sesión.
- No hay backend que reciba tus datos.
- No hay analytics, publicidad, rastreadores ni píxeles.
- No se cargan fuentes, scripts ni íconos desde CDN: todo viene del mismo sitio.
- No se pide acceso a contactos, cámara, micrófono, ubicación, archivos ni
  notificaciones.
- No hay sincronización entre dispositivos.

## Qué se guarda y dónde

| Dato | Dónde | Para qué |
| --- | --- | --- |
| Proyectos, tareas y listas | IndexedDB del navegador | Contenido de la app |
| Copia de seguridad de esos datos | `localStorage` | Red de seguridad si IndexedDB falla |
| Tema, filtros e interruptores | `localStorage` | Recordar preferencias |
| Shell de la app (HTML, CSS, JS, íconos) | Cache Storage del service worker | Abrir sin conexión |

Todo eso queda en el almacenamiento local del navegador o de la app instalada. No se
envía por la red.

## Qué ve el hosting

El sitio es estático. El servidor solo entrega archivos y, como cualquier servidor web,
puede registrar metadatos técnicos de la conexión: dirección IP, fecha/hora, URL
solicitada y user agent. Esos registros no incluyen el contenido de tus tareas, porque
el contenido nunca se transmite.

Si usás Cloudflare, Netlify o un servidor propio, revisá su política de logs. La app no
agrega ningún pedido adicional después de cargar.

## Qué no hace la app

- No envía datos a A.R.G.O.S ni a ningún otro sistema.
- No usa cookies de seguimiento.
- No incluye bibliotecas de terceros que hagan pedidos de red.
- No abre enlaces externos automáticamente.
- No tiene telemetría de errores ni de uso.

La política de seguridad del sitio (`Content-Security-Policy`) limita scripts, estilos,
imágenes, fuentes y conexiones al propio origen. Aunque hubiera un error, la app no
puede hablar con dominios externos.

## Respaldos

**Ajustes → Exportar Todo** genera un archivo JSON en tu dispositivo. Ese archivo
contiene tus datos en texto plano y queda donde vos lo guardes (Archivos, iCloud Drive,
correo, etc.). A partir de ahí, su privacidad depende de dónde lo guardes.

**Importar JSON** lee un archivo que elijas vos. No se sube a ningún lado.

## Pérdida de datos

Los datos viven en un solo lugar. Se pueden perder si:

- borrás la app o los datos del sitio,
- usás “Borrar historial y datos de sitios” en Safari,
- el sistema desaloja almacenamiento por falta de espacio,
- cambiás de dispositivo o de navegador.

Por eso conviene exportar un JSON cada tanto y guardarlo fuera del teléfono. La app no
puede recuperar datos que el sistema borró.

## Pestaña de Safari vs. app instalada

Safari en una pestaña y la PWA instalada en la pantalla de inicio tienen
almacenamientos separados. Los datos cargados en una pestaña no aparecen en el ícono.
Para moverlos, exportá el JSON desde un lado e importalo en el otro.

## Menores y contenido

La app no recopila datos personales, así que no hay datos de menores que proteger del
lado del servicio. El contenido que escribas es tuyo y queda en tu dispositivo.

## Cambios

Si en el futuro la app incorpora sincronización o cualquier función que implique enviar
datos, esta política y la app van a decirlo de forma explícita antes de activarla. La
versión actual es **1.8.0** y es completamente local.
