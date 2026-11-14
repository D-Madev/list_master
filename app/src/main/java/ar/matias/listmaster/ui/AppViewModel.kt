package ar.matias.listmaster.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ar.matias.listmaster.ListMasterApplication
import ar.matias.listmaster.R
import ar.matias.listmaster.data.BackupCodec
import ar.matias.listmaster.data.BackupException
import ar.matias.listmaster.data.ThemeMode
import ar.matias.listmaster.domain.model.AppData
import ar.matias.listmaster.domain.model.Priority
import ar.matias.listmaster.domain.model.ProjectFilter
import ar.matias.listmaster.domain.model.ProjectStatus
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val container = application as ListMasterApplication
    private val repository = container.repository
    private val preferences = container.preferences

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    val data: StateFlow<AppData> = repository.observeData()
        .onEach { _loaded.value = true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppData.EMPTY,
        )

    val themeMode: StateFlow<ThemeMode> = preferences.themeMode
    val showDoneTasks: StateFlow<Boolean> = preferences.showDoneTasks
    val showDoneListItems: StateFlow<Boolean> = preferences.showDoneListItems
    val projectFilter: StateFlow<ProjectFilter> = preferences.projectFilter

    val navigation = Navigator()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun notify(message: String) {
        _messages.tryEmit(message)
    }

    private fun action(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _messages.emit(error.message?.takeIf { it.isNotBlank() } ?: "Algo salió mal.")
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        preferences.setThemeMode(mode)
    }

    fun setShowDoneTasks(show: Boolean) {
        preferences.setShowDoneTasks(show)
    }

    fun setShowDoneListItems(show: Boolean) {
        preferences.setShowDoneListItems(show)
    }

    fun setProjectFilter(filter: ProjectFilter) {
        preferences.setProjectFilter(filter)
    }

    // Respaldo

    fun exportTo(uri: Uri, onFinished: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = runCatching {
                val json = BackupCodec.encode(repository.snapshot())
                withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openOutputStream(uri)
                        ?: throw IllegalStateException("No se pudo abrir el archivo.")
                    stream.use { output ->
                        output.write(json.toByteArray(Charsets.UTF_8))
                    }
                }
            }
            result.onSuccess {
                val name = displayName(uri) ?: "el archivo"
                _messages.emit(context.getString(R.string.export_success, name))
                onFinished(true)
            }.onFailure {
                _messages.emit(context.getString(R.string.export_error))
                onFinished(false)
            }
        }
    }

    fun importFrom(uri: Uri, onFinished: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val result = runCatching {
                val text = withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openInputStream(uri)
                        ?: throw BackupException("No se pudo leer el archivo.")
                    stream.use { input ->
                        input.readBytes().toString(Charsets.UTF_8)
                    }
                }
                val data = BackupCodec.decode(text)
                repository.replaceAll(data)
                data
            }
            result.onSuccess { data ->
                val empty = data.projects.isEmpty() && data.tasks.isEmpty() && data.lists.isEmpty()
                _messages.emit(
                    context.getString(
                        if (empty) R.string.import_empty_warning else R.string.import_success,
                    ),
                )
                onFinished(true)
            }.onFailure { error ->
                _messages.emit(
                    if (error is BackupException && !error.message.isNullOrBlank()) {
                        error.message!!
                    } else {
                        context.getString(R.string.import_error)
                    },
                )
                onFinished(false)
            }
        }
    }

    private fun displayName(uri: Uri): String? = runCatching {
        val context = getApplication<Application>()
        context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else {
                    null
                }
            }
    }.getOrNull()

    // Navegación

    fun switchTab(screen: Screen) {
        navigation.switchTab(screen)
    }

    fun back() {
        navigation.pop()
    }

    fun openProject(projectId: String) {
        navigation.push(Screen.ProjectDetail(projectId))
    }

    fun newProject(parentId: String? = null) {
        navigation.push(Screen.ProjectForm(parentId = parentId))
    }

    fun editProject(projectId: String) {
        navigation.push(Screen.ProjectForm(parentId = null, projectId = projectId))
    }

    fun openList(listId: String) {
        navigation.push(Screen.ListDetail(listId))
    }

    fun newList(parentId: String? = null) {
        navigation.push(Screen.ListForm(parentId = parentId))
    }

    fun editList(listId: String) {
        navigation.push(Screen.ListForm(listId = listId))
    }

    // Proyectos

    fun createProject(
        title: String,
        parentId: String?,
        status: ProjectStatus,
        priority: Priority?,
        icon: String?,
    ) {
        action {
            repository.createProject(
                title = title,
                parentId = parentId,
                status = status,
                priority = priority,
                icon = icon,
            )
            navigation.pop()
        }
    }

    fun updateProject(
        id: String,
        title: String,
        status: ProjectStatus,
        priority: Priority?,
        icon: String?,
        newParentId: String? = null,
    ) {
        action {
            val currentParentId = data.value.projects.firstOrNull { it.id == id }?.parentId
            if (currentParentId != newParentId) repository.moveProject(id, newParentId)
            repository.updateProject(id, title, status, priority, icon)
            navigation.pop()
        }
    }

    fun setProjectStatus(id: String, status: ProjectStatus) {
        action { repository.setProjectStatus(id, status) }
    }

    fun setProjectPriority(id: String, priority: Priority?) {
        action { repository.setProjectPriority(id, priority) }
    }

    fun deleteProject(id: String) {
        action {
            repository.deleteProject(id)
            navigation.popToRoot()
        }
    }

    // Tareas

    fun createTask(
        projectId: String,
        title: String,
        dueDate: LocalDate?,
        description: String = "",
        priority: Priority? = null,
        onSaved: () -> Unit = {},
    ) {
        action {
            repository.createTask(projectId, title, dueDate, description, priority)
            onSaved()
        }
    }

    fun updateTask(
        id: String,
        title: String,
        dueDate: LocalDate?,
        description: String = "",
        priority: Priority? = null,
        onSaved: () -> Unit = {},
    ) {
        action {
            repository.updateTask(id, title, dueDate, description, priority)
            onSaved()
        }
    }

    fun setTaskDone(id: String, done: Boolean) {
        action { repository.setTaskDone(id, done) }
    }

    fun deleteTask(id: String) {
        action { repository.deleteTask(id) }
    }

    // Listas

    fun createList(title: String, projectId: String?, parentId: String? = null) {
        action {
            repository.createList(title, projectId, parentId)
            navigation.pop()
        }
    }

    fun updateList(id: String, title: String, projectId: String?, newParentId: String?) {
        action {
            val currentParentId = data.value.lists.firstOrNull { it.id == id }?.parentId
            if (currentParentId != newParentId) repository.moveList(id, newParentId)
            repository.updateList(id, title, projectId)
            navigation.pop()
        }
    }

    fun deleteList(id: String) {
        action {
            repository.deleteList(id)
            navigation.popToRoot()
        }
    }

    fun addListItem(listId: String, title: String) {
        action { repository.addListItem(listId, title) }
    }

    fun setListItemDone(id: String, done: Boolean) {
        action { repository.setListItemDone(id, done) }
    }

    fun updateListItemTitle(id: String, title: String) {
        action { repository.updateListItemTitle(id, title) }
    }

    fun deleteListItem(id: String) {
        action { repository.deleteListItem(id) }
    }

    fun reorderListItems(listId: String, orderedIds: List<String>) {
        action { repository.reorderListItems(listId, orderedIds) }
    }
}
