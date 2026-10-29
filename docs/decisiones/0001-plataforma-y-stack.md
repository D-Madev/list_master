# 0001: Plataforma y stack

- **Estado:** aprobada e implementada.
- **Fecha:** 29 de octubre de 2026.
- **Decide:** con qué tecnología se construye List Master.
- **No decide todavía:** nombre del paquete, versión mínima definitiva ni detalles de implementación.

## Contexto

List Master es una app **solo para Android**, personal, sin cuenta, sin servidor y sin analytics. Tiene que funcionar sin conexión, guardar todo en el celular y exportar/importar JSON con IDs estables. La UI de referencia es la tarjeta de `docs/referencias/tarjeta-progreso.png`: tipografía Figtree, profundidad suave, chips, árbol con conectores y progreso visible.

La app de sueño (`D:\src\2b\registro_sueno`) ya resolvió un problema parecido con **Kotlin + Jetpack Compose + Material 3 + Room/KSP**. No se trata de copiar su código: se trata de compartir criterio visual, herramientas y forma de compilar.

### Lo que ya está en esta PC (no hay que descargar)

Verificado el 29/10/2026:

| Recurso | Dónde | Estado |
|---|---|---|
| JDK 17 (Temurin 17.0.20.1+1) | `D:\src\tools\jdk-17.0.20.1+1` | Instalado |
| Gradle 8.9 | `D:\src\tools\gradle-8.9\bin\gradle.bat` | Instalado; arranca con el JDK 17 |
| Android SDK | `D:\src\tools\android-sdk` | `platforms/android-34`, `build-tools/34.0.0`, `platform-tools` |
| AGP 8.5.2 | caché de Gradle | Presente |
| Kotlin 2.0.21 + KSP 2.0.21-1.0.28 | caché de Gradle | Presentes |
| Compose BOM 2024.09.03 | caché de Gradle | Presente |
| Room 2.6.1 | caché de Gradle | Presente |
| Figtree variable (400–900) | `registro_sueno/app/src/main/res/font/figtree_variable.ttf` | Presente; licencia OFL |
| Node 24.11.1 / npm 11.6.2 | sistema | Presentes (solo relevantes para React Native) |
| Flutter | — | **No instalado** |

No hace falta Android Studio, ni descargar un SDK nuevo, ni instalar otro JDK. La app de referencia se compila con:

```bash
JAVA_HOME=/d/src/tools/jdk-17.0.20.1+1 \
/d/src/tools/gradle-8.9/bin/gradle.bat assembleDebug
```

La Figtree se puede reutilizar copiando el `.ttf` y su licencia; es un activo, no código de la app de sueño. El resto de los componentes visuales se construyen de nuevo para List Master.

## Criterios de comparación

1. **Encaje con `registro_sueno`:** cuánto se comparte el stack, el criterio visual y la forma de trabajar.
2. **Velocidad para lograr la UI de referencia:** cuánto trabajo real exige la tarjeta, el árbol, los chips, la tipografía y los gestos.
3. **Costo de compilar en esta PC:** qué falta instalar, cuánto tarda la primera build y cuánto pesa el APK.
4. **Riesgo y mantenimiento:** una sola persona, un solo repo, Android solamente.
5. **Testabilidad:** tests de la lógica de progreso y del export/import sin depender de la UI.

## Opción A: Kotlin + Jetpack Compose (la referencia)

**Qué es:** app Android nativa con Kotlin 2.0.21, Compose (BOM 2024.09.03), Material 3, Room 2.6.1 con KSP, minSdk 28, Java 17. Es exactamente el stack de `registro_sueno`.

**Encaje con `registro_sueno`:** máximo. Se comparte el lenguaje, el build, la tipografía, la paleta, la forma de resolver profundidad y la manera de testear. Los dos repos se compilan con el mismo comando y el mismo JDK. No se copia código: se reutilizan decisiones y, si hace falta, el `.ttf` de Figtree.

**Velocidad para la UI de referencia:** alta. Compose tiene todo lo necesario sin puentes:
- `Canvas`/`drawBehind` para los conectores del árbol y la barra de progreso.
- `shadow` con colores ambient/spot para la sombra suave de las tarjetas.
- `HapticFeedback` nativo para marcar tareas y completar proyectos.
- Insets, gesto de atrás predictivo y edge-to-edge sin librerías extra.
- `LazyColumn` con estados de scroll y animaciones de estado sin pelear con un runtime de JavaScript.

