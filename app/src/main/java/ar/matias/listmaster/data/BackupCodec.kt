package ar.matias.listmaster.data

import ar.matias.listmaster.domain.ProjectTree
import ar.matias.listmaster.domain.model.AppData
import ar.matias.listmaster.domain.model.ListItem
import ar.matias.listmaster.domain.model.MAX_ICON_LENGTH
import ar.matias.listmaster.domain.model.MAX_ITEM_TITLE_LENGTH
import ar.matias.listmaster.domain.model.MAX_SUBPROJECT_DEPTH
import ar.matias.listmaster.domain.model.MAX_TITLE_LENGTH
import ar.matias.listmaster.domain.model.Priority
import ar.matias.listmaster.domain.model.Project
import ar.matias.listmaster.domain.model.ProjectStatus
import ar.matias.listmaster.domain.model.Task
import ar.matias.listmaster.domain.model.TaskList
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Export/import JSON con nombres de campo en español.
 *
 * Se arma a mano en vez de serializar las clases de dominio para que el
 * archivo no cambie si se renombra un identificador de Kotlin. El orden de los
 * campos y de las entidades es determinista: el mismo estado produce el mismo
 * archivo.
 */
object BackupCodec {

    const val VERSION = 1
    const val APPLICATION = "list_master"

    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    fun encode(data: AppData, exportedAt: Instant = Instant.now()): String {
        val root = JsonObject()
        root.addProperty("version", VERSION)
        root.addProperty("aplicacion", APPLICATION)
        root.addProperty("exportado", exportedAt.toString())

        val projects = data.projects.sortedWith(compareBy({ it.createdAt }, { it.id }))
        val tasks = data.tasks.sortedWith(compareBy({ it.createdAt }, { it.id }))
        val lists = data.lists.sortedWith(compareBy({ it.createdAt }, { it.id }))

        root.add("proyectos", JsonArray().apply {
            projects.forEach { project ->
                add(JsonObject().apply {
                    addProperty("id", project.id)
                    addProperty("titulo", project.title)
                    addProperty("estado", project.status.storedValue)
                    addProperty("proyecto_padre", project.parentId)
                    addProperty("prioridad", project.priority?.storedValue)
                    addProperty("icono", project.icon)
                    addProperty("creado", project.createdAt.toString())
                    addProperty("actualizado", project.updatedAt.toString())
                })
            }
        })

        root.add("tareas", JsonArray().apply {
            tasks.forEach { task ->
                add(JsonObject().apply {
                    addProperty("id", task.id)
                    addProperty("titulo", task.title)
                    addProperty("hecha", task.done)
                    addProperty("proyecto_id", task.projectId)
                    addProperty("fecha", task.dueDate?.toString())
                    addProperty("creado", task.createdAt.toString())
                    addProperty("actualizado", task.updatedAt.toString())
                })
            }
        })

        root.add("listas", JsonArray().apply {
            lists.forEach { list ->
                add(JsonObject().apply {
                    addProperty("id", list.id)
                    addProperty("titulo", list.title)
                    addProperty("proyecto_id", list.projectId)
                    addProperty("creado", list.createdAt.toString())
                    addProperty("actualizado", list.updatedAt.toString())
                    add("items", JsonArray().apply {
                        list.items
                            .sortedWith(compareBy({ it.position }, { it.id }))
                            .forEach { item ->
                                add(JsonObject().apply {
                                    addProperty("id", item.id)
                                    addProperty("titulo", item.title)
                                    addProperty("hecha", item.done)
                                    addProperty("orden", item.position)
                                })
                            }
                    })
                })
            }
        })

        return gson.toJson(root) + "\n"
    }

