# List Master

Gestor de proyectos y listas para el celular: proyectos con subproyectos y tareas, y listas simples, en un solo lugar.

Es una app Android propia. Los datos quedan en el celular, sin cuenta ni servidor. Más adelante se integra con A.R.G.O.S, el sistema mayor del segundo cerebro.

## Estado

Fase 1, corte 1: esqueleto compilable e instalable, con tema claro/oscuro/sistema y navegación base entre Proyectos, Listas y Ajustes. Todavía no hay pantallas de producto.

## Cómo compilar

No hace falta Android Studio: alcanza con el JDK 17, Gradle 8.9 y el SDK de Android que ya están en esta PC.

```bash
JAVA_HOME=/d/src/tools/jdk-17.0.20.1+1 \
/d/src/tools/gradle-8.9/bin/gradle.bat assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk` y se instala con:

```bash
/d/src/tools/android-sdk/platform-tools/adb.exe install -r app/build/outputs/apk/debug/app-debug.apk
```

Los tests corren con:

```bash
JAVA_HOME=/d/src/tools/jdk-17.0.20.1+1 \
/d/src/tools/gradle-8.9/bin/gradle.bat testDebugUnitTest
```

## Documentación

- [Alcance y decisiones](docs/decisiones/0000-alcance.md): qué entra, qué no, y qué está abierto.
- [Plataforma y stack](docs/decisiones/0001-plataforma-y-stack.md): comparación de Kotlin + Compose, React Native con Expo y Flutter, con recomendación.
- [Metodología](docs/decisiones/0002-metodologia.md): cortes verticales, tests y commits.
- [Modelo de datos](docs/decisiones/0003-modelo-de-datos.md): entidades, progreso, export/import y decisiones abiertas.
- [Diseño](DESIGN.md): identidad visual, componentes, pantallas y criterios de UX/UI.
- [Prompt de inicio](docs/prompt-inicial.md): el prompt con el que arranca cada sesión de trabajo.
- [Tarjeta de progreso](docs/referencias/tarjeta-progreso.png): referencia visual de la tarjeta de proyecto.
- [Historial](docs/historial/propuesta-inicial-ia.md): la propuesta inicial de IA, sin revisar. No es el plan vigente.
