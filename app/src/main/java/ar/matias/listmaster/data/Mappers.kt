package ar.matias.listmaster.data

import ar.matias.listmaster.data.local.ListEntity
import ar.matias.listmaster.data.local.ListItemEntity
import ar.matias.listmaster.data.local.ProjectEntity
import ar.matias.listmaster.data.local.TaskEntity
import ar.matias.listmaster.domain.model.ListItem
import ar.matias.listmaster.domain.model.Priority
import ar.matias.listmaster.domain.model.Project
import ar.matias.listmaster.domain.model.ProjectStatus
import ar.matias.listmaster.domain.model.Task
import ar.matias.listmaster.domain.model.TaskList
import java.time.Instant
import java.time.LocalDate

fun ProjectEntity.toDomain(): Project? {
    val projectStatus = ProjectStatus.fromStored(status) ?: return null
    return Project(
        id = id,
        title = title,
        status = projectStatus,
        parentId = parentId,
        priority = priority?.let(Priority::fromStored),
        createdAt = Instant.parse(createdAt),
        updatedAt = Instant.parse(updatedAt),
        icon = icon,
    )
}

fun Project.toEntity(): ProjectEntity = ProjectEntity(
    id = id,
    title = title,
    status = status.storedValue,
    parentId = parentId,
    priority = priority?.storedValue,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    icon = icon,
)

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    title = title,
    done = done,
    projectId = projectId,
    dueDate = dueDate?.let(LocalDate::parse),
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    done = done,
    projectId = projectId,
    dueDate = dueDate?.toString(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun ListEntity.toDomain(items: List<ListItemEntity>): TaskList = TaskList(
    id = id,
    title = title,
    projectId = projectId,
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
    items = items
        .sortedWith(compareBy({ it.position }, { it.id }))
        .map { it.toDomain() },
)

fun TaskList.toEntity(): ListEntity = ListEntity(
    id = id,
    title = title,
    projectId = projectId,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun ListItemEntity.toDomain(): ListItem = ListItem(
    id = id,
    title = title,
    done = done,
    position = position,
)

fun ListItem.toEntity(listId: String): ListItemEntity = ListItemEntity(
    id = id,
    listId = listId,
    title = title,
    done = done,
    position = position,
)
