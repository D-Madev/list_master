package ar.matias.listmaster.domain

import ar.matias.listmaster.domain.model.Priority
import ar.matias.listmaster.domain.model.Project
import ar.matias.listmaster.domain.model.ProjectFilter
import ar.matias.listmaster.domain.model.ProjectStatus
import ar.matias.listmaster.domain.model.Task
import ar.matias.listmaster.domain.model.TaskList
import ar.matias.listmaster.domain.model.TaskOrder
import ar.matias.listmaster.domain.model.ListItem
import ar.matias.listmaster.ui.screens.DetailRow
import ar.matias.listmaster.ui.screens.ListDetailRow
import ar.matias.listmaster.ui.screens.buildDetailRows
import ar.matias.listmaster.ui.screens.buildListDetailRows
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingAndMoveTest {

    private val base = Instant.parse("2026-10-30T12:00:00Z")
    private var tick = 0L

    private fun project(
        id: String,
        parentId: String? = null,
        priority: Priority? = null,
        status: ProjectStatus = ProjectStatus.ACTIVE,
    ) = Project(
        id = id,
        title = id,
        status = status,
        parentId = parentId,
        priority = priority,
        createdAt = base.plusSeconds(tick++),
        updatedAt = base,
    )

    private fun task(id: String, projectId: String, done: Boolean = false, priority: Priority? = null) = Task(
        id = id,
        title = id,
        done = done,
        projectId = projectId,
        dueDate = null,
        createdAt = base.plusSeconds(tick++),
        updatedAt = base,
        priority = priority,
    )

    private fun list(
        id: String,
        parentId: String? = null,
        position: Int = 0,
        items: List<ListItem> = emptyList(),
    ) = TaskList(
        id = id,
        title = id,
        projectId = null,
        parentId = parentId,
        position = position,
        createdAt = base.plusSeconds(tick++),
        updatedAt = base,
        items = items,
    )

    private fun listItem(id: String, done: Boolean = false, position: Int = 0) =
        ListItem(id = id, title = id, done = done, position = position)

    @Test
    fun `las tareas se ordenan por prioridad y a igual prioridad por antiguedad`() {
        val sin = task("sin", "P")
        val baja = task("baja", "P", priority = Priority.LOW)
        val alta2 = task("alta2", "P", priority = Priority.HIGH)
        val alta1 = task("alta1", "P", priority = Priority.HIGH)

        val sorted = listOf(sin, baja, alta1, alta2).sortedWith(TaskOrder)

        assertEquals(listOf("alta2", "alta1", "baja", "sin"), sorted.map { it.id })
    }

    @Test
    fun `los proyectos se ordenan por prioridad y los sin prioridad quedan al final`() {
        val none = project("sin")
        val low = project("baja", priority = Priority.LOW)
        val high = project("alta", priority = Priority.HIGH)
        val medium = project("media", priority = Priority.MEDIUM)

        val sorted = listOf(none, low, high, medium).sortedWith(ProjectTree.byPriority)

        assertEquals(listOf("alta", "media", "baja", "sin"), sorted.map { it.id })
    }

    @Test
    fun `la vista de pendientes agrupa por proyecto, deja afuera lo hecho y lo cerrado`() {
        val a = project("A", priority = Priority.HIGH)
        val a1 = project("A1", parentId = "A")
        val b = project("B")
        val closed = project("C", status = ProjectStatus.DONE)
        val empty = project("D")
        val tasks = listOf(
            task("a-1", "A"), task("a-2", "A", done = true), task("a1-1", "A1"),
            task("b-1", "B"), task("c-1", "C"),
        )

        val groups = PendingTasks.groups(listOf(b, a, a1, closed, empty), tasks)

        assertEquals(listOf("A", "A1", "B"), groups.map { it.project.id })
        assertEquals(listOf(0, 1, 0), groups.map { it.depth })
        assertEquals(listOf("a-1"), groups[0].tasks.map { it.id })
        assertEquals(3, PendingTasks.count(groups))
    }

    @Test
    fun `un subproyecto sin tareas propias aparece si tiene pendientes mas abajo`() {
        val root = project("R")
        val mid = project("M", parentId = "R")
        val leaf = project("L", parentId = "M")

        val groups = PendingTasks.groups(listOf(root, mid, leaf), listOf(task("l-1", "L")))

        assertEquals(listOf("R", "M", "L"), groups.map { it.project.id })
        assertTrue(groups[0].tasks.isEmpty())
    }

    @Test
    fun `en el detalle las hechas quedan en su lugar y se ocultan solo si se pide`() {
        val root = project("R")
        val sub = project("S", parentId = "R")
        val tasks = listOf(
            task("hecha", "R", done = true), task("pend-1", "R"), task("pend-2", "R"),
        )

        fun titles(show: Boolean) = buildDetailRows(root, listOf(root, sub), tasks, show).map {
            when (it) {
                is DetailRow.TaskItem -> it.task.title
                is DetailRow.ProjectItem -> it.project.title
            }
        }

        // "hecha" quedó donde le toca por antigüedad, no al final.
        assertEquals(listOf("hecha", "pend-1", "pend-2", "S"), titles(show = true))
        assertEquals(listOf("pend-1", "pend-2", "S"), titles(show = false))
    }

    @Test
    fun `al reordenar los hechos conservan su lugar y los pendientes se permutan`() {
        val all = listOf("a", "hecha", "b", "c")
        val draggable = listOf("a", "b", "c")

        assertEquals(
            listOf("b", "hecha", "a", "c"),
            ListTree.mergeReorder(all, draggable, listOf("b", "a", "c")),
        )
        assertEquals(
            listOf("b", "hecha", "c", "a"),
            ListTree.mergeReorder(all, draggable, listOf("b", "c", "a")),
        )
    }

    @Test
    fun `cualquier proyecto puede moverse entre arboles y los raiz pueden tener padre`() {
        val a = project("a")
        val a1 = project("a1", "a")
        val a2 = project("a2", "a")
        val a1x = project("a1x", "a1")
        val b = project("b")
        val b1 = project("b1", "b")
        val all = listOf(a, a1, a2, a1x, b, b1)

        val parents = ProjectTree.validParents("a1", all).map { it.id }

        // a1 puede moverse a: a (d0), b (d0), a2 (d1), b1 (d1) — no su propio subárbol
        assertEquals(listOf("a", "b", "a2", "b1"), parents)
        // a (raíz, height=2) puede ir bajo b: b(d0)+1+2=3<=3 ✓; bajo b1: b1(d1)+1+2=4>3 ✗
        assertEquals(listOf("b"), ProjectTree.validParents("a", all).map { it.id })
        assertEquals(a.id, ProjectTree.rootOf("a1x", all)?.id)
    }

    @Test
    fun `mover un subproyecto no puede pasar de tres niveles`() {
        val r = project("r")
        val l1 = project("l1", "r")
        val l2 = project("l2", "l1")
        val l3 = project("l3", "l2")
        val branch = project("branch", "r")
        val branchChild = project("branchChild", "branch")
        val all = listOf(r, l1, l2, l3, branch, branchChild)

        // branch tiene un hijo: bajo l2 quedaria a nivel 3 y su hijo a nivel 4.
        assertFalse("l2" in ProjectTree.validParents("branch", all).map { it.id })
        assertTrue("l1" in ProjectTree.validParents("branch", all).map { it.id })
    }

    @Test
    fun `el filtro por defecto oculta terminados y cancelados y se puede acotar por prioridad`() {
        val active = project("a", priority = Priority.HIGH)
        val done = project("d", status = ProjectStatus.DONE)

        assertTrue(ProjectFilter.DEFAULT.isDefault)
        assertTrue(ProjectFilter.DEFAULT.matches(active))
        assertFalse(ProjectFilter.DEFAULT.matches(done))

        val onlyLow = ProjectFilter(priorities = setOf(Priority.LOW))
        assertFalse(onlyLow.matches(active))
        assertEquals(1, onlyLow.activeCount)
    }

    @Test
    fun `cualquier lista puede moverse entre arboles y las raiz pueden tener padre`() {
        val a = list("a")
        val a1 = list("a1", "a")
        val a2 = list("a2", "a")
        val a1x = list("a1x", "a1")
        val b = list("b")
        val b1 = list("b1", "b")
        val all = listOf(a, a1, a2, a1x, b, b1)

        val parents = ListTree.validParents("a1", all).map { it.id }

        assertEquals(listOf("a", "b", "a2", "b1"), parents)
        assertEquals(listOf("b"), ListTree.validParents("a", all).map { it.id })
    }

    @Test
    fun `mover una sublista no puede pasar de tres niveles`() {
        val r = list("r")
        val l1 = list("l1", "r")
        val l2 = list("l2", "l1")
        val l3 = list("l3", "l2")
        val branch = list("branch", "r")
        val branchChild = list("branchChild", "branch")
        val all = listOf(r, l1, l2, l3, branch, branchChild)

        assertFalse("l2" in ListTree.validParents("branch", all).map { it.id })
        assertTrue("l1" in ListTree.validParents("branch", all).map { it.id })
    }

    @Test
    fun `las listas en el árbol salen en orden con sus sublistas debajo`() {
        val root = list("r")
        val sub = list("s", "r")
        val other = list("o")

        val flattened = ListTree.flatten(listOf(other, root, sub))

        assertEquals(listOf("r" to 0, "s" to 1, "o" to 0), flattened.map { it.first.id to it.second })
    }

    @Test
    fun `los items y las sublistas de una lista se intercalan por posicion`() {
        val itemA = listItem("itemA", position = 0)
        val itemB = listItem("itemB", position = 2)
        val root = list("r", position = 0, items = listOf(itemA, itemB))
        val sub = list("sub", "r", position = 1)

        val rows = buildListDetailRows(root, listOf(root, sub))

        val order = rows.map { row ->
            when (row) {
                is ListDetailRow.ItemRow -> row.item.id
                is ListDetailRow.SublistItem -> row.list.id
            }
        }
        assertEquals(listOf("itemA", "sub", "itemB"), order)
    }

    @Test
    fun `una sublista replegada no muestra sus propios hijos`() {
        val childItem = listItem("child", position = 0)
        val sub = list("sub", "r", position = 0, items = listOf(childItem))
        val root = list("r", position = 0)
        val all = listOf(root, sub)

        val expanded = buildListDetailRows(root, all)
        val collapsed = buildListDetailRows(root, all, collapsedIds = setOf("sub"))

        assertEquals(2, expanded.size)
        assertEquals(1, collapsed.size)
        assertTrue(collapsed.single() is ListDetailRow.SublistItem)
    }

    @Test
    fun `el orden en vivo de un arrastre reemplaza el orden natural`() {
        val itemA = listItem("itemA", position = 0)
        val itemB = listItem("itemB", position = 1)
        val root = list("r", position = 0, items = listOf(itemA, itemB))

        val rows = buildListDetailRows(
            root,
            listOf(root),
            orderOverride = mapOf("r" to listOf("itemB", "itemA")),
        )

        val order = rows.map { (it as ListDetailRow.ItemRow).item.id }
        assertEquals(listOf("itemB", "itemA"), order)
    }
}
