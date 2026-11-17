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

> **Antes de elegir esta opción:** GitHub Pages solo publica desde repositorios
> privados en planes **Pro, Team o Enterprise**. En el plan Free, el repositorio tiene
> que ser público. Además, en Pro/Team el sitio publicado **es público**: cualquiera con
> la URL puede abrir la app. Eso no expone tus tareas (viven en cada iPhone), pero sí el
> código y la existencia del sitio. Solo GitHub Enterprise Cloud permite un Pages
> privado con control de acceso.

El repositorio ya incluye el workflow [`.github/workflows/pages.yml`](../.github/workflows/pages.yml).
Pasos:

1. **Settings → Pages → Build and deployment → Source: GitHub Actions**.
2. Hacé push a `main` o corré el workflow **Publicar PWA en GitHub Pages** desde
   **Actions**.
3. La URL queda como `https://<usuario>.github.io/list_master/`.

El workflow publica únicamente `pwa/` como artefacto, sin build. La app usa rutas
relativas (`./`, `sw.js`, manifest con `scope: "./"`), así que funciona bien en ese
subdirectorio.

Limitaciones de GitHub Pages:

- No lee `_headers`: se pierden `X-Frame-Options`, `Permissions-Policy`,
  `X-Robots-Tag` y los `Cache-Control` por archivo. La CSP del `index.html`, el
  `referrer` y el `robots` meta siguen vigentes.
- Los archivos se sirven con caché de unos 10 minutos. Después de un deploy, subí
  `CACHE_VERSION` en `pwa/sw.js` y esperá a que GitHub publique.
- GitHub registra la IP de cada visita en sus logs de seguridad (igual que cualquier
  hosting).

GitHub Pages **no** permite publicar desde la rama usando una subcarpeta distinta de
`/` o `/docs`; por eso el workflow sube `pwa/` como artefacto en lugar de usar
“Deploy from a branch”.

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
   - **Cache Storage**: `list-master-shell-1.9.0-r1`.
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
