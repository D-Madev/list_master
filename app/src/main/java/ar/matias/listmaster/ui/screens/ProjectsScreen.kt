package ar.matias.listmaster.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import ar.matias.listmaster.domain.model.ProjectFilter
import ar.matias.listmaster.ui.sheets.ProjectFilterSheet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ar.matias.listmaster.R
import ar.matias.listmaster.domain.ProgressCalculator
import ar.matias.listmaster.domain.ProjectProgress
import ar.matias.listmaster.domain.ProjectTree
import ar.matias.listmaster.domain.model.MAX_SUBPROJECT_DEPTH
import ar.matias.listmaster.domain.model.Priority
import ar.matias.listmaster.domain.model.Project
import ar.matias.listmaster.domain.model.ProjectStatus
import ar.matias.listmaster.domain.model.Task
import ar.matias.listmaster.ui.components.CenteredEmpty
import ar.matias.listmaster.ui.components.ProjectCard
import ar.matias.listmaster.ui.components.ProjectCompactRow
import ar.matias.listmaster.ui.components.ScreenTitle
import ar.matias.listmaster.ui.components.SectionHeader
import ar.matias.listmaster.ui.sheets.DeleteProjectDialog
import ar.matias.listmaster.ui.sheets.PriorityPickerSheet
import ar.matias.listmaster.ui.sheets.ProjectMenuSheet
import ar.matias.listmaster.ui.sheets.StatusPickerSheet
import ar.matias.listmaster.ui.sheets.TaskFormSheet
import java.time.LocalDate

private data class CardTaskEditor(
    val projectId: String,
    val task: Task?,
)

