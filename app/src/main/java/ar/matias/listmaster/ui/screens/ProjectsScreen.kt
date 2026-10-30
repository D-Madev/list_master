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
    onOpenProject: (String) -> Unit,
    onNewProject: (String?) -> Unit,
    onEditProject: (String) -> Unit,
    onSetStatus: (String, ProjectStatus) -> Unit,
    onSetPriority: (String, Priority?) -> Unit,
    onDeleteProject: (String) -> Unit,
    onToggleTask: (Task) -> Unit,
    onCreateTask: (String, String, LocalDate?) -> Unit,
    onUpdateTask: (String, String, LocalDate?) -> Unit,
    onDeleteTask: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClosed by rememberSaveable { mutableStateOf(false) }
    // Los ids abiertos se guardan para que el estado sobreviva a la rotación.
    var expandedIds by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var menuProject by remember { mutableStateOf<Project?>(null) }
    var statusProject by remember { mutableStateOf<Project?>(null) }
    var priorityProject by remember { mutableStateOf<Project?>(null) }
    var deleteProject by remember { mutableStateOf<Project?>(null) }
    var taskEditor by remember { mutableStateOf<CardTaskEditor?>(null) }
    val haptics = LocalHapticFeedback.current

    val roots = projects.filter { it.parentId == null }
    val active = roots.filter { it.status == ProjectStatus.ACTIVE }
    val paused = roots.filter {
        it.status == ProjectStatus.PAUSED || it.status == ProjectStatus.POSTPONED
    }
    val closed = roots.filter { it.status.isClosed }

    val childrenByParent = projects.groupBy { it.parentId }
    val tasksByProject = tasks.groupBy { it.projectId }

    fun progressOf(id: String) = ProgressCalculator.calculate(id, projects, tasks)

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

    Box(modifier = modifier.fillMaxSize()) {
        if (roots.isEmpty()) {
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
                            subtitle = if (active.isEmpty()) {
                                null
                            } else {
                                pluralStringResource(
                                    R.plurals.projects_active_count,
                                    active.size,
                                    active.size,
                                )
                            },
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
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

                if (active.isNotEmpty()) {
                    item(key = "section-active") {
                        SectionHeader(stringResource(R.string.section_active))
                    }
                    items(active, key = { it.id }) { project ->
                        ProjectCardItem(
                            project = project,
                            projects = projects,
                            tasksByProject = tasksByProject,
                            childrenByParent = childrenByParent,
                            progressOf = ::progressOf,
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
                }

                if (paused.isNotEmpty()) {
                    item(key = "section-paused") {
                        SectionHeader(
                            stringResource(R.string.section_paused),
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    items(paused, key = { it.id }) { project ->
                        ProjectCardItem(
                            project = project,
                            projects = projects,
                            tasksByProject = tasksByProject,
                            childrenByParent = childrenByParent,
                            progressOf = ::progressOf,
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
                }

                if (closed.isNotEmpty()) {
                    item(key = "section-closed") {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text(
                                text = if (showClosed) {
                                    stringResource(R.string.section_hide_closed)
                                } else {
                                    stringResource(R.string.section_show_closed, closed.size)
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clickable { showClosed = !showClosed }
                                    .padding(vertical = 12.dp),
                            )
                        }
                    }
                    if (showClosed) {
                        items(closed, key = { it.id }) { project ->
                            ProjectCardItem(
                                project = project,
                                projects = projects,
                                tasksByProject = tasksByProject,
                                childrenByParent = childrenByParent,
                                progressOf = ::progressOf,
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
                    }
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
            onDismiss = { taskEditor = null },
            onSave = { title, dueDate ->
                if (editor.task == null) {
                    onCreateTask(editor.projectId, title, dueDate)
                } else {
                    onUpdateTask(editor.task.id, title, dueDate)
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
    tasksByProject: Map<String, List<Task>>,
    childrenByParent: Map<String?, List<Project>>,
    progressOf: (String) -> ProjectProgress,
    onOpenProject: (String) -> Unit,
    onMenu: () -> Unit,
    onStatusClick: () -> Unit,
    onPriorityClick: () -> Unit,
    onToggleTask: (Task) -> Unit,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    val children = childrenByParent[project.id].orEmpty()
    val progress = progressOf(project.id)
    Column(modifier = Modifier.animateContentSize()) {
        if (expanded) {
            ProjectCard(
                project = project,
                progress = progress,
                directTasks = tasksByProject[project.id].orEmpty(),
                subprojects = children,
                subprojectProgress = children.associate { it.id to progressOf(it.id) },
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
