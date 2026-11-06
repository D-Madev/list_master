package ar.matias.listmaster.domain

import ar.matias.listmaster.domain.model.Project
import ar.matias.listmaster.domain.model.Task
import ar.matias.listmaster.domain.model.TaskOrder

/** Un proyecto con sus tareas pendientes dentro de la vista general. */
data class PendingGroup(
    val project: Project,
    /** 0 para un proyecto principal, 1..3 para subproyectos. */
    val depth: Int,
    /** Solo las tareas propias del proyecto, no las de sus subproyectos. */
    val tasks: List<Task>,
)

/**
 * Todo lo pendiente en una sola lista: cada proyecto principal seguido de sus
 * subproyectos, ordenados por prioridad. Quedan afuera los proyectos
 * terminados o cancelados y los que no tienen nada pendiente (un subproyecto
 * sin tareas propias igual aparece si algo más abajo tiene pendientes, para
 * no perder la jerarquía).
 */
object PendingTasks {

    fun groups(projects: List<Project>, tasks: List<Task>): List<PendingGroup> {
        val childrenByParent = projects.groupBy { it.parentId }
        val pendingByProject = tasks
            .filter { !it.done }
            .groupBy { it.projectId }

        val result = mutableListOf<PendingGroup>()

        // Devuelve true si el subárbol aportó algo; si no, se descarta el encabezado.
        fun visit(project: Project, depth: Int, visited: Set<String>): Boolean {
            if (project.status.isClosed) return false
            val mark = result.size
            val own = pendingByProject[project.id]
                .orEmpty()
                .sortedWith(TaskOrder)
            result.add(PendingGroup(project, depth, own))

            var childHadPending = false
            childrenByParent[project.id].orEmpty()
                .filter { it.id !in visited }
                .sortedWith(ProjectTree.byPriority)
                .forEach { child ->
                    if (visit(child, depth + 1, visited + project.id)) childHadPending = true
                }

            if (own.isEmpty() && !childHadPending) {
                while (result.size > mark) result.removeAt(result.lastIndex)
                return false
            }
            return true
        }

        childrenByParent[null].orEmpty()
            .sortedWith(ProjectTree.byPriority)
            .forEach { visit(it, 0, emptySet()) }
        return result
    }

    fun count(groups: List<PendingGroup>): Int = groups.sumOf { it.tasks.size }
}
