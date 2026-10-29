# Prompt de inicio

Pegalo completo al abrir Claude Code en `D:\src\list-master`.

```text
Vas a arrancar List Master, una app Android propia para gestionar proyectos con subproyectos, tareas y listas. El repo ya existe en D:\src\list-master con README y docs/. Antes de tocar nada, leé docs/decisiones/0000-alcance.md y docs/referencias/tarjeta-progreso.png.

Más adelante la app se va a integrar con un sistema mayor (A.R.G.O.S), pero hoy NO construyas esa integración. Sí dejá los datos en un formato que la haga posible.

## Problema
Los proyectos y sus tareas están repartidos entre listas sueltas. Hace falta un lugar único donde un proyecto tenga subproyectos, tareas y progreso visible, y donde las listas simples (compras, películas por ver) vivan aparte.

## Alcance
Dentro: proyectos, subproyectos, tareas, listas simples.
Fuera (ya viven en el vault o pertenecen a otro proyecto; no los construyas): notas de sesiones de psicología, volcado de ideas, finanzas, lectura, anillo de tiempo, calendario o agenda, login, nube, sincronización, compartir.

## Principios
- Mobile first: se usa con una mano, texto legible, pantallas simples.
- Sin cuenta, sin servidor, sin analytics. Los datos quedan en el celular.
- Funciona siempre sin conexión.
- Lo que no está en el alcance no se agrega "por si acaso".
- IDs estables (UUID) y datos exportables: la integración futura depende de eso.
- Primero la UX, después todo lo demás. Si una instrucción de este prompt te parece confusa o fea, señalalo antes de construir.

## Stack de referencia
La app de sueño del mismo usuario (D:\src\2b\registro_sueno) es la referencia de stack y de diseño:
- Kotlin + Jetpack Compose, Material 3, Room con KSP, minSdk 28, Java 17. No usa React.
- Compila sin Android Studio: JDK en D:\src\tools\jdk-17.0.20.1+1 y Gradle 8.9 en D:\src\tools\gradle-8.9\bin\gradle.bat.
- Su DESIGN.md (D:\src\2b\registro_sueno\DESIGN.md) define la identidad visual. Leelo.

En la fase 0 se comparan opciones (ver abajo). Si proponés algo distinto de Kotlin + Compose, justificá por qué conviene romper la consistencia con esa app. El usuario mencionó React como posibilidad: incluí React Native con Expo en la comparación.

## Identidad visual
Heredada de la app de sueño, para que las dos se vean de la misma familia. En la fase 0 se crea un DESIGN.md propio del repo a partir de esto (sin copiar código):
- Tipografía Figtree (variable, pesos 400 a 900), títulos en SemiBold.
- Paleta: oscuro #0D1117 con acento #7DCFFF; claro #F6F8FB con acento #1A6FA8. Tema claro, oscuro y seguir al sistema.
- Profundidad, no planitud: tarjetas que se despegan del fondo con sombra suave, nunca tarjetas que se confunden con el fondo.
- Iconos de línea, no rellenos.
- Rótulos en Mayúscula Palabra Por Palabra ("En Curso", "Mis Proyectos"). Las frases de estado se escriben como se hablan, en segunda persona.
- Haptics en gestos significativos (marcar una tarea, completar un proyecto). Nunca en navegación simple.
- Animación con propósito: sólo si ayuda a entender un cambio de estado.
- Nunca inventar datos: si algo no tiene valor, decirlo.

## Referencia visual: la tarjeta de proyecto
Ver docs/referencias/tarjeta-progreso.png. Hay que lograr:
- Encabezado con ícono, título y menú (…) a la derecha.
- Pastilla de progreso: "3 de 4" y "75%", con barra.
- Subtareas en árbol, con conectores. Las hechas con un círculo de check lleno; las pendientes con círculo vacío.
- Chips de Prioridad y Estado, con fondo de color suave y su ícono.
- Adaptación: la referencia muestra personas asignadas (avatares). Esta app es personal, así que esa fila no va.
- Sombra y bordes redondeados, consistentes con el principio de profundidad.

Para diseñar las pantallas, usá las skills de diseño disponibles: `impeccable` para pantallas nativas Android, `emil-design-eng` para los detalles de interacción y `frontend-design` para la dirección visual. El objetivo es que se vea cuidada, no sólo que funcione.

## Modelo de datos (propuesta, ajustable en la fase 0)
- Proyecto: id, titulo, estado (Activo, Pausa, Terminado, Cancelado, Postergado; mismo vocabulario que el vault), proyecto_padre (opcional, para subproyectos), creado.
- Tarea: id, titulo, hecha, proyecto_id, fecha (opcional).
- Lista: id, titulo, proyecto_id (opcional), items [{id, titulo, hecha}].
- El progreso de un proyecto se calcula desde sus tareas y subproyectos. Nunca se escribe a mano.
- Subproyectos hasta 3 niveles de profundidad.

## Fase 0: antes de escribir código (parar al final)
1. Crear .gitignore si falta, y escribir docs/decisiones/0001-plataforma-y-stack.md con 2 o 3 opciones para la app: Kotlin + Compose (la referencia), React Native con Expo, y Flutter si conviene. Para cada una: encaje con registro_sueno, velocidad para lograr la UI de la referencia, y costo de compilar en esta PC. Recomendá una y justificala mostrando las alternativas.
2. Escribir docs/decisiones/0002-metodologia.md: cortes verticales pequeños, tests de la lógica de progreso, un commit por tarea terminada.
3. Escribir DESIGN.md en la raíz del repo, a partir de la identidad visual de arriba.
4. Parar y esperar la aprobación del stack antes de crear el proyecto de la app.

## Fase 1: MVP (sólo después de aprobar la fase 0)
Criterios de aceptación:
- Crear, editar, cerrar y borrar proyectos; cambiar su estado.
- Crear subproyectos hasta 3 niveles.
- Crear tareas dentro de un proyecto, marcarlas, con fecha opcional.
- Cada proyecto muestra progreso (hechas / total, con barra) que incluye sus subproyectos, con la tarjeta parecida a la referencia.
- Crear listas, marcar ítems, vincularlas a un proyecto o dejarlas sueltas.
- Los datos sobreviven a cerrar la app y a reiniciar el celular.
- Exportar todo a un archivo JSON, e importar ese mismo archivo.
- Funciona sin conexión.
- Tema claro, oscuro y seguir al sistema.
- Tests unitarios de la lógica de progreso y de exportar/importar.

## Reglas de trabajo
- Commit al terminar cada tarea completa, con mensaje en español en imperativo, como los demás repos.
- Código e identificadores en inglés; interfaz y documentación en español. Los nombres de campos del export van en español para coincidir con el vault.
- Antes de un cambio grande, mostrar el plan.
- Si surge una decisión que este prompt no cubre, anotarla en docs/decisiones/ en vez de resolverla en silencio.
```
