# List Master

Gestor de proyectos y listas para el celular: proyectos con subproyectos y tareas, y listas simples, en un solo lugar.

Es una app Android propia. Los datos quedan en el celular, sin cuenta ni servidor. Más adelante se integra con A.R.G.O.S, el sistema mayor del segundo cerebro.

## Estado

**Versión 1.4.0.** La app está completa según el alcance acordado:

- Proyectos con subproyectos hasta tres niveles, estados y prioridad opcional, con ícono propio o emoji.
- Listado de proyectos y listas en registros compactos que se despliegan en el lugar, con “Expandir Todo” / “Colapsar Todo”.
- Tareas con fecha opcional, descripción y prioridad (solo se ven y editan al abrir la tarea); dentro de cada proyecto y en Pendientes las más importantes van arriba.
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

El APK listo para instalar está en la raíz del repositorio: **`ListMaster.apk`**.

El archivo **`list-master-import.json`** es un respaldo listo para importar: trae los proyectos en marcha del vault, el recordatorio personal y la lista del MCU en orden cronológico. Se importa desde **Ajustes → Importar JSON** y reemplaza todo el contenido actual.

## Cómo instalar

Copiá `ListMaster.apk` al celular y abrilo. Android va a pedir permiso para instalar apps de esa fuente; después se instala como cualquier APK. No requiere Android Studio ni conexión.

También se puede instalar por cable con `adb`:

```bash
/d/src/tools/android-sdk/platform-tools/adb.exe install -r ListMaster.apk
```

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

## Tests

```bash
JAVA_HOME=/d/src/tools/jdk-17.0.20.1+1 \
/d/src/tools/gradle-8.9/bin/gradle.bat testDebugUnitTest
```

Cubren el cálculo de progreso, las reglas del árbol, la validación del export/import (incluidos archivos dañados, ciclos, profundidad e ids repetidos) y un smoke test de la navegación con Robolectric.

## Respaldo

El único respaldo es el export manual desde **Ajustes → Exportar Todo**. Genera un JSON con proyectos, tareas y listas. **Importar JSON** reemplaza todo el contenido después de validar el archivo; la app pide confirmación y ofrece exportar primero.

El respaldo automático de Android está desactivado (`allowBackup="false"`) para que los datos no salgan del celular.

## Documentación

- [Alcance y decisiones](docs/decisiones/0000-alcance.md): qué entra, qué no, y qué está abierto.
- [Plataforma y stack](docs/decisiones/0001-plataforma-y-stack.md): la tecnología elegida y por qué.
- [Metodología](docs/decisiones/0002-metodologia.md): cortes verticales, tests y commits.
- [Modelo de datos](docs/decisiones/0003-modelo-de-datos.md): entidades, progreso, export/import y decisiones adoptadas.
- [Diseño](DESIGN.md): identidad visual, componentes, pantallas y criterios de UX/UI.
- [Prompt de inicio](docs/prompt-inicial.md): el prompt con el que arranca cada sesión de trabajo.
- [Tarjeta de progreso](docs/referencias/tarjeta-progreso.png): referencia visual de la tarjeta de proyecto.
- [Historial](docs/historial/propuesta-inicial-ia.md): la propuesta inicial de IA, sin revisar. No es el plan vigente.
