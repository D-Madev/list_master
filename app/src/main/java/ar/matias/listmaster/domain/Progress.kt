package ar.matias.listmaster.domain

import ar.matias.listmaster.domain.model.Project
import ar.matias.listmaster.domain.model.ProjectStatus
import ar.matias.listmaster.domain.model.Task
import kotlin.math.roundToInt

/**
 * Progreso de un proyecto: tareas hechas sobre tareas totales del árbol.
 *
 * No se guarda en la base. Las listas simples no cuentan. Los subárboles
 * cancelados no cuentan. Si no hay ninguna tarea, no hay progreso: la UI dice
 * "Sin tareas todavía", no 0%.
 */
data class ProjectProgress(
    val done: Int,
    val total: Int,
) {
    val hasTasks: Boolean get() = total > 0
    val isComplete: Boolean get() = hasTasks && done == total

    val percentage: Int
        get() = when {
            !hasTasks -> 0
            isComplete -> 100
            // 199 de 200 redondea a 100, pero mientras falte una tarea se topa en 99.
            else -> ((done * 100.0) / total).roundToInt().coerceAtMost(99)
        }
}

object ProgressCalculator {

    fun calculate(
        rootId: String,
        projects: List<Project>,
        tasks: List<Task>,
    ): ProjectProgress {
        val root = projects.firstOrNull { it.id == rootId } ?: return ProjectProgress(0, 0)
        if (root.status == ProjectStatus.CANCELLED) return ProjectProgress(0, 0)

        val included = includedIds(root, projects)
        val scopedTasks = tasks.filter { it.projectId in included }

        return ProjectProgress(
            done = scopedTasks.count { it.done },
            total = scopedTasks.size,
        )
    }

    /** IDs del árbol que suman progreso: se corta en cada subárbol cancelado. */
    private fun includedIds(root: Project, projects: List<Project>): Set<String> {
        val byParent = projects.groupBy { it.parentId }
        val included = mutableSetOf<String>()
        val pending = ArrayDeque<Project>()
        pending.add(root)

        while (pending.isNotEmpty()) {
            val project = pending.removeFirst()
            if (project.status == ProjectStatus.CANCELLED) continue
            if (!included.add(project.id)) continue
            byParent[project.id].orEmpty().forEach { pending.add(it) }
        }
        return included
    }
}
