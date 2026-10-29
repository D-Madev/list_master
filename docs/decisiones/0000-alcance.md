# 0000: Alcance y decisiones

## Qué es

Una app mobile propia para administrar proyectos con **subproyectos y tareas**, en lugar de repartirlos entre listas sueltas, y para tener aparte las listas simples (compras, películas por ver).

## Alcance

**Dentro:**
- Proyectos, con subproyectos y tareas. El progreso de cada proyecto se ve sin abrirlo.
- Listas simples: compras, películas por ver, y otras que no pertenezcan a un proyecto.
- Uso desde el celular, siempre a mano.

**Fuera** (ya viven en el vault de Obsidian o pertenecen a otro proyecto):
- Sesiones de psicología.
- Volcado de ideas.
- Finanzas.
- Lectura.
- Anillo de tiempo (vista de 24 horas): idea de la propuesta inicial, sin validar. Si vuelve, es otro proyecto.
- Agenda y calendario.
- Fichas tipo Zettelkasten: ya lo cubre el vault.

## Decisiones tomadas

- **Relación con A.R.G.O.S:** proyecto propio, que se integra más adelante. Hoy no depende de A.R.G.O.S.
- **Plataforma:** mobile, pensada para el celular.
- **Datos:** locales, sin cuenta ni servidor. Exportables a JSON con IDs estables, para que la integración futura sea posible.
- **Identidad visual:** la misma familia que la app de sueño (`D:\src\2b\registro_sueno`), según su `DESIGN.md`.
- **Referencia de stack:** la app de sueño es Kotlin + Jetpack Compose, con Room. No usa React.

## Decisiones abiertas

Las toma la fase 0, con opciones y recomendación antes de escribir código:

1. **Stack** para Android. Se compara Kotlin + Compose (la referencia), React Native con Expo y Flutter.
2. **Metodología** de trabajo sobre el repo.
3. **DESIGN.md propio** del repo, derivado de la identidad de la app de sueño.
