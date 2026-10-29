package ar.matias.listmaster.domain

import ar.matias.listmaster.domain.model.Project
import ar.matias.listmaster.domain.model.ProjectStatus
import ar.matias.listmaster.domain.model.Task
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressCalculatorTest {

    private val base = Instant.parse("2026-10-29T12:00:00Z")

    private fun project(
        id: String,
        parentId: String? = null,
        status: ProjectStatus = ProjectStatus.ACTIVE,
    ) = Project(
        id = id,
        title = id,
        status = status,
        parentId = parentId,
        priority = null,
        createdAt = base,
        updatedAt = base,
    )

    private fun task(id: String, projectId: String, done: Boolean) = Task(
        id = id,
        title = id,
        done = done,
        projectId = projectId,
        dueDate = null,
        createdAt = base,
        updatedAt = base,
    )

    @Test
    fun `proyecto sin tareas no inventa progreso`() {
        val progress = ProgressCalculator.calculate(
            rootId = "a",
            projects = listOf(project("a")),
            tasks = emptyList(),
        )

        assertFalse(progress.hasTasks)
        assertEquals(0, progress.total)
        assertEquals(0, progress.percentage)
        assertFalse(progress.isComplete)
    }

    @Test
    fun `tres de cuatro es setenta y cinco`() {
        val progress = ProgressCalculator.calculate(
            rootId = "a",
            projects = listOf(project("a")),
            tasks = listOf(
                task("t1", "a", true),
                task("t2", "a", true),
                task("t3", "a", true),
                task("t4", "a", false),
            ),
        )

        assertEquals(3, progress.done)
        assertEquals(4, progress.total)
        assertEquals(75, progress.percentage)
        assertFalse(progress.isComplete)
    }

    @Test
    fun `las tareas de los subproyectos suman al padre`() {
        val progress = ProgressCalculator.calculate(
            rootId = "a",
            projects = listOf(project("a"), project("b", parentId = "a")),
            tasks = listOf(
                task("t1", "a", true),
                task("t2", "b", false),
            ),
        )

        assertEquals(1, progress.done)
        assertEquals(2, progress.total)
        assertEquals(50, progress.percentage)
    }

    @Test
    fun `una tarea en el tercer nivel de subproyectos suma a la raiz`() {
        val progress = ProgressCalculator.calculate(
            rootId = "a",
            projects = listOf(
                project("a"),
                project("b", parentId = "a"),
                project("c", parentId = "b"),
                project("d", parentId = "c"),
            ),
            tasks = listOf(task("t1", "d", true)),
        )

        assertEquals(1, progress.done)
        assertEquals(1, progress.total)
        assertEquals(100, progress.percentage)
    }

    @Test
    fun `un subproyecto terminado con pendientes sigue contando`() {
        val progress = ProgressCalculator.calculate(
            rootId = "a",
            projects = listOf(
                project("a"),
                project("b", parentId = "a", status = ProjectStatus.DONE),
            ),
            tasks = listOf(
                task("t1", "b", true),
                task("t2", "b", false),
            ),
        )

        assertEquals(1, progress.done)
        assertEquals(2, progress.total)
        assertEquals(50, progress.percentage)
    }

    @Test
    fun `un subarbol cancelado no suma al padre`() {
        val progress = ProgressCalculator.calculate(
            rootId = "a",
            projects = listOf(
                project("a"),
                project("b", parentId = "a", status = ProjectStatus.CANCELLED),
                project("c", parentId = "b"),
            ),
            tasks = listOf(
                task("t1", "a", true),
                task("t2", "b", false),
                task("t3", "c", false),
            ),
        )

        assertEquals(1, progress.done)
        assertEquals(1, progress.total)
        assertEquals(100, progress.percentage)
    }

    @Test
    fun `un proyecto cancelado no muestra progreso`() {
        val progress = ProgressCalculator.calculate(
            rootId = "a",
            projects = listOf(project("a", status = ProjectStatus.CANCELLED)),
            tasks = listOf(task("t1", "a", true)),
        )

        assertFalse(progress.hasTasks)
    }

    @Test
    fun `ciento noventa y nueve de doscientas se topa en noventa y nueve`() {
        val tasks = (1..200).map { index ->
            task("t$index", "a", done = index <= 199)
        }

        val progress = ProgressCalculator.calculate(
            rootId = "a",
            projects = listOf(project("a")),
            tasks = tasks,
        )

        assertEquals(199, progress.done)
        assertEquals(200, progress.total)
        assertEquals(99, progress.percentage)
        assertFalse(progress.isComplete)
    }

    @Test
    fun `todo hecho es cien`() {
        val progress = ProgressCalculator.calculate(
            rootId = "a",
            projects = listOf(project("a")),
            tasks = listOf(task("t1", "a", true), task("t2", "a", true)),
        )

        assertEquals(100, progress.percentage)
        assertTrue(progress.isComplete)
    }

    @Test
    fun `cero de cuatro es cero por ciento real`() {
        val progress = ProgressCalculator.calculate(
            rootId = "a",
            projects = listOf(project("a")),
            tasks = listOf(task("t1", "a", false)),
        )

        assertEquals(0, progress.done)
        assertEquals(1, progress.total)
        assertEquals(0, progress.percentage)
        assertTrue(progress.hasTasks)
    }

    @Test
    fun `un ciclo no cuelga el calculo`() {
        val progress = ProgressCalculator.calculate(
            rootId = "a",
            projects = listOf(
                project("a", parentId = "b"),
                project("b", parentId = "a"),
            ),
            tasks = listOf(task("t1", "a", true), task("t2", "b", false)),
        )

        assertEquals(1, progress.done)
        assertEquals(2, progress.total)
    }

    @Test
    fun `un proyecto inexistente no rompe`() {
        val progress = ProgressCalculator.calculate("fantasma", emptyList(), emptyList())

        assertFalse(progress.hasTasks)
    }
}
