package ar.matias.listmaster.domain.model

import java.time.Instant
import java.time.LocalDate

const val MAX_TITLE_LENGTH = 120
const val MAX_ITEM_TITLE_LENGTH = 200
const val MAX_SUBPROJECT_DEPTH = 3
const val MAX_ICON_LENGTH = 32
const val MAX_DESCRIPTION_LENGTH = 2000

enum class ProjectStatus(val storedValue: String) {
    ACTIVE("Activo"),
    PAUSED("Pausa"),
    POSTPONED("Postergado"),
    DONE("Terminado"),
    CANCELLED("Cancelado"),
    ;

    val isClosed: Boolean get() = this == DONE || this == CANCELLED

    companion object {
        fun fromStored(value: String): ProjectStatus? =
            entries.firstOrNull { it.storedValue == value }
    }
}

enum class Priority(val storedValue: String) {
    HIGH("Alta"),
    MEDIUM("Media"),
    LOW("Baja"),
    ;

    companion object {
        fun fromStored(value: String): Priority? =
            entries.firstOrNull { it.storedValue == value }
    }
}

/** Orden de los proyectos: Alta, Media, Baja y al final los que no tienen prioridad. */
val Priority?.sortRank: Int
    get() = when (this) {
        Priority.HIGH -> 0
        Priority.MEDIUM -> 1
        Priority.LOW -> 2
        null -> 3
    }

data class Project(
    val id: String,
    val title: String,
    val status: ProjectStatus,
    val parentId: String?,
    val priority: Priority?,
    val createdAt: Instant,
    val updatedAt: Instant,
    /** Clave de un ícono incluido ("star", "bolt"…) o un emoji. Null = predeterminado. */
    val icon: String? = null,
)

data class Task(
    val id: String,
    val title: String,
    val done: Boolean,
    val projectId: String,
    val dueDate: LocalDate?,
    val createdAt: Instant,
    val updatedAt: Instant,
    /** Texto libre que solo se ve al abrir la tarea. */
    val description: String = "",
    /** Solo se ve y se edita al abrir la tarea; define el orden dentro del proyecto. */
    val priority: Priority? = null,
)

/** Alta primero, después Media, Baja y sin prioridad; a igual prioridad, la más antigua. */
val TaskOrder: Comparator<Task> =
    compareBy<Task>({ it.priority.sortRank }, { it.createdAt }, { it.id })

data class TaskList(
    val id: String,
    val title: String,
    val projectId: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val items: List<ListItem>,
)

data class ListItem(
    val id: String,
    val title: String,
    val done: Boolean,
    val position: Int,
)

/** Todo lo que la app guarda y exporta. */
data class AppData(
    val projects: List<Project>,
    val tasks: List<Task>,
    val lists: List<TaskList>,
) {
    companion object {
        val EMPTY = AppData(emptyList(), emptyList(), emptyList())
    }
}
