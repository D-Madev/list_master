package ar.matias.listmaster.domain

import ar.matias.listmaster.domain.model.MAX_SUBPROJECT_DEPTH
import ar.matias.listmaster.domain.model.Project

/**
 * Reglas del árbol de proyectos.
 *
 * La raíz es el nivel 0. Se permiten hasta [MAX_SUBPROJECT_DEPTH] niveles de
 * subproyectos debajo de la raíz (4 nodos en una cadena).
 */
object ProjectTree {

    fun byId(projects: List<Project>): Map<String, Project> = projects.associateBy { it.id }

    fun childrenOf(parentId: String?, projects: List<Project>): List<Project> =
        projects
            .filter { it.parentId == parentId }
            .sortedWith(compareBy({ it.createdAt }, { it.id }))

    /**
     * Nivel del proyecto: 0 para una raíz, 1..3 para subproyectos.
     * Devuelve -1 si no existe o si hay un ciclo.
     */
    fun depthOf(projectId: String, projects: List<Project>): Int {
        val index = byId(projects)
        var current = index[projectId] ?: return -1
        var depth = 0
        val visited = mutableSetOf(projectId)

        while (true) {
            val parentId = current.parentId ?: return depth
            val parent = index[parentId] ?: return -1
            if (!visited.add(parentId)) return -1
            depth++
            current = parent
        }
    }

    fun canHaveSubproject(projectId: String, projects: List<Project>): Boolean {
        val depth = depthOf(projectId, projects)
        return depth in 0 until MAX_SUBPROJECT_DEPTH
    }

    fun wouldCreateCycle(projectId: String, newParentId: String?, projects: List<Project>): Boolean {
        if (newParentId == null) return false
        if (projectId == newParentId) return true

        val index = byId(projects)
        var current = index[newParentId] ?: return false
        val visited = mutableSetOf(newParentId)

        while (true) {
            if (current.id == projectId) return true
            val parentId = current.parentId ?: return false
            if (!visited.add(parentId)) return true
            current = index[parentId] ?: return false
        }
    }

    /**
     * IDs del proyecto y de todos sus descendientes. Incluye subárboles
     * cancelados: quien decide si cuentan es el cálculo de progreso.
     */
    fun subtreeIds(rootId: String, projects: List<Project>): Set<String> {
        val index = byId(projects)
        if (rootId !in index) return emptySet()

        val result = mutableSetOf<String>()
        val pending = ArrayDeque<String>()
        pending.add(rootId)

        while (pending.isNotEmpty()) {
            val id = pending.removeFirst()
            if (!result.add(id)) continue
            index[id]?.let { project ->
                childrenOf(project.id, projects).forEach { pending.add(it.id) }
            }
        }
        return result
    }

    /** Ancestros desde la raíz hasta el padre inmediato. */
    fun ancestorsOf(projectId: String, projects: List<Project>): List<Project> {
        val index = byId(projects)
        val result = mutableListOf<Project>()
        val visited = mutableSetOf(projectId)
        var current = index[projectId] ?: return emptyList()

        while (true) {
            val parentId = current.parentId ?: break
            if (!visited.add(parentId)) break
            current = index[parentId] ?: break
            result.add(0, current)
        }
        return result
    }
}