La tarjeta de la referencia se puede reproducir con componentes propios; Material 3 aporta la estructura (botones, hojas, diálogos, navegación) y la identidad se aplica por tokens.

**Costo de compilar en esta PC:** el más bajo. Toolchain completo y dependencias exactas en caché. Estimación:
- Primera build limpia: **2–4 minutos** (sin descargas, con caché caliente).
- Build incremental: **20–60 segundos**.
- APK de referencia: **19 MB release / 24 MB debug** (medido en `registro_sueno`).
- Sin Android Studio; se instala con `adb install`.

*Las estimaciones se confirman en la fase 1, midiendo la primera build real de List Master.*

**Riesgo:** bajo. Es el stack que ya se usa y se mantiene. La única deuda es que Kotlin/Compose tiene curva si se viene de React; no es el caso de esta PC, donde ya hay una app funcionando.

**Tests:** JUnit para la lógica pura de progreso y export/import; Robolectric para DAOs de Room; tests de UI de Compose solo para flujos críticos. La lógica de progreso no necesita Android.

## Opción B: React Native con Expo

**Qué es:** app Android (y potencialmente iOS) con JavaScript/TypeScript, React Native y Expo. UI con componentes propios o `react-native-paper`; datos con `expo-sqlite` o WatermelonDB; archivos con `expo-file-system` + `expo-document-picker`; haptics con `expo-haptics`.

**Encaje con `registro_sueno`:** bajo. No se comparte lenguaje, build, componentes ni forma de testear. La identidad visual se reconstruye desde cero: React Native no reutiliza CSS ni componentes de Compose. Que las dos apps se vean de la misma familia es posible, pero es trabajo nuevo, no herencia.

**Velocidad para la UI de referencia:** media. El árbol se puede dibujar con `react-native-svg`; la barra de progreso con `Animated`/Reanimated; los chips y la tipografía con `expo-font`. Los puntos que más fricción agregan en Android:
- Sombras: `elevation` de Android es más dura que la sombra suave de la referencia; para lograrla hay que usar sombras compuestas/SVG.
- Métricas de texto y line-height: Figtree se ve distinto que en Compose; hay que ajustar a mano.
- Insets, teclado y gesto de atrás: funcionan con librerías, pero no son gratis.
- Exportar a un archivo elegido por el usuario: en Android requiere Storage Access Framework; `expo-file-system` cubre parte, pero el flujo de “guardar donde yo quiera” agrega un módulo o una vuelta extra. En Kotlin es una API del sistema.

**Costo de compilar en esta PC:** medio-alto. Node y npm están, pero no hay proyecto Expo. La primera puesta en marcha implica:
- `npm install` (cientos de MB a ~1–2 GB de `node_modules`).
- Un development build o `npx expo run:android`; el build nativo descarga las dependencias Gradle de React Native (más pesado que el de Compose).
- Metro como servidor de desarrollo para debug; el APK release empaqueta el bundle JS.
- En Windows, paths largos y enlaces simbólicos pueden sumar fricción.
- APK release típico: **25–60 MB** según módulos y Hermes.

No hay un número medido en esta PC porque no existe el proyecto; la estimación es cualitativa y se puede validar con un spike si se elige esta opción.

**Riesgo:** medio. Expo simplifica mucho, pero se suman dos toolchains (Node + Android) y una capa de puente. Para una app personal Android-only, la complejidad no compra nada de producto. Además, el camino “sin cuenta” choca con EAS Build (servicio en la nube); se puede compilar local, pero hay que configurarlo.

**Tests:** Jest para la lógica pura; React Native Testing Library para componentes. No hay tests instrumentados de Android por defecto. Alcanza, pero no es mejor que JUnit + Robolectric.

**Cuándo ganaría:** si List Master también tuviera que correr en iPhone o web, si se quisiera reutilizar experiencia en React, o si se necesitara publicar actualizaciones OTA. Nada de eso está en el alcance.

## Opción C: Flutter

**Qué es:** app Android (y potencialmente iOS) con Dart y Flutter. UI con widgets propios, `CustomPainter` para el árbol, `sqflite`/Drift para datos, `flutter_test` para tests.

**Encaje con `registro_sueno`:** bajo. Es un tercer stack: no se comparte nada con la app de sueño. La identidad se reconstruye con widgets de Flutter; Material 3 ayuda, pero la tarjeta suave y los conectores son trabajo propio.

