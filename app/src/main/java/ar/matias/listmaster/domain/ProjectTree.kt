package ar.matias.listmaster.domain

import ar.matias.listmaster.domain.model.MAX_SUBPROJECT_DEPTH
import ar.matias.listmaster.domain.model.Project
import ar.matias.listmaster.domain.model.sortRank

/**
 * Reglas del árbol de proyectos.
 *
 * La raíz es el nivel 0. Se permiten hasta [MAX_SUBPROJECT_DEPTH] niveles de
 * subproyectos debajo de la raíz (4 nodos en una cadena).
 */
object ProjectTree {

    fun byId(projects: List<Project>): Map<String, Project> = projects.associateBy { it.id }

    /** Hermanos ordenados por prioridad (Alta primero) y, a igual prioridad, por antigüedad. */
    val byPriority: Comparator<Project> =
        compareBy<Project>({ it.priority.sortRank }, { it.createdAt }, { it.id })

    fun childrenOf(parentId: String?, projects: List<Project>): List<Project> =
        projects
            .filter { it.parentId == parentId }
            .sortedWith(byPriority)

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

    /** Proyecto principal (nivel 0) del árbol al que pertenece [projectId]. */
    fun rootOf(projectId: String, projects: List<Project>): Project? {
        val index = byId(projects)
        var current = index[projectId] ?: return null
        val visited = mutableSetOf(projectId)
        while (true) {
            val parentId = current.parentId ?: return current
            if (!visited.add(parentId)) return null
            current = index[parentId] ?: return null
        }
    }

    /** Niveles que hay debajo del proyecto: 0 si no tiene subproyectos. */
    fun subtreeHeight(projectId: String, projects: List<Project>): Int {
        val byParent = projects.groupBy { it.parentId }
        fun height(id: String, seen: Set<String>): Int {
            val children = byParent[id].orEmpty().filter { it.id !in seen }
            return if (children.isEmpty()) 0 else 1 + children.maxOf { height(it.id, seen + id) }
        }
        return height(projectId, emptySet())
    }

    /**
     * Padres posibles para un subproyecto: el proyecto principal de su árbol y
     * cualquiera de sus subproyectos, menos él mismo y su descendencia, siempre
     * que el subárbol movido no pase de [MAX_SUBPROJECT_DEPTH]. Los proyectos
     * principales no se mueven: devuelve vacío.
     */
    fun validParents(projectId: String, projects: List<Project>): List<Project> {
        val index = byId(projects)
        val project = index[projectId] ?: return emptyList()
        if (project.parentId == null) return emptyList()
        val root = rootOf(projectId, projects) ?: return emptyList()
        val own = subtreeIds(projectId, projects)
        val height = subtreeHeight(projectId, projects)
        return subtreeIds(root.id, projects)
            .asSequence()
            .filter { it !in own }
            .mapNotNull { index[it] }
            .filter { depthOf(it.id, projects) + 1 + height <= MAX_SUBPROJECT_DEPTH }
            .sortedWith(compareBy({ depthOf(it.id, projects) }, { it.createdAt }, { it.id }))
            .toList()
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