    fun decode(rawText: String): AppData {
        val text = normalize(rawText)
        if (text.isBlank()) throw BackupException("El archivo está vacío.")

        val root = try {
            JsonParser.parseString(text)
        } catch (error: Exception) {
            throw BackupException("El archivo no es un JSON válido.", error)
        }
        if (!root.isJsonObject) {
            throw BackupException("El archivo no parece un export de List Master.")
        }
        val obj = root.asJsonObject

        val application = obj.optionalString("aplicacion")
        if (application != APPLICATION) {
            throw BackupException("El archivo no parece un export de List Master.")
        }

        val version = obj.optionalInt("version")
            ?: throw BackupException("Al archivo le falta la versión.")
        if (version > VERSION) {
            throw BackupException("Este archivo es de una versión más nueva de List Master.")
        }
        if (version != VERSION) {
            throw BackupException("El archivo no parece un export de List Master.")
        }

        val ids = mutableSetOf<String>()

        val projectObjects = obj.requireArray("proyectos")
        val projects = projectObjects.mapIndexed { index, element ->
            parseProject(element, index, ids)
        }

        val projectIds = projects.map { it.id }.toSet()
        projects.forEach { project ->
            if (project.parentId != null && project.parentId !in projectIds) {
                throw BackupException("Un subproyecto apunta a un proyecto que no existe.")
            }
            val depth = ProjectTree.depthOf(project.id, projects)
            if (depth < 0) {
                throw BackupException("Los proyectos tienen un ciclo o una relación inválida.")
            }
            if (depth > MAX_SUBPROJECT_DEPTH) {
                throw BackupException("Hay subproyectos anidados más de tres niveles.")
            }
        }

        val tasks = obj.requireArray("tareas").mapIndexed { index, element ->
            parseTask(element, index, ids, projectIds)
        }

        val lists = obj.requireArray("listas").mapIndexed { index, element ->
            parseList(element, index, ids, projectIds)
        }

        return AppData(projects = projects, tasks = tasks, lists = lists)
    }

    /**
     * Quita el BOM y, si el archivo llegó envuelto en un bloque de código o con
     * texto alrededor, se queda con el objeto JSON que hay adentro. El contenido
     * sigue validándose entero después.
     */
    private fun normalize(rawText: String): String {
        val candidate = rawText.removePrefix("\uFEFF").trim()
        if (candidate.startsWith("{")) return candidate
        val start = candidate.indexOf('{')
        val end = candidate.lastIndexOf('}')
        return if (start >= 0 && end > start) candidate.substring(start, end + 1) else candidate
    }

    private fun parseProject(
        element: JsonElement,
        index: Int,
        ids: MutableSet<String>,
    ): Project {
        val obj = element.requireObject("proyecto ${index + 1}")
        val id = obj.requireId(ids, "proyecto ${index + 1}")
        val title = obj.requireTitle("proyecto ${index + 1}", MAX_TITLE_LENGTH)
        val status = obj.optionalString("estado")
            ?.let(ProjectStatus::fromStored)
            ?: throw BackupException("El proyecto «$title» tiene un estado inválido.")
        val priority = obj.optionalString("prioridad")?.let { value ->
            Priority.fromStored(value)
                ?: throw BackupException("El proyecto «$title» tiene una prioridad inválida.")
        }
        val icon = obj.optionalString("icono")?.trim()?.takeIf { it.isNotEmpty() }?.also { value ->
            if (value.length > MAX_ICON_LENGTH) {
                throw BackupException("El proyecto «$title» tiene un ícono demasiado largo.")
            }
        }
        val createdAt = obj.requireInstant("creado", "proyecto «$title»")
        val updatedAt = obj.optionalInstant("actualizado") ?: createdAt
        return Project(
            id = id,
            title = title,
            status = status,
            parentId = obj.optionalString("proyecto_padre")?.also { parent ->
                validateUuid(parent, "proyecto «$title»")
            },
            priority = priority,
            createdAt = createdAt,
            updatedAt = updatedAt,
            icon = icon,
        )
    }

