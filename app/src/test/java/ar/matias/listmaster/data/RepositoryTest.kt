package ar.matias.listmaster.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ar.matias.listmaster.data.local.ListMasterDatabase
import ar.matias.listmaster.domain.model.AppData
import ar.matias.listmaster.domain.model.Priority
import ar.matias.listmaster.domain.model.Project
import ar.matias.listmaster.domain.model.ProjectStatus
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class RepositoryTest {

    private lateinit var db: ListMasterDatabase
    private lateinit var repository: ListMasterRepository
    private var clock: Instant = Instant.parse("2026-10-29T12:00:00Z")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ListMasterDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ListMasterRepository(db) { clock }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun tick() {
        clock = clock.plusSeconds(1)
    }

    @Test
    fun `borrar un proyecto arrastra el arbol y suelta las listas`() = runBlocking {
        val root = repository.createProject("Diseño Web")
        tick()
        val child = repository.createProject("Identidad", parentId = root)
        tick()
        repository.createTask(child, "Definir la paleta", LocalDate.parse("2026-11-05"))
        tick()
        val list = repository.createList("Compras", projectId = child)
        tick()
        repository.addListItem(list, "Leche")

        repository.deleteProject(root)

        val data = repository.snapshot()
        assertTrue(data.projects.isEmpty())
        assertTrue(data.tasks.isEmpty())
        assertEquals(1, data.lists.size)
        assertNull(data.lists.single().projectId)
        assertEquals(1, data.lists.single().items.size)
    }

    @Test
    fun `el orden de creacion se conserva`() = runBlocking {
        val first = repository.createProject("Primero")
        tick()
        val second = repository.createProject("Segundo")
        tick()
        repository.createProject("Tercero")

        val projects = repository.snapshot().projects
        assertEquals(listOf(first, second), projects.take(2).map { it.id })
        assertEquals(listOf("Primero", "Segundo", "Tercero"), projects.map { it.title })
    }

    @Test
    fun `no permite un cuarto nivel de subproyectos`() = runBlocking {
        val level1 = repository.createProject("Nivel 1")
        tick()
        val level2 = repository.createProject("Nivel 2", parentId = level1)
        tick()
        val level3 = repository.createProject("Nivel 3", parentId = level2)
        tick()
        val level4 = repository.createProject("Nivel 4", parentId = level3)
        tick()

        // level4 ya está en el tercer nivel de subproyectos: no admite hijos.
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.createProject("Nivel 5", parentId = level4) }
        }
        Unit
    }

    @Test
    fun `valida titulos vacios y largos`() = runBlocking {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.createProject("   ") }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.createProject("a".repeat(121)) }
        }

        val id = repository.createProject("  Con espacios  ")
        assertEquals("Con espacios", repository.snapshot().projects.single().title)
        assertTrue(id.isNotBlank())
    }

    @Test
    fun `marcar una tarea actualiza el progreso y la fecha`() = runBlocking {
        val project = repository.createProject("Proyecto")
        tick()
        val task = repository.createTask(project, "Tarea", LocalDate.parse("2026-11-05"))
        tick()
        repository.setTaskDone(task, true)

        val saved = repository.snapshot().tasks.single()
        assertTrue(saved.done)
        assertEquals(LocalDate.parse("2026-11-05"), saved.dueDate)
    }

    @Test
    fun `la ubicacion se guarda y se puede cambiar`() = runBlocking {
        val project = repository.createProject("Proyecto")
        tick()
        val task = repository.createTask(
            project,
            "Comprar filtros",
            null,
            location = "Ferretería",
        )
        assertEquals("Ferretería", repository.snapshot().tasks.single().location)

        tick()
        repository.updateTask(task, "Comprar filtros", null, location = "Casa")
        assertEquals("Casa", repository.snapshot().tasks.single().location)

        tick()
        repository.updateTask(task, "Comprar filtros", null)
        assertEquals("", repository.snapshot().tasks.single().location)
    }

    @Test
    fun `mover una tarea conserva sus datos y cambia solo el proyecto`() = runBlocking {
        val origin = repository.createProject("Área")
        tick()
        val target = repository.createProject("Proyecto específico")
        tick()
        val task = repository.createTask(
            origin,
            "Definir la paleta",
            LocalDate.parse("2026-11-05"),
            description = "Notas",
            priority = Priority.HIGH,
        )
        tick()
        repository.setTaskDone(task, true)
        tick()

        repository.updateTask(
            task,
            "Definir la paleta",
            LocalDate.parse("2026-11-05"),
            description = "Notas",
            priority = Priority.HIGH,
            newProjectId = target,
        )

        val moved = repository.snapshot().tasks.single()
        assertEquals(target, moved.projectId)
        assertEquals("Definir la paleta", moved.title)
        assertEquals(LocalDate.parse("2026-11-05"), moved.dueDate)
        assertEquals("Notas", moved.description)
        assertEquals(Priority.HIGH, moved.priority)
        assertTrue(moved.done)
        assertEquals(clock, moved.updatedAt)
    }

    @Test
    fun `mover una tarea a un proyecto inexistente falla y no la toca`() = runBlocking {
        val origin = repository.createProject("Área")
        tick()
        val task = repository.createTask(origin, "Tarea", null)
        tick()

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.updateTask(task, "Tarea", null, newProjectId = "no-existe")
            }
        }

        assertEquals(origin, repository.snapshot().tasks.single().projectId)
    }

    @Test
    fun `los items de lista conservan el orden y se pueden marcar`() = runBlocking {
        val list = repository.createList("Compras", projectId = null)
        repository.addListItem(list, "Leche")
        tick()
        repository.addListItem(list, "Pan")
        tick()
        repository.addListItem(list, "Café")

        val items = repository.snapshot().lists.single().items
        assertEquals(listOf("Leche", "Pan", "Café"), items.map { it.title })
        assertEquals(listOf(0, 1, 2), items.map { it.position })

        repository.setListItemDone(items[1].id, true)
        assertTrue(repository.snapshot().lists.single().items[1].done)
    }

    @Test
    fun `estado y prioridad se guardan`() = runBlocking {
        val id = repository.createProject("Proyecto", priority = Priority.HIGH)
        tick()
        repository.setProjectStatus(id, ProjectStatus.DONE)
        tick()
        repository.setProjectPriority(id, null)

        val project = repository.snapshot().projects.single()
        assertEquals(ProjectStatus.DONE, project.status)
        assertNull(project.priority)
    }

    @Test
    fun `reemplazar todo deja exactamente los datos importados`() = runBlocking {
        val old = repository.createProject("Viejo")
        tick()
        repository.createTask(old, "Tarea vieja", null)

        val imported = AppData(
            projects = listOf(
                Project(
                    id = "550e8400-e29b-41d4-a716-446655440000",
                    title = "Importado",
                    status = ProjectStatus.ACTIVE,
                    parentId = null,
                    priority = Priority.MEDIUM,
                    createdAt = clock,
                    updatedAt = clock,
                ),
            ),
            tasks = emptyList(),
            lists = emptyList(),
        )

        repository.replaceAll(imported)

        val data = repository.snapshot()
        assertEquals(listOf("Importado"), data.projects.map { it.title })
        assertTrue(data.tasks.isEmpty())
        assertTrue(data.lists.isEmpty())
    }
}
