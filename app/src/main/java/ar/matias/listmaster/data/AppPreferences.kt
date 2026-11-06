package ar.matias.listmaster.data

import android.content.Context
import ar.matias.listmaster.domain.model.Priority
import ar.matias.listmaster.domain.model.ProjectFilter
import ar.matias.listmaster.domain.model.ProjectStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}

class AppPreferences(context: Context) {

    private val preferences = context.applicationContext.getSharedPreferences(
        "list_master_settings",
        Context.MODE_PRIVATE,
    )

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _showDoneTasks = MutableStateFlow(preferences.getBoolean(KEY_SHOW_DONE, false))

    /** Si las tareas hechas se ven dentro de los proyectos. Por defecto, ocultas. */
    val showDoneTasks: StateFlow<Boolean> = _showDoneTasks.asStateFlow()

    private val _showDoneListItems =
        MutableStateFlow(preferences.getBoolean(KEY_SHOW_DONE_LIST_ITEMS, false))

    /** Si los ítems tildados se ven dentro de las listas. Por defecto, ocultos, igual que en los proyectos. */
    val showDoneListItems: StateFlow<Boolean> = _showDoneListItems.asStateFlow()

    private val _projectFilter = MutableStateFlow(loadProjectFilter())
    val projectFilter: StateFlow<ProjectFilter> = _projectFilter.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        preferences.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode
    }

    fun setShowDoneTasks(show: Boolean) {
        preferences.edit().putBoolean(KEY_SHOW_DONE, show).apply()
        _showDoneTasks.value = show
    }

    fun setShowDoneListItems(show: Boolean) {
        preferences.edit().putBoolean(KEY_SHOW_DONE_LIST_ITEMS, show).apply()
        _showDoneListItems.value = show
    }

    fun setProjectFilter(filter: ProjectFilter) {
        preferences.edit()
            .putStringSet(KEY_FILTER_STATUSES, filter.statuses.map { it.storedValue }.toSet())
            .putStringSet(
                KEY_FILTER_PRIORITIES,
                filter.priorities.map { it?.storedValue ?: NO_PRIORITY }.toSet(),
            )
            .apply()
        _projectFilter.value = filter
    }

    private fun loadThemeMode(): ThemeMode {
        val stored = preferences.getString(KEY_THEME, null) ?: return ThemeMode.SYSTEM
        return runCatching { ThemeMode.valueOf(stored) }.getOrDefault(ThemeMode.SYSTEM)
    }

    private fun loadProjectFilter(): ProjectFilter {
        val statuses = preferences.getStringSet(KEY_FILTER_STATUSES, null)
            ?.mapNotNull(ProjectStatus::fromStored)
            ?.toSet()
            ?: ProjectFilter.DEFAULT_STATUSES
        val priorities = preferences.getStringSet(KEY_FILTER_PRIORITIES, null)
            ?.map { if (it == NO_PRIORITY) null else Priority.fromStored(it) }
            ?.toSet()
            ?: ProjectFilter.ALL_PRIORITIES
        return ProjectFilter(statuses = statuses, priorities = priorities)
    }

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_SHOW_DONE = "show_done_tasks"
        const val KEY_SHOW_DONE_LIST_ITEMS = "show_done_list_items"
        const val KEY_FILTER_STATUSES = "filter_statuses"
        const val KEY_FILTER_PRIORITIES = "filter_priorities"
        const val NO_PRIORITY = "_ninguna"
    }
}
