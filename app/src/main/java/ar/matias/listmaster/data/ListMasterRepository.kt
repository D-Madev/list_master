package ar.matias.listmaster.data

import androidx.room.withTransaction
import ar.matias.listmaster.data.local.ListItemEntity
import ar.matias.listmaster.data.local.ListMasterDatabase
import ar.matias.listmaster.domain.ProjectTree
import ar.matias.listmaster.domain.Validation
import ar.matias.listmaster.domain.model.AppData
import ar.matias.listmaster.domain.model.ListItem
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
            ).toEntity(),
        )
        return id
    }

    suspend fun updateProject(
        id: String,
        title: String,
        status: ProjectStatus,
        priority: Priority?,
    ) {
        val existing = projectDao.getById(id) ?: throw IllegalArgumentException("El proyecto ya no existe.")
        val clean = Validation.title(title)
        projectDao.upsert(
            existing.copy(
                title = clean,
                status = status.storedValue,
                priority = priority?.storedValue,
                updatedAt = now().toString(),
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

    suspend fun deleteProject(id: String) {
        projectDao.delete(id)
    }

    // Tareas

    suspend fun createTask(projectId: String, title: String, dueDate: LocalDate?): String {
        val clean = Validation.title(title)
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
            ).toEntity(),
        )
        return id
    }

    suspend fun setTaskDone(id: String, done: Boolean) {
        taskDao.setDone(id, done, now().toString())
    }

    suspend fun updateTask(id: String, title: String, dueDate: LocalDate?) {
        val existing = taskDao.getById(id) ?: throw IllegalArgumentException("La tarea ya no existe.")
        val clean = Validation.title(title)
        taskDao.upsert(
            existing.copy(
                title = clean,
                dueDate = dueDate?.toString(),
                updatedAt = now().toString(),
            ),
        )
    }

    suspend fun deleteTask(id: String) {
        taskDao.delete(id)
    }

    // Listas

    suspend fun createList(title: String, projectId: String?): String {
        val clean = Validation.title(title)
        if (projectId != null) {
            require(projectDao.getById(projectId) != null) { "El proyecto ya no existe." }
        }
        val timestamp = now()
        val id = UUID.randomUUID().toString()
        listDao.upsert(
            TaskList(
                id = id,
                title = clean,
                projectId = projectId,
                createdAt = timestamp,
                updatedAt = timestamp,
                items = emptyList(),
            ).toEntity(),
        )
        return id
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

    suspend fun deleteList(id: String) {
        listDao.delete(id)
    }

    // Ítems de lista

    suspend fun addListItem(listId: String, title: String): String {
        val clean = Validation.itemTitle(title)
        require(listDao.getById(listId) != null) { "La lista ya no existe." }
        val position = itemDao.maxPosition(listId) + 1
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
            listDao.insertAll(data.lists.map { it.toEntity() })

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
