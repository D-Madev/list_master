# Desplegar la PWA

La app es estática: no hay build, variables de entorno ni backend. Cualquier hosting
que sirva archivos con HTTPS funciona. Estos son los caminos recomendados.

Requisitos:

- HTTPS (obligatorio para instalar el service worker y para “Agregar a pantalla de
  inicio” con comportamiento de app).
- Publicar el contenido de `pwa/` como raíz del sitio.
- Mantener el repositorio privado si no querés que el código sea público. El hosting
  nunca recibe datos de uso.

---

## Opción A: Cloudflare Pages (recomendada)

Cloudflare Pages soporta repositorios privados y da HTTPS y CDN sin costo para este
tamaño.

### Desde el panel

1. Entrá a <https://dash.cloudflare.com/> → **Workers & Pages** → **Create** →
   **Pages** → **Connect to Git**.
2. Autorizá GitHub y elegí el repositorio `D-Madev/list_master`.
3. Configurá:
   - **Project name:** `list-master`
   - **Production branch:** `main`
   - **Framework preset:** `None`
   - **Build command:** *(vacío)*
   - **Build output directory:** `pwa`
4. **Save and Deploy**. La URL queda como
   `https://list-master.pages.dev`.
5. Abrila en Safari del iPhone y agregala a la pantalla de inicio.

Cada push a `main` vuelve a publicar automáticamente. Los pull requests generan
deploys de vista previa.

### Desde la terminal (Wrangler)

```bash
npx wrangler pages project create list-master --production-branch main
npx wrangler pages deploy pwa --project-name list-master
```

Wrangler pide login la primera vez. No hace falta build.

### Encabezados

Cloudflare Pages lee `pwa/_headers` y aplica CSP, `X-Frame-Options`, `noindex`,
`Permissions-Policy` y `Cache-Control: no-cache` para `index.html`, `sw.js` y el
manifest. No hay que configurar nada extra.

---

## Opción B: Netlify Drop (la más rápida)

1. Entrá a <https://app.netlify.com/drop>.
2. Arrastrá la carpeta `pwa/` completa.
3. Netlify devuelve una URL HTTPS al instante.
4. Opcional: creá una cuenta para que el sitio no expire y para conectar el repo.

Netlify también respeta `pwa/_headers`. Para deploys automáticos, conectá el
repositorio con:

- **Build command:** *(vacío)*
- **Publish directory:** `pwa`

---

## Opción C: GitHub Pages

GitHub Pages en repositorios privados requiere un plan pago. Si ya lo tenés:

1. **Settings → Pages → Build and deployment → Source: GitHub Actions**.
2. Publicá el contenido de `pwa/` como artefacto. Un workflow mínimo:

```yaml
name: Deploy PWA
on:
  push:
    branches: [main]
  workflow_dispatch:
permissions:
  contents: read
  pages: write
  id-token: write
concurrency:
  group: pages
  cancel-in-progress: true
jobs:
  deploy:
    runs-on: ubuntu-latest
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/upload-pages-artifact@v3
        with:
          path: pwa
      - id: deployment
        uses: actions/deploy-pages@v4
```

GitHub Pages **no** lee `_headers`; la CSP del `index.html` sigue protegiendo la app,
pero los encabezados HTTP extra no se aplican. Si te importa, usá Cloudflare o Netlify.

---

## Opción D: cualquier servidor propio

Serví `pwa/` con Nginx, Caddy, Apache o similar. Ejemplo de Nginx:

```nginx
server {
    listen 443 ssl;
    server_name list-master.example.com;
    root /var/www/list-master;
    index index.html;

    add_header X-Content-Type-Options nosniff always;
    add_header X-Frame-Options DENY always;
    add_header Referrer-Policy no-referrer always;
    add_header X-Robots-Tag "noindex, nofollow, noarchive" always;

    location = /index.html { add_header Cache-Control "no-cache"; }
    location = /sw.js      { add_header Cache-Control "no-cache"; }
    location = /manifest.webmanifest { add_header Cache-Control "no-cache"; }

    location / { try_files $uri $uri/ /index.html; }
}
```

Caddy equivalente:

```
list-master.example.com {
    root * /var/www/list-master
    file_server
    try_files {path} {path}/ /index.html
    header {
        X-Content-Type-Options nosniff
        X-Frame-Options DENY
        Referrer-Policy no-referrer
        X-Robots-Tag "noindex, nofollow, noarchive"
    }
    @nocache path /index.html /sw.js /manifest.webmanifest
    header @nocache Cache-Control "no-cache"
}
```

---

## Verificar el deploy

1. Abrí la URL en una ventana privada de escritorio.
2. DevTools → **Application**:
   - **Manifest**: nombre, íconos y `start_url` correctos.
   - **Service Workers**: estado `activated`.
   - **Cache Storage**: `list-master-shell-1.8.0-r2`.
3. Sin conexión (DevTools → Network → Offline) recargá: la app debe abrir.
4. En iPhone: Safari → Compartir → **Agregar a pantalla de inicio**.
5. Abrí desde el ícono: sin barra de Safari, con el tema y los datos propios del ícono.
6. Exportá un JSON y volvé a importarlo para confirmar el ciclo completo.

---

## Actualizar

1. Hacé cambios en `pwa/`.
2. Si tocaste CSS, JS o íconos, subí `CACHE_VERSION` en `pwa/sw.js` para invalidar el
   shell cacheado.
3. Corré las pruebas y publicá:

```bash
cd pwa
node --test
git add pwa
git commit -m "Actualiza la PWA"
git push
```

En Cloudflare/Netlify el push dispara el deploy. En Netlify Drop hay que volver a
arrastrar la carpeta.

## Rollback

- **Cloudflare Pages / Netlify con Git:** elegí un deploy anterior en el panel y
  promovelo a producción.
- **Netlify Drop:** volvé a arrastrar una copia anterior de `pwa/`.

Los datos de los usuarios viven en el teléfono, no en el hosting, así que un rollback
de la app no toca sus tareas.
