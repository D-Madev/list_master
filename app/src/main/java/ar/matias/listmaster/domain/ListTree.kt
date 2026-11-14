package ar.matias.listmaster.domain

import ar.matias.listmaster.domain.model.MAX_SUBLIST_DEPTH
import ar.matias.listmaster.domain.model.TaskList

/**
 * Reglas del árbol de listas. Es el mismo esquema que [ProjectTree] pero para
 * listas: la raíz es el nivel 0 y se permiten hasta [MAX_SUBLIST_DEPTH]
 * niveles de sublistas debajo. Los hermanos se ordenan por su posición propia
 * (la que se arrastra en el detalle), y a igual posición, por antigüedad.
 */
object ListTree {

    fun byId(lists: List<TaskList>): Map<String, TaskList> = lists.associateBy { it.id }

    val byPosition: Comparator<TaskList> = compareBy({ it.position }, { it.createdAt }, { it.id })

    fun childrenOf(parentId: String?, lists: List<TaskList>): List<TaskList> =
        lists
            .filter { it.parentId == parentId }
            .sortedWith(byPosition)

    /** Nivel de la lista: 0 para una raíz, 1..3 para sublistas. -1 si no existe o hay ciclo. */
    fun depthOf(listId: String, lists: List<TaskList>): Int {
        val index = byId(lists)
        var current = index[listId] ?: return -1
        var depth = 0
        val visited = mutableSetOf(listId)

        while (true) {
            val parentId = current.parentId ?: return depth
            val parent = index[parentId] ?: return -1
            if (!visited.add(parentId)) return -1
            depth++
            current = parent
        }
    }

    fun canHaveSublist(listId: String, lists: List<TaskList>): Boolean {
        val depth = depthOf(listId, lists)
        return depth in 0 until MAX_SUBLIST_DEPTH
    }

    /** IDs de la lista y de todas sus descendientes. */
    fun subtreeIds(rootId: String, lists: List<TaskList>): Set<String> {
        val index = byId(lists)
        if (rootId !in index) return emptySet()

        val result = mutableSetOf<String>()
        val pending = ArrayDeque<String>()
        pending.add(rootId)

        while (pending.isNotEmpty()) {
            val id = pending.removeFirst()
            if (!result.add(id)) continue
            index[id]?.let { list ->
                childrenOf(list.id, lists).forEach { pending.add(it.id) }
            }
        }
        return result
    }

    /** Niveles que hay debajo de la lista: 0 si no tiene sublistas. */
    fun subtreeHeight(listId: String, lists: List<TaskList>): Int {
        val byParent = lists.groupBy { it.parentId }
        fun height(id: String, seen: Set<String>): Int {
            val children = byParent[id].orEmpty().filter { it.id !in seen }
            return if (children.isEmpty()) 0 else 1 + children.maxOf { height(it.id, seen + id) }
        }
        return height(listId, emptySet())
    }

    /**
     * Padres posibles para una lista: cualquier lista fuera de su propio
     * subárbol, siempre que el subárbol movido no pase de [MAX_SUBLIST_DEPTH].
     * Permite mover entre árboles y convertir una lista raíz en sublista.
     */
    fun validParents(listId: String, lists: List<TaskList>): List<TaskList> {
        val index = byId(lists)
        index[listId] ?: return emptyList()
        val own = subtreeIds(listId, lists)
        val height = subtreeHeight(listId, lists)
        return lists
            .asSequence()
            .filter { it.id !in own }
            .filter { depthOf(it.id, lists) + 1 + height <= MAX_SUBLIST_DEPTH }
            .sortedWith(compareBy({ depthOf(it.id, lists) }, { it.position }, { it.createdAt }, { it.id }))
            .toList()
    }

    /** Listas en orden de árbol (raíces y sus sublistas debajo), con su nivel. */
    fun flatten(lists: List<TaskList>): List<Pair<TaskList, Int>> {
        val result = mutableListOf<Pair<TaskList, Int>>()
        fun visit(parentId: String?, depth: Int) {
            childrenOf(parentId, lists).forEach { list ->
                result.add(list to depth)
                visit(list.id, depth + 1)
            }
        }
        visit(null, 0)
        // Por si alguna lista quedó con un padre inexistente, igual se ve.
        val seen = result.map { it.first.id }.toSet()
        lists.filter { it.id !in seen }.forEach { result.add(it to 0) }
        return result
    }
}
