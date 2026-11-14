package ar.matias.listmaster.data

import androidx.room.withTransaction
import ar.matias.listmaster.data.local.ListItemEntity
import ar.matias.listmaster.data.local.ListMasterDatabase
import ar.matias.listmaster.domain.ListTree
import ar.matias.listmaster.domain.ProjectTree
import ar.matias.listmaster.domain.Validation
import ar.matias.listmaster.domain.model.AppData
import ar.matias.listmaster.domain.model.ListItem
import ar.matias.listmaster.domain.model.MAX_SUBLIST_DEPTH
import ar.matias.listmaster.domain.model.MAX_SUBPROJECT_DEPTH
import ar.matias.listmaster.domain.model.Priority
import ar.matias.listmaster.domain.model.Project
import ar.matias.listmaster.domain.model.ProjectStatus
import ar.matias.listmaster.domain.model.Task
import ar.matias.listmaster.domain.model.TaskList
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ListMasterRepository(
    private val db: ListMasterDatabase,
    private val now: () -> Instant = { Instant.now().truncatedTo(ChronoUnit.MILLIS) },
) {
    private val projectDao = db.projectDao()
    private val taskDao = db.taskDao()
    private val listDao = db.listDao()
    private val itemDao = db.listItemDao()

    fun observeData(): Flow<AppData> = combine(
        projectDao.observeAll(),
        taskDao.observeAll(),
        listDao.observeAll(),
        itemDao.observeAll(),
    ) { projects, tasks, lists, items ->
        val itemsByList = items.groupBy { it.listId }
        AppData(
            projects = projects.mapNotNull { it.toDomain() },
            tasks = tasks.map { it.toDomain() },
            lists = lists.map { it.toDomain(itemsByList[it.id].orEmpty()) },
        )
    }

    suspend fun snapshot(): AppData {
        val itemsByList = itemDao.getAll().groupBy { it.listId }
        return AppData(
            projects = projectDao.getAll().mapNotNull { it.toDomain() },
            tasks = taskDao.getAll().map { it.toDomain() },
            lists = listDao.getAll().map { it.toDomain(itemsByList[it.id].orEmpty()) },
        )
    }

    // Proyectos

    suspend fun createProject(
        title: String,
        parentId: String? = null,
        status: ProjectStatus = ProjectStatus.ACTIVE,
        priority: Priority? = null,
        icon: String? = null,
    ): String {
        val clean = Validation.title(title)
        if (parentId != null) {
            val projects = projectDao.getAll().mapNotNull { it.toDomain() }
            val depth = ProjectTree.depthOf(parentId, projects)
            require(depth in 0 until MAX_SUBPROJECT_DEPTH) {
                "No se pueden anidar más de tres niveles de subproyectos."
            }
        }

        val timestamp = now()
        val id = UUID.randomUUID().toString()
        projectDao.upsert(
            Project(
                id = id,
                title = clean,
                status = status,
                parentId = parentId,
                priority = priority,
                createdAt = timestamp,
                updatedAt = timestamp,
                icon = Validation.icon(icon),
            ).toEntity(),
        )
        return id
    }

    suspend fun updateProject(
        id: String,
        title: String,
        status: ProjectStatus,
        priority: Priority?,
        icon: String? = null,
    ) {
        val existing = projectDao.getById(id) ?: throw IllegalArgumentException("El proyecto ya no existe.")
        val clean = Validation.title(title)
        projectDao.upsert(
            existing.copy(
                title = clean,
                status = status.storedValue,
                priority = priority?.storedValue,
                updatedAt = now().toString(),
                icon = Validation.icon(icon),
            ),
        )
    }

    suspend fun setProjectStatus(id: String, status: ProjectStatus) {
        val existing = projectDao.getById(id) ?: return
        projectDao.upsert(existing.copy(status = status.storedValue, updatedAt = now().toString()))
    }

    suspend fun setProjectPriority(id: String, priority: Priority?) {
        val existing = projectDao.getById(id) ?: return
        projectDao.upsert(
            existing.copy(priority = priority?.storedValue, updatedAt = now().toString()),
        )
    }

    /**
     * Cambia el padre de un proyecto. newParentId null convierte el proyecto en
     * raíz independiente. Permite mover entre árboles y asignar padre a proyectos
     * que eran raíz.
     */
    suspend fun moveProject(id: String, newParentId: String?) {
        val projects = projectDao.getAll().mapNotNull { it.toDomain() }
        val project = projects.firstOrNull { it.id == id }
            ?: throw IllegalArgumentException("El proyecto ya no existe.")
        if (project.parentId == newParentId) return
        if (newParentId != null) {
            require(ProjectTree.validParents(id, projects).any { it.id == newParentId }) {
                "Ese proyecto no puede ser el nuevo padre."
            }
        }
        val existing = projectDao.getById(id) ?: return
        projectDao.upsert(existing.copy(parentId = newParentId, updatedAt = now().toString()))
    }

    suspend fun deleteProject(id: String) {
        projectDao.delete(id)
    }

    // Tareas

    suspend fun createTask(
        projectId: String,
        title: String,
        dueDate: LocalDate?,
        description: String = "",
        priority: Priority? = null,
    ): String {
        val clean = Validation.title(title)
        val cleanDescription = Validation.description(description)
        require(projectDao.getById(projectId) != null) { "El proyecto ya no existe." }
        val timestamp = now()
        val id = UUID.randomUUID().toString()
        taskDao.upsert(
            Task(
                id = id,
                title = clean,
                done = false,
                projectId = projectId,
                dueDate = dueDate,
                createdAt = timestamp,
                updatedAt = timestamp,
                description = cleanDescription,
                priority = priority,
            ).toEntity(),
        )
        return id
    }

    suspend fun setTaskDone(id: String, done: Boolean) {
        taskDao.setDone(id, done, now().toString())
    }

    suspend fun updateTask(
        id: String,
        title: String,
        dueDate: LocalDate?,
        description: String = "",
        priority: Priority? = null,
    ) {
        val existing = taskDao.getById(id) ?: throw IllegalArgumentException("La tarea ya no existe.")
        val clean = Validation.title(title)
        val cleanDescription = Validation.description(description)
        taskDao.upsert(
            existing.copy(
                title = clean,
                dueDate = dueDate?.toString(),
                description = cleanDescription.ifEmpty { null },
                priority = priority?.storedValue,
                updatedAt = now().toString(),
            ),
        )
    }

    suspend fun deleteTask(id: String) {
        taskDao.delete(id)
    }

    // Listas

    suspend fun createList(title: String, projectId: String?, parentId: String? = null): String {
        val clean = Validation.title(title)
        if (projectId != null) {
            require(projectDao.getById(projectId) != null) { "El proyecto ya no existe." }
        }
        if (parentId != null) {
            val lists = listDao.getAll().map { it.toDomain(emptyList()) }
            val depth = ListTree.depthOf(parentId, lists)
            require(depth in 0 until MAX_SUBLIST_DEPTH) {
                "No se pueden anidar más de tres niveles de listas."
            }
        }
        val timestamp = now()
        val id = UUID.randomUUID().toString()
        listDao.upsert(
            TaskList(
                id = id,
                title = clean,
                projectId = projectId,
                parentId = parentId,
                position = nextChildPosition(parentId),
                createdAt = timestamp,
                updatedAt = timestamp,
                items = emptyList(),
            ).toEntity(),
        )
        return id
    }

    /**
     * Próxima posición para un hijo (ítem o sublista) de [ownerId]: items y
     * sublistas de una misma lista comparten el mismo espacio de posiciones,
     * para poder intercalarlos al arrastrar. Con [ownerId] null es una lista
     * raíz y solo compite con otras raíces.
     */
    private suspend fun nextChildPosition(ownerId: String?): Int {
        val siblingListsMax = listDao.getAll()
            .filter { it.parentId == ownerId }
            .maxOfOrNull { it.position } ?: -1
        val siblingItemsMax = if (ownerId != null) {
            itemDao.getAll().filter { it.listId == ownerId }.maxOfOrNull { it.position } ?: -1
        } else {
            -1
        }
        return maxOf(siblingListsMax, siblingItemsMax) + 1
    }

    /**
     * Reordena los hijos pendientes de una lista: sus ítems y sus sublistas
     * comparten el mismo orden, así que [orderedPendingIds] puede mezclar
     * ambos tipos de id. Las hechas quedan siempre al final.
     */
    suspend fun reorderListChildren(ownerId: String, orderedPendingIds: List<String>) {
        val allItems = itemDao.getAll().filter { it.listId == ownerId }
        val allSublists = listDao.getAll().filter { it.parentId == ownerId }
        val pendingSet = orderedPendingIds.toSet()
        val doneItems = allItems.filter { it.id !in pendingSet }.sortedBy { it.position }
        db.withTransaction {
            orderedPendingIds.forEachIndexed { index, id ->
                if (allItems.any { it.id == id }) {
                    itemDao.updatePosition(id, index)
                } else if (allSublists.any { it.id == id }) {
                    listDao.updatePosition(id, index)
                }
            }
            doneItems.forEachIndexed { index, item ->
                itemDao.updatePosition(item.id, orderedPendingIds.size + index)
            }
        }
    }

    suspend fun updateList(id: String, title: String, projectId: String?) {
        val existing = listDao.getById(id) ?: throw IllegalArgumentException("La lista ya no existe.")
        val clean = Validation.title(title)
        if (projectId != null) {
            require(projectDao.getById(projectId) != null) { "El proyecto ya no existe." }
        }
        listDao.upsert(
            existing.copy(
                title = clean,
                projectId = projectId,
                updatedAt = now().toString(),
            ),
        )
    }

    /** Cambia el padre de una lista. newParentId null la convierte en raíz independiente. */
    suspend fun moveList(id: String, newParentId: String?) {
        val lists = listDao.getAll().map { it.toDomain(emptyList()) }
        val list = lists.firstOrNull { it.id == id }
            ?: throw IllegalArgumentException("La lista ya no existe.")
        if (list.parentId == newParentId) return
        if (newParentId != null) {
            require(ListTree.validParents(id, lists).any { it.id == newParentId }) {
                "Esa lista no puede ser la nueva lista padre."
            }
        }
        val existing = listDao.getById(id) ?: return
        listDao.upsert(existing.copy(parentId = newParentId, updatedAt = now().toString()))
    }

    suspend fun deleteList(id: String) {
        listDao.delete(id)
    }

    // Ítems de lista

    suspend fun addListItem(listId: String, title: String): String {
        val clean = Validation.itemTitle(title)
        require(listDao.getById(listId) != null) { "La lista ya no existe." }
        val position = nextChildPosition(listId)
        val id = UUID.randomUUID().toString()
        itemDao.upsert(
            ListItem(
                id = id,
                title = clean,
                done = false,
                position = position,
            ).toEntity(listId),
        )
        touchList(listId)
        return id
    }

    suspend fun setListItemDone(id: String, done: Boolean) {
        val item = itemDao.getById(id) ?: return
        itemDao.setDone(id, done)
        touchList(item.listId)
    }

    suspend fun updateListItemTitle(id: String, title: String) {
        val item = itemDao.getById(id) ?: return
        itemDao.setTitle(id, Validation.itemTitle(title))
        touchList(item.listId)
    }

    suspend fun deleteListItem(id: String) {
        val item = itemDao.getById(id) ?: return
        itemDao.delete(id)
        touchList(item.listId)
    }

    private suspend fun touchList(listId: String) {
        val list = listDao.getById(listId) ?: return
        listDao.upsert(list.copy(updatedAt = now().toString()))
    }

    // Import

    /** Reemplaza todo el contenido en una sola transacción. */
    suspend fun replaceAll(data: AppData) {
        db.withTransaction {
            itemDao.deleteAll()
            listDao.deleteAll()
            taskDao.deleteAll()
            projectDao.deleteAll()

            val orderedProjects = data.projects.sortedBy {
                ProjectTree.depthOf(it.id, data.projects).coerceAtLeast(0)
            }
            projectDao.insertAll(orderedProjects.map { it.toEntity() })
            taskDao.insertAll(data.tasks.map { it.toEntity() })

            val orderedLists = data.lists.sortedBy {
                ListTree.depthOf(it.id, data.lists).coerceAtLeast(0)
            }
            listDao.insertAll(orderedLists.map { it.toEntity() })

            val items = data.lists.flatMap { list ->
                list.items.map { item ->
                    ListItemEntity(
                        id = item.id,
                        listId = list.id,
                        title = item.title,
                        done = item.done,
                        position = item.position,
                    )
                }
            }
            itemDao.insertAll(items)
        }
    }
}