@Composable
fun ProjectsScreen(
    projects: List<Project>,
    tasks: List<Task>,
    filter: ProjectFilter,
    onFilterChange: (ProjectFilter) -> Unit,
    onOpenProject: (String) -> Unit,
    onNewProject: (String?) -> Unit,
    onEditProject: (String) -> Unit,
    onSetStatus: (String, ProjectStatus) -> Unit,
    onSetPriority: (String, Priority?) -> Unit,
    onDeleteProject: (String) -> Unit,
    showDoneTasks: Boolean,
    onShowDoneTasksChange: (Boolean) -> Unit,
    onToggleTask: (Task) -> Unit,
    onCreateTask: (String, String, LocalDate?, String, Priority?) -> Unit,
    onUpdateTask: (String, String, LocalDate?, String, Priority?, String) -> Unit,
    onDeleteTask: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Los ids abiertos se guardan para que el estado sobreviva a la rotación.
    var expandedIds by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var filterOpen by remember { mutableStateOf(false) }
    var menuProject by remember { mutableStateOf<Project?>(null) }
    var statusProject by remember { mutableStateOf<Project?>(null) }
    var priorityProject by remember { mutableStateOf<Project?>(null) }
    var deleteProject by remember { mutableStateOf<Project?>(null) }
    var taskEditor by remember { mutableStateOf<CardTaskEditor?>(null) }
    val haptics = LocalHapticFeedback.current

    val allRoots = projects.filter { it.parentId == null }
    // Orden automático: Alta, Media, Baja y sin prioridad; a igual prioridad, el más viejo primero.
    val roots = allRoots.filter(filter::matches).sortedWith(ProjectTree.byPriority)
    val active = roots.filter { it.status == ProjectStatus.ACTIVE }
    val paused = roots.filter {
        it.status == ProjectStatus.PAUSED || it.status == ProjectStatus.POSTPONED
    }
    val closed = roots.filter { it.status.isClosed }
    val activeCount = allRoots.count { it.status == ProjectStatus.ACTIVE }

    val allExpanded = roots.isNotEmpty() && roots.all { it.id in expandedIds }

    fun toggleExpanded(id: String) {
        expandedIds = ArrayList(expandedIds).also { ids ->
            if (!ids.remove(id)) ids.add(id)
        }
    }

    fun setAllExpanded(expanded: Boolean) {
        expandedIds = if (expanded) {
            ArrayList(roots.map { it.id })
        } else {
            arrayListOf()
        }
    }

    @Composable
    fun CardItem(project: Project) {
        ProjectCardItem(
            project = project,
            projects = projects,
            tasks = tasks,
            showDoneTasks = showDoneTasks,
            onOpenProject = onOpenProject,
            onMenu = { menuProject = project },
            onStatusClick = { statusProject = project },
            onPriorityClick = { priorityProject = project },
            onToggleTask = { task ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggleTask(task)
            },
            expanded = project.id in expandedIds,
            onToggleExpanded = { toggleExpanded(project.id) },
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (allRoots.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
            ) {
                ScreenTitle(
                    text = stringResource(R.string.projects_title),
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CenteredEmpty(
                        iconRes = R.drawable.ic_folder,
                        title = stringResource(R.string.projects_empty_title),
                        body = stringResource(R.string.projects_empty_body),
                        actionLabel = stringResource(R.string.projects_empty_action),
                        onAction = { onNewProject(null) },
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "header") {
                    Column {
                        ScreenTitle(
                            text = stringResource(R.string.projects_title),
                            subtitle = if (activeCount == 0) {
                                null
                            } else {
                                pluralStringResource(
                                    R.plurals.projects_active_count,
                                    activeCount,
                                    activeCount,
                                )
                            },
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = if (filter.activeCount == 0) {
                                    stringResource(R.string.filter_button)
                                } else {
                                    stringResource(R.string.filter_button_count, filter.activeCount)
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .heightIn(min = 40.dp)
                                    .clickable { filterOpen = true }
                                    .padding(horizontal = 4.dp, vertical = 10.dp),
                            )
                            Row(
                                modifier = Modifier
                                    .heightIn(min = 40.dp)
                                    .clickable { onShowDoneTasksChange(!showDoneTasks) },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.done_toggle_short),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Switch(
                                    checked = showDoneTasks,
                                    onCheckedChange = onShowDoneTasksChange,
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .scale(0.8f),
                                )
                            }
                            Text(
                                text = stringResource(
                                    if (allExpanded) {
                                        R.string.projects_collapse_all
                                    } else {
                                        R.string.projects_expand_all
                                    },
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .heightIn(min = 40.dp)
                                    .clickable { setAllExpanded(!allExpanded) }
                                    .padding(horizontal = 4.dp, vertical = 10.dp),
                            )
                        }
                    }
                }

                if (roots.isEmpty()) {
                    item(key = "filter-empty") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(R.string.filter_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.filter_empty_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(16.dp))
                            TextButton(onClick = { onFilterChange(ProjectFilter.DEFAULT) }) {
                                Text(stringResource(R.string.filter_clear))
                            }
                        }
                    }
                }

                if (active.isNotEmpty()) {
                    item(key = "section-active") {
                        SectionHeader(stringResource(R.string.section_active))
                    }
                    items(active, key = { it.id }) { project -> CardItem(project) }
                }

                if (paused.isNotEmpty()) {
                    item(key = "section-paused") {
                        SectionHeader(
                            stringResource(R.string.section_paused),
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    items(paused, key = { it.id }) { project -> CardItem(project) }
                }

                if (closed.isNotEmpty()) {
                    item(key = "section-closed") {
                        SectionHeader(
                            stringResource(R.string.section_closed),
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    items(closed, key = { it.id }) { project -> CardItem(project) }
                }
            }
        }

        FloatingActionButton(
            onClick = { onNewProject(null) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.project_new),
            )
        }
    }

    if (filterOpen) {
        ProjectFilterSheet(
            filter = filter,
            onChange = onFilterChange,
            onDismiss = { filterOpen = false },
        )
    }

    menuProject?.let { project ->
        ProjectMenuSheet(
            project = project,
            canAddSubproject = ProjectTree.depthOf(project.id, projects) in
                0 until MAX_SUBPROJECT_DEPTH,
            onDismiss = { menuProject = null },
            onEdit = {
                menuProject = null
                onEditProject(project.id)
            },
            onChangeStatus = {
                menuProject = null
                statusProject = project
            },
            onAddSubproject = {
                menuProject = null
                onNewProject(project.id)
            },
            onAddTask = {
                menuProject = null
                taskEditor = CardTaskEditor(project.id, null)
            },
            onDelete = {
                menuProject = null
                deleteProject = project
            },
        )
    }

    statusProject?.let { project ->
        StatusPickerSheet(
            current = project.status,
            onDismiss = { statusProject = null },
            onSelect = { status ->
                statusProject = null
                if (status == ProjectStatus.DONE || status == ProjectStatus.CANCELLED) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onSetStatus(project.id, status)
            },
        )
    }

    priorityProject?.let { project ->
        PriorityPickerSheet(
            current = project.priority,
            onDismiss = { priorityProject = null },
            onSelect = { priority ->
                priorityProject = null
                onSetPriority(project.id, priority)
            },
        )
    }

    deleteProject?.let { project ->
        DeleteProjectDialog(
            project = project,
            onDismiss = { deleteProject = null },
            onConfirm = {
                deleteProject = null
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onDeleteProject(project.id)
            },
        )
    }

    taskEditor?.let { editor ->
        TaskFormSheet(
            task = editor.task,
            projects = projects,
            initialProjectId = editor.projectId,
            onDismiss = { taskEditor = null },
            onSave = { title, dueDate, description, priority, targetProjectId ->
                if (editor.task == null) {
                    onCreateTask(targetProjectId, title, dueDate, description, priority)
                } else {
                    onUpdateTask(editor.task.id, title, dueDate, description, priority, targetProjectId)
                }
                taskEditor = null
            },
            onDelete = editor.task?.let { task ->
                {
                    onDeleteTask(task.id)
                    taskEditor = null
                }
            },
        )
    }
}

@Composable
private fun ProjectCardItem(
    project: Project,
    projects: List<Project>,
    tasks: List<Task>,
    showDoneTasks: Boolean,
    onOpenProject: (String) -> Unit,
    onMenu: () -> Unit,
    onStatusClick: () -> Unit,
    onPriorityClick: () -> Unit,
    onToggleTask: (Task) -> Unit,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    val progress = remember(project.id, projects, tasks) {
        ProgressCalculator.calculate(project.id, projects, tasks)
    }
    Column(modifier = Modifier.animateContentSize()) {
        if (expanded) {
            val rows = remember(project.id, projects, tasks, showDoneTasks) {
                buildDetailRows(project, projects, tasks, showDoneTasks)
            }
            ProjectCard(
                project = project,
                progress = progress,
                rows = rows,
                onOpen = { onOpenProject(project.id) },
                onToggleTask = onToggleTask,
                onMenu = onMenu,
                onStatusClick = onStatusClick,
                onPriorityClick = onPriorityClick,
                onOpenSubproject = onOpenProject,
                onCollapse = onToggleExpanded,
            )
        } else {
            ProjectCompactRow(
                project = project,
                progress = progress,
                expanded = false,
                onToggle = onToggleExpanded,
            )
        }
    }
}