    private fun parseTask(
        element: JsonElement,
        index: Int,
        ids: MutableSet<String>,
        projectIds: Set<String>,
    ): Task {
        val obj = element.requireObject("tarea ${index + 1}")
        val id = obj.requireId(ids, "tarea ${index + 1}")
        val title = obj.requireTitle("tarea ${index + 1}", MAX_TITLE_LENGTH)
        val done = obj.requireBoolean("hecha", "tarea «$title»")
        val projectId = obj.optionalString("proyecto_id")
            ?: throw BackupException("La tarea «$title» no tiene proyecto.")
        validateUuid(projectId, "tarea «$title»")
        if (projectId !in projectIds) {
            throw BackupException("La tarea «$title» apunta a un proyecto que no existe.")
        }
        val dueDate = obj.optionalString("fecha")?.let { raw ->
            try {
                LocalDate.parse(raw)
            } catch (error: Exception) {
                throw BackupException("La tarea «$title» tiene una fecha inválida.", error)
            }
        }
        val createdAt = obj.requireInstant("creado", "tarea «$title»")
        val updatedAt = obj.optionalInstant("actualizado") ?: createdAt
        return Task(
            id = id,
            title = title,
            done = done,
            projectId = projectId,
            dueDate = dueDate,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
    }

    private fun parseList(
        element: JsonElement,
        index: Int,
        ids: MutableSet<String>,
        projectIds: Set<String>,
    ): TaskList {
        val obj = element.requireObject("lista ${index + 1}")
        val id = obj.requireId(ids, "lista ${index + 1}")
        val title = obj.requireTitle("lista ${index + 1}", MAX_TITLE_LENGTH)
        val projectId = obj.optionalString("proyecto_id")?.also { value ->
            validateUuid(value, "lista «$title»")
            if (value !in projectIds) {
                throw BackupException("La lista «$title» apunta a un proyecto que no existe.")
            }
        }
        val createdAt = obj.requireInstant("creado", "lista «$title»")
        val updatedAt = obj.optionalInstant("actualizado") ?: createdAt

        val itemObjects = obj.requireArray("items")
        val items = itemObjects.mapIndexed { itemIndex, itemElement ->
            val itemObj = itemElement.requireObject("ítem ${itemIndex + 1} de «$title»")
            val itemId = itemObj.requireId(ids, "ítem ${itemIndex + 1} de «$title»")
            val itemTitle = itemObj.requireTitle(
                "ítem ${itemIndex + 1} de «$title»",
                MAX_ITEM_TITLE_LENGTH,
            )
            ListItem(
                id = itemId,
                title = itemTitle,
                done = itemObj.requireBoolean("hecha", "ítem «$itemTitle»"),
                position = itemObj.optionalInt("orden") ?: itemIndex,
            )
        }

        return TaskList(
            id = id,
            title = title,
            projectId = projectId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            items = items,
        )
    }

    // Helpers de validación

    private fun JsonElement.requireObject(label: String): JsonObject {
        if (!isJsonObject) throw BackupException("El $label está dañado.")
        return asJsonObject
    }

    private fun JsonObject.requireArray(name: String): JsonArray {
        val element = get(name)
        if (element == null || element.isJsonNull || !element.isJsonArray) {
            throw BackupException("Al archivo le falta la lista «$name».")
        }
        return element.asJsonArray
    }

    private fun JsonObject.optionalString(name: String): String? {
        val element = get(name) ?: return null
        if (element.isJsonNull) return null
        if (!element.isJsonPrimitive || !element.asJsonPrimitive.isString) {
            throw BackupException("El campo «$name» tiene un tipo inválido.")
        }
        return element.asString
    }

    private fun JsonObject.optionalInt(name: String): Int? {
        val element = get(name) ?: return null
        if (element.isJsonNull) return null
        if (!element.isJsonPrimitive || !element.asJsonPrimitive.isNumber) {
            throw BackupException("El campo «$name» tiene un tipo inválido.")
        }
        return try {
            element.asInt
        } catch (error: Exception) {
            throw BackupException("El campo «$name» tiene un tipo inválido.", error)
        }
    }

    private fun JsonObject.requireBoolean(name: String, label: String): Boolean {
        val element = get(name)
        if (element == null || element.isJsonNull ||
            !element.isJsonPrimitive || !element.asJsonPrimitive.isBoolean
        ) {
            throw BackupException("Al $label le falta el campo «$name».")
        }
        return element.asBoolean
    }

    private fun JsonObject.requireTitle(label: String, maxLength: Int): String {
        val raw = optionalString("titulo")
            ?: throw BackupException("Al $label le falta el título.")
        val clean = raw.trim()
        if (clean.isEmpty()) throw BackupException("El $label tiene el título vacío.")
        if (clean.length > maxLength) {
            throw BackupException("El $label supera los $maxLength caracteres.")
        }
        return clean
    }

    private fun JsonObject.requireId(ids: MutableSet<String>, label: String): String {
        val id = optionalString("id")
            ?: throw BackupException("Al $label le falta el id.")
        validateUuid(id, label)
        if (!ids.add(id)) {
            throw BackupException("El id «$id» está repetido en el archivo.")
        }
        return id
    }

    private fun JsonObject.requireInstant(name: String, label: String): Instant =
        optionalInstant(name) ?: throw BackupException("Al $label le falta la fecha «$name».")

    private fun JsonObject.optionalInstant(name: String): Instant? =
        optionalString(name)?.let { raw ->
            try {
                Instant.parse(raw)
            } catch (error: Exception) {
                throw BackupException("El campo «$name» no es una fecha ISO-8601 válida.", error)
            }
        }

    private fun validateUuid(value: String, label: String) {
        try {
            UUID.fromString(value)
        } catch (error: Exception) {
            throw BackupException("El $label tiene un id que no es un UUID válido.", error)
        }
    }
}