**Velocidad para la UI de referencia:** alta-media. Flutter es muy bueno para dibujar a medida y para animaciones consistentes; el árbol y la barra se resuelven bien. El costo es aprender Dart y reconstruir el sistema de diseño. Para una sola app Android, no hay ventaja clara frente a Compose.

**Costo de compilar en esta PC:** alto al principio. **Flutter no está instalado**: hay que descargar el SDK (≈1 GB), configurar el toolchain y hacer la primera build. El SDK de Android y el JDK ya están, así que no haría falta instalar Android Studio. Después, builds incrementales razonables; APK típico ≈20 MB.

**Riesgo:** medio. Un tercer lenguaje y un tercer build para una persona que ya mantiene Kotlin. La ventaja real de Flutter (iOS + Android con la misma UI) no se usa.

**Tests:** `flutter test` es bueno para lógica y widgets. No es un diferencial frente a JUnit/Compose.

**Cuándo ganaría:** si el requisito fuera iOS + Android con una sola base de UI, o si la app necesitara gráficos personalizados muy pesados. Ninguno de los dos aplica.

## Comparación

| Criterio | Kotlin + Compose | React Native + Expo | Flutter |
|---|---|---|---|
| Encaje con `registro_sueno` | **Alto** | Bajo | Bajo |
| Velocidad para la tarjeta de referencia | **Alta** | Media | Alta-media |
| Toolchain ya instalado | **Sí, completo** | Parcial (Node sí, RN no) | No (falta Flutter) |
| Primera build en esta PC | **2–4 min estimados** | 10–20 min + descargas | 10–20 min + SDK |
| APK de referencia | **19–24 MB** | 25–60 MB | ≈20 MB |
| Haptics / back / insets nativos | **Directo** | Con librerías | Con plugins |
| Export/import a archivo del usuario | **SAF nativo** | SAF con módulo extra | Plugin |
| Tests de lógica | **JUnit puro** | Jest | flutter test |
| Mantenimiento a un año | **Un stack conocido** | Dos toolchains | Tercer stack |
| Necesidad real de multiplataforma | No | No | No |

## Recomendación

**Kotlin + Jetpack Compose, Material 3, Room con KSP, minSdk 28, Java 17.**

La decisión no es “porque la app de sueño lo usa”. Es porque, para este producto concreto:

1. **No hay requisito de multiplataforma.** List Master es Android, personal y offline. React Native y Flutter pagarían un costo de complejidad sin devolver una ventaja de producto.
2. **La UI de referencia es más barata en Compose.** El árbol, la sombra suave, los chips, los haptics y el gesto de atrás son APIs nativas o dibujo directo, no puentes.
3. **El costo de compilar es casi cero.** Todo el toolchain y las dependencias exactas ya están en esta PC; no hay que descargar SDKs, JDKs ni Android Studio. La app de referencia ya produce un APK de 19 MB con este mismo stack.
4. **La consistencia visual es real, no una promesa.** Se comparte la Figtree, la paleta, el criterio de profundidad y la forma de testear. Con React Native o Flutter habría que reconstruir esa familia desde cero.
5. **El export/import es más simple.** Storage Access Framework es una API del sistema; en RN/Flutter hay que resolverlo con módulos y casos borde.

### Qué haría cambiar la recomendación

- Si **iOS o web** entraran al alcance: React Native o Flutter pasan a ser serios.
- Si se quisiera **reutilizar experiencia en React** y aceptar reconstruir el sistema de diseño: React Native.
- Si se necesitaran **gráficos o animaciones muy personalizadas** más allá de Compose: Flutter.
- Si se quisiera **publicar actualizaciones OTA** con Expo: React Native, aunque eso agrega un servicio en la nube que hoy está fuera del alcance.

Ninguna de esas condiciones se cumple hoy.

## Consecuencias si se aprueba

- Se crea un proyecto Gradle nuevo en `D:\src\2b\list_master`, sin Android Studio.
- Paquete propuesto: `ar.matias.listmaster` (a confirmar).
- Se reutiliza el `.ttf` de Figtree y su licencia OFL; no se copia código de `registro_sueno`.
- Se usan las mismas versiones de AGP, Kotlin, KSP, Compose BOM y Room que la app de sueño, para no abrir dos frentes de compatibilidad.
- La primera tarea de la fase 1 es un esqueleto que compile e instale, antes de cualquier pantalla.
