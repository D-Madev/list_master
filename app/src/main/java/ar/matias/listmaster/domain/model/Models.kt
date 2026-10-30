package ar.matias.listmaster.domain.model

import java.time.Instant
import java.time.LocalDate

const val MAX_TITLE_LENGTH = 120
const val MAX_ITEM_TITLE_LENGTH = 200
const val MAX_SUBPROJECT_DEPTH = 3
const val MAX_ICON_LENGTH = 32

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
)

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
