package ar.matias.listmaster.data

import ar.matias.listmaster.domain.model.AppData
import ar.matias.listmaster.domain.model.ListItem
import ar.matias.listmaster.domain.model.Priority
import ar.matias.listmaster.domain.model.Project
import ar.matias.listmaster.domain.model.ProjectStatus
import ar.matias.listmaster.domain.model.Task
import ar.matias.listmaster.domain.model.TaskList
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupCodecTest {

    private val base = Instant.parse("2026-10-29T12:00:00Z")

    private val projectId = "550e8400-e29b-41d4-a716-446655440000"
    private val childId = "6ba7b810-9dad-41d1-80b4-00c04fd430c8"
    private val taskId = "7c9e6679-7425-40de-944b-e07fc1f90ae7"
    private val listId = "9b2f4c1e-3a6d-4b8e-9f21-5c7d0a1b2e3f"
    private val itemId = "c1a2b3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d"

    private fun sampleData() = AppData(
        projects = listOf(
            Project(
                id = projectId,
                title = "Diseño Web",
                status = ProjectStatus.ACTIVE,
                parentId = null,
                priority = Priority.HIGH,
                createdAt = base,
                updatedAt = base.plusSeconds(60),
                icon = "star",
            ),
            Project(
                id = childId,
                title = "Identidad",
                status = ProjectStatus.PAUSED,
                parentId = projectId,
                priority = null,
                createdAt = base.plusSeconds(10),
                updatedAt = base.plusSeconds(20),
            ),
        ),
        tasks = listOf(
            Task(
                id = taskId,
                title = "Definir la paleta 🎨",
                done = true,
                projectId = childId,
                dueDate = LocalDate.parse("2026-11-05"),
                createdAt = base.plusSeconds(30),
                updatedAt = base.plusSeconds(40),
            ),
        ),
        lists = listOf(
            TaskList(
                id = listId,
                title = "Compras",
                projectId = null,
                createdAt = base.plusSeconds(50),
                updatedAt = base.plusSeconds(70),
                items = listOf(
                    ListItem(id = itemId, title = "Leche", done = false, position = 0),
                ),
            ),
        ),
    )

    private fun decode(text: String): AppData = BackupCodec.decode(text)

    private fun expectFailure(text: String, fragment: String? = null) {
        try {
            BackupCodec.decode(text)
            fail("Se esperaba un BackupException")
        } catch (error: BackupException) {
            if (fragment != null) {
                assertTrue(
                    "Mensaje inesperado: ${error.message}",
                    error.message.orEmpty().contains(fragment),
                )
            }
        }
    }

    private fun validJson(
        projects: String = """
            {"id":"$projectId","titulo":"Diseño Web","estado":"Activo",
             "proyecto_padre":null,"prioridad":"Alta",
             "creado":"2026-10-20T12:15:00Z","actualizado":"2026-10-29T17:30:00Z"}
        """.trimIndent(),
        tasks: String = """
            {"id":"$taskId","titulo":"Definir la paleta","hecha":true,
             "proyecto_id":"$projectId","fecha":"2026-11-05",
             "creado":"2026-10-21T09:00:00Z","actualizado":"2026-10-29T17:30:00Z"}
        """.trimIndent(),
        lists: String = """
            {"id":"$listId","titulo":"Compras","proyecto_id":null,
             "creado":"2026-10-22T10:00:00Z","actualizado":"2026-10-28T18:00:00Z",
             "items":[{"id":"$itemId","titulo":"Leche","hecha":false,"orden":0}]}
        """.trimIndent(),
    ) = """
        {
          "version": 1,
          "aplicacion": "list_master",
          "exportado": "2026-10-29T17:32:00Z",
          "proyectos": [$projects],
          "tareas": [$tasks],
          "listas": [$lists]
        }
    """.trimIndent()

    @Test
    fun `ida y vuelta conserva todo`() {
        val data = sampleData()
        val decoded = decode(BackupCodec.encode(data, base))
        assertEquals(data, decoded)
    }

    @Test
    fun `el mismo estado produce el mismo archivo`() {
        val data = sampleData()
        assertEquals(
            BackupCodec.encode(data, base),
            BackupCodec.encode(data, base),
        )
    }

    @Test
    fun `el archivo usa los nombres de campo en espanol`() {
        val text = BackupCodec.encode(sampleData(), base)
        assertTrue(text.contains("\"aplicacion\": \"list_master\""))
        assertTrue(text.contains("\"proyectos\""))
        assertTrue(text.contains("\"proyecto_padre\""))
        assertTrue(text.contains("\"prioridad\": \"Alta\""))
        assertTrue(text.contains("\"tareas\""))
        assertTrue(text.contains("\"hecha\": true"))
        assertTrue(text.contains("\"listas\""))
        assertTrue(text.contains("\"orden\": 0"))
        assertTrue(text.endsWith("\n"))
    }

    @Test
    fun `acepta un archivo valido con campos opcionales en null`() {
        val data = decode(validJson())
        assertEquals(1, data.projects.size)
        assertEquals(ProjectStatus.ACTIVE, data.projects.single().status)
        assertEquals(Priority.HIGH, data.projects.single().priority)
        assertEquals(LocalDate.parse("2026-11-05"), data.tasks.single().dueDate)
        assertEquals(1, data.lists.single().items.size)
    }

    @Test
    fun `ignora un BOM al inicio`() {
        val data = decode("\uFEFF" + validJson())
        assertEquals(1, data.projects.size)
    }

    @Test
    fun `ignora campos desconocidos`() {
        val text = validJson().replace(
            "\"version\": 1,",
            "\"version\": 1,\n  \"futuro\": {\"algo\": true},",
        )
        val data = decode(text)
        assertEquals(1, data.projects.size)
    }

    @Test
    fun `si falta actualizado usa creado`() {
        val data = decode(
            """
            {
              "version": 1,
              "aplicacion": "list_master",
              "exportado": "2026-10-29T17:32:00Z",
              "proyectos": [
                {"id":"$projectId","titulo":"Diseño Web","estado":"Activo",
                 "proyecto_padre":null,"prioridad":null,
                 "creado":"2026-10-20T12:15:00Z"}
              ],
              "tareas": [],
              "listas": []
            }
            """.trimIndent(),
        )
        assertEquals(baseOf("2026-10-20T12:15:00Z"), data.projects.single().updatedAt)
    }

    @Test
    fun `si falta orden usa el indice`() {
        val text = """
            {
              "version": 1,
              "aplicacion": "list_master",
              "exportado": "2026-10-29T17:32:00Z",
              "proyectos": [],
              "tareas": [],
              "listas": [
                {"id":"$listId","titulo":"Compras","proyecto_id":null,
                 "creado":"2026-10-22T10:00:00Z","actualizado":"2026-10-28T18:00:00Z",
                 "items":[
                   {"id":"$itemId","titulo":"Leche","hecha":false},
                   {"id":"c1a2b3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5e","titulo":"Pan","hecha":false}
                 ]}
              ]
            }
        """.trimIndent()
        val items = decode(text).lists.single().items
        assertEquals(listOf(0, 1), items.map { it.position })
    }

    @Test
    fun `rechaza un json roto`() {
        expectFailure("{ esto no es json", "JSON")
    }

    @Test
    fun `rechaza otra aplicacion`() {
        expectFailure(validJson().replace("list_master", "otra_app"), "export de List Master")
    }

    @Test
    fun `rechaza una version mas nueva`() {
        expectFailure(
            validJson().replace("\"version\": 1", "\"version\": 2"),
            "versión más nueva",
        )
    }

    @Test
    fun `rechaza ids repetidos`() {
        val first = """
            {"id":"$projectId","titulo":"Uno","estado":"Activo",
             "proyecto_padre":null,"prioridad":null,
             "creado":"2026-10-20T12:15:00Z","actualizado":"2026-10-20T12:15:00Z"}
        """.trimIndent()
        val second = """
            {"id":"$projectId","titulo":"Dos","estado":"Activo",
             "proyecto_padre":null,"prioridad":null,
             "creado":"2026-10-20T12:16:00Z","actualizado":"2026-10-20T12:16:00Z"}
        """.trimIndent()
        expectFailure(
            validJson(projects = "$first, $second", tasks = "", lists = ""),
            "repetido",
        )
    }

    @Test
    fun `rechaza una tarea con proyecto inexistente`() {
        val text = validJson(
            projects = "",
            tasks = """
                {"id":"$taskId","titulo":"Suelta","hecha":false,
                 "proyecto_id":"$projectId","fecha":null,
                 "creado":"2026-10-21T09:00:00Z","actualizado":"2026-10-21T09:00:00Z"}
            """.trimIndent(),
        )
        expectFailure(text, "no existe")
    }

    @Test
    fun `rechaza una lista con proyecto inexistente`() {
        val text = validJson(
            projects = "",
            tasks = "",
            lists = """
                {"id":"$listId","titulo":"Compras","proyecto_id":"$projectId",
                 "creado":"2026-10-22T10:00:00Z","actualizado":"2026-10-22T10:00:00Z",
                 "items":[]}
            """.trimIndent(),
        )
        expectFailure(text, "no existe")
    }

    @Test
    fun `rechaza un padre inexistente`() {
        val text = validJson(
            projects = """
                {"id":"$projectId","titulo":"Hijo","estado":"Activo",
                 "proyecto_padre":"$childId","prioridad":null,
                 "creado":"2026-10-20T12:15:00Z","actualizado":"2026-10-20T12:15:00Z"}
            """.trimIndent(),
            tasks = "",
            lists = "",
        )
        expectFailure(text, "no existe")
    }

    @Test
    fun `rechaza ciclos`() {
        val a = """
            {"id":"$projectId","titulo":"A","estado":"Activo",
             "proyecto_padre":"$childId","prioridad":null,
             "creado":"2026-10-20T12:15:00Z","actualizado":"2026-10-20T12:15:00Z"}
        """.trimIndent()
        val b = """
            {"id":"$childId","titulo":"B","estado":"Activo",
             "proyecto_padre":"$projectId","prioridad":null,
             "creado":"2026-10-20T12:16:00Z","actualizado":"2026-10-20T12:16:00Z"}
        """.trimIndent()
        expectFailure(validJson(projects = "$a, $b", tasks = "", lists = ""), "ciclo")
    }

    @Test
    fun `rechaza mas de tres niveles de subproyectos`() {
        fun node(id: String, parent: String?, minute: Int): String {
            val stamp = "2026-10-20T12:%02d:00Z".format(minute)
            return """
                {"id":"$id","titulo":"N$minute","estado":"Activo",
                 "proyecto_padre":${parent?.let { "\"$it\"" } ?: "null"},"prioridad":null,
                 "creado":"$stamp","actualizado":"$stamp"}
            """.trimIndent()
        }

        val ids = listOf(
            "11111111-1111-4111-8111-111111111111",
            "22222222-2222-4222-8222-222222222222",
            "33333333-3333-4333-8333-333333333333",
            "44444444-4444-4444-8444-444444444444",
            "55555555-5555-4555-8555-555555555555",
        )
        val nodes = ids.mapIndexed { index, id ->
            node(id, if (index == 0) null else ids[index - 1], index)
        }
        expectFailure(validJson(projects = nodes.joinToString(","), tasks = "", lists = ""), "tres niveles")
    }

    @Test
    fun `rechaza estados y prioridades invalidos`() {
        expectFailure(
            validJson().replace("\"estado\":\"Activo\"", "\"estado\":\"Volando\""),
            "estado inválido",
        )
        expectFailure(
            validJson().replace("\"prioridad\":\"Alta\"", "\"prioridad\":\"Urgente\""),
            "prioridad inválida",
        )
    }

    @Test
    fun `rechaza fechas invalidas`() {
        expectFailure(
            validJson().replace("\"fecha\":\"2026-11-05\"", "\"fecha\":\"2026-13-40\""),
            "fecha inválida",
        )
        expectFailure(
            validJson().replace("\"creado\":\"2026-10-20T12:15:00Z\"", "\"creado\":\"ayer\""),
            "ISO-8601",
        )
    }

    @Test
    fun `rechaza titulos vacios o demasiado largos`() {
        expectFailure(
            validJson(projects = """
                {"id":"$projectId","titulo":"   ","estado":"Activo",
                 "proyecto_padre":null,"prioridad":null,
                 "creado":"2026-10-20T12:15:00Z","actualizado":"2026-10-20T12:15:00Z"}
            """.trimIndent(), tasks = "", lists = ""),
            "vacío",
        )
        expectFailure(
            validJson(projects = """
                {"id":"$projectId","titulo":"${"a".repeat(121)}","estado":"Activo",
                 "proyecto_padre":null,"prioridad":null,
                 "creado":"2026-10-20T12:15:00Z","actualizado":"2026-10-20T12:15:00Z"}
            """.trimIndent(), tasks = "", lists = ""),
            "120",
        )
    }

    @Test
    fun `rechaza ids que no son uuid`() {
        expectFailure(
            validJson(projects = """
                {"id":"no-es-uuid","titulo":"X","estado":"Activo",
                 "proyecto_padre":null,"prioridad":null,
                 "creado":"2026-10-20T12:15:00Z","actualizado":"2026-10-20T12:15:00Z"}
            """.trimIndent(), tasks = "", lists = ""),
            "UUID",
        )
    }

    @Test
    fun `un archivo vacio es valido`() {
        val text = """
            {
              "version": 1,
              "aplicacion": "list_master",
              "exportado": "2026-10-29T17:32:00Z",
              "proyectos": [],
              "tareas": [],
              "listas": []
            }
        """.trimIndent()
        assertEquals(AppData.EMPTY, decode(text))
    }

    @Test
    fun `conserva unicode y emojis`() {
        val data = sampleData()
        val decoded = decode(BackupCodec.encode(data, base))
        assertEquals("Definir la paleta 🎨", decoded.tasks.single().title)
    }

    @Test
    fun `conserva el icono y el emoji del proyecto`() {
        val data = sampleData()
        val decoded = decode(BackupCodec.encode(data, base))
        assertEquals("star", decoded.projects.single { it.id == projectId }.icon)

        val withEmoji = data.copy(
            projects = data.projects.map { project ->
                if (project.id == projectId) project.copy(icon = "🚀") else project
            },
        )
        val emoji = decode(BackupCodec.encode(withEmoji, base))
        assertEquals("🚀", emoji.projects.single { it.id == projectId }.icon)
    }

    @Test
    fun `acepta un export envuelto en un bloque de codigo`() {
        val json = BackupCodec.encode(sampleData(), base)
        val wrapped = "```json\n$json\n```"
        val decoded = decode(wrapped)
        assertEquals(2, decoded.projects.size)
        assertEquals(1, decoded.tasks.size)
        assertEquals(1, decoded.lists.size)
    }

    private fun baseOf(value: String): Instant = Instant.parse(value)
}
