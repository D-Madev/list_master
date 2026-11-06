package ar.matias.listmaster.domain.model

/**
 * Qué proyectos principales se ven en la pantalla de Proyectos. Se guarda en el
 * celular para que el filtro siga puesto la próxima vez. Una prioridad nula es
 * "Sin prioridad".
 */
data class ProjectFilter(
    val statuses: Set<ProjectStatus> = DEFAULT_STATUSES,
    val priorities: Set<Priority?> = ALL_PRIORITIES,
) {
    val isDefault: Boolean get() = this == DEFAULT

    /** Cuántos criterios se apartan de lo predeterminado: sirve para el globito del botón. */
    val activeCount: Int
        get() = (if (statuses != DEFAULT_STATUSES) 1 else 0) +
            (if (priorities != ALL_PRIORITIES) 1 else 0)

    fun matches(project: Project): Boolean =
        project.status in statuses && project.priority in priorities

    companion object {
        val DEFAULT_STATUSES: Set<ProjectStatus> =
            setOf(ProjectStatus.ACTIVE, ProjectStatus.PAUSED, ProjectStatus.POSTPONED)
        val ALL_PRIORITIES: Set<Priority?> = Priority.entries.toSet() + setOf<Priority?>(null)
        val DEFAULT = ProjectFilter()
    }
}
