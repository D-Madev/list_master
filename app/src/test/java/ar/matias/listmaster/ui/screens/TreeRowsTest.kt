package ar.matias.listmaster.ui.screens

import ar.matias.listmaster.domain.model.Project
import ar.matias.listmaster.domain.model.ProjectStatus
import ar.matias.listmaster.domain.model.Task
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TreeRowsTest {

    private val base = Instant.parse("2026-10-30T12:00:00Z")

    private fun project(id: String, title: String, parentId: String? = null) = Project(
        id = id,
        title = title,
        status = ProjectStatus.ACTIVE,
        parentId = parentId,
        priority = null,
        createdAt = base,
        updatedAt = base,
    )

    private fun task(id: String, title: String, projectId: String) = Task(
        id = id,
        title = title,
        done = false,
        projectId = projectId,
        dueDate = null,
        createdAt = base,
        updatedAt = base,
    )

    @Test
    fun `las guias continuan hasta el ultimo hermano y bajan a los hijos`() {
        val root = project("root", "Raíz")
        val a = task("a", "A", root.id)
        val b = project("b", "B", root.id)
        val b1 = task("b1", "B1", b.id)
        val b2 = task("b2", "B2", b.id)
        val c = project("c", "C", root.id)
        val c1 = task("c1", "C1", c.id)

        val rows = buildDetailRows(
            root = root,
            projects = listOf(root, b, c),
            tasks = listOf(a, b1, b2, c1),
        )

        val byTitle = rows.associateBy {
            when (it) {
                is DetailRow.TaskItem -> it.task.title
                is DetailRow.ProjectItem -> it.project.title
            }
        }

        fun row(title: String): DetailRow = byTitle.getValue(title)

        // A y B tienen un hermano después: su vertical sigue hasta el final.
        assertEquals(false, row("A").isLast)
        assertEquals(false, row("B").isLast)
        assertTrue(row("B").ancestorContinues.isEmpty())

        // B1 y B2 cuelgan de B, que todavía tiene a C como hermano.
        assertEquals(listOf(true), row("B1").ancestorContinues)
        assertEquals(false, row("B1").isLast)
        assertEquals(listOf(true), row("B2").ancestorContinues)
        assertEquals(true, row("B2").isLast)

        // C es el último hermano: su rama termina y no sigue en C1.
        assertEquals(true, row("C").isLast)
        assertEquals(listOf(false), row("C1").ancestorContinues)
        assertEquals(true, row("C1").isLast)

        // B y C tienen hijos: dibujan el tallo que baja hasta la primera tarea.
        assertTrue((row("B") as DetailRow.ProjectItem).hasChildren)
        assertTrue((row("C") as DetailRow.ProjectItem).hasChildren)
    }
}
