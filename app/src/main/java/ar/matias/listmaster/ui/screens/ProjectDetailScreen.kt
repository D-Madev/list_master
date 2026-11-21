package ar.matias.listmaster.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import ar.matias.listmaster.domain.model.TaskOrder
import ar.matias.listmaster.ui.components.CenteredEmpty
import ar.matias.listmaster.ui.components.rememberCompletionReveal
import ar.matias.listmaster.ui.components.DetailHeader
import ar.matias.listmaster.ui.components.ProjectIconTile
import ar.matias.listmaster.ui.components.NoticeCard
import ar.matias.listmaster.ui.components.NoticeTone
import ar.matias.listmaster.ui.components.ProgressPill
import ar.matias.listmaster.ui.components.StatusChip
import ar.matias.listmaster.ui.components.NoPriorityChip
import ar.matias.listmaster.ui.components.PriorityChip
import ar.matias.listmaster.ui.components.PropertyRow
import ar.matias.listmaster.ui.components.SubprojectRow
import ar.matias.listmaster.ui.components.TaskRow
import ar.matias.listmaster.ui.sheets.DeleteProjectDialog
import ar.matias.listmaster.ui.sheets.PriorityPickerSheet
import ar.matias.listmaster.ui.sheets.ProjectMenuSheet
import ar.matias.listmaster.ui.sheets.StatusPickerSheet
import ar.matias.listmaster.ui.sheets.TaskFormSheet
import ar.matias.listmaster.ui.iconRes
import java.time.LocalDate

private data class TaskEditor(
    val projectId: String,
    val task: Task?,
)

internal sealed interface DetailRow {
    val depth: Int
    val isLast: Boolean
    val ancestorContinues: List<Boolean>

    data class TaskItem(
        val task: Task,
        val owner: Project,
        override val depth: Int,
        override val isLast: Boolean,
        override val ancestorContinues: List<Boolean>,
    ) : DetailRow

    data class ProjectItem(
        val project: Project,
        val progress: ProjectProgress,
        val hasChildren: Boolean,
        override val depth: Int,
        override val isLast: Boolean,
        override val ancestorContinues: List<Boolean>,
    ) : DetailRow
}

@Composable
fun ProjectDetailScreen(
    projectId: String,
    projects: List<Project>,
    tasks: List<Task>,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onNewSubproject: (String) -> Unit,
    onOpenProject: (String) -> Unit,
    onSetStatus: (String, ProjectStatus) -> Unit,
    onSetPriority: (String, Priority?) -> Unit,
    onDeleteProject: (String) -> Unit,
    showDoneTasks: Boolean,
    onShowDoneTasksChange: (Boolean) -> Unit,
    onToggleTask: (Task) -> Unit,
    onCreateTask: (String, String, LocalDate?, String, Priority?, String) -> Unit,
    onUpdateTask: (String, String, LocalDate?, String, Priority?, String, String) -> Unit,
    onDeleteTask: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val project = projects.firstOrNull { it.id == projectId }
    var menuOpen by remember { mutableStateOf(false) }
    var statusOpen by remember { mutableStateOf(false) }
    var priorityOpen by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }
    var taskEditor by remember { mutableStateOf<TaskEditor?>(null) }
    val haptics = LocalHapticFeedback.current

    if (project == null) {
        Column(modifier = modifier.fillMaxSize()) {
            DetailHeader(
                title = stringResource(R.string.projects_title),
                onBack = onBack,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                CenteredEmpty(
                    iconRes = R.drawable.ic_folder,
                    title = stringResource(R.string.project_not_found),
                    body = "",
                )
            }
        }
        return
    }

    val progress = ProgressCalculator.calculate(project.id, projects, tasks)
    val completionReveal = rememberCompletionReveal()
    val rows = remember(project.id, projects, tasks, showDoneTasks, completionReveal.ids) {
        buildDetailRows(
            project,
            projects,
            tasks,
            showDoneTasks,
            extraVisibleIds = completionReveal.ids,
        )
    }
    val doneCount = progress.done
    val depth = ProjectTree.depthOf(project.id, projects)
    val canHaveSubproject = depth in 0 until MAX_SUBPROJECT_DEPTH
    val ancestors = ProjectTree.ancestorsOf(project.id, projects)
    val pending = (progress.total - progress.done).coerceAtLeast(0)

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        ProjectIconTile(
                            project = project,
                            contentDescription = stringResource(
                                if (project.parentId == null) {
                                    R.string.cd_project_icon
                                } else {
                                    R.string.cd_subproject_icon
                                },
                            ),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = project.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (project.status == ProjectStatus.CANCELLED) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = stringResource(R.string.cd_menu, project.title),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (ancestors.isNotEmpty()) {
                        Text(
                            text = ancestors.joinToString(" / ") { it.title },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 56.dp, bottom = 4.dp),
                        )
                    }
                }
            }

            if (project.status != ProjectStatus.CANCELLED) {
                item(key = "progress") {
                    ProgressPill(progress = progress)
                }
            }

            item(key = "properties") {
                Column {
                    PropertyRow(
                        iconRes = R.drawable.ic_flag,
                        label = stringResource(R.string.project_priority_label),
                        onClick = { priorityOpen = true },
                    ) {
                        if (project.priority != null) {
                            PriorityChip(
                                priority = project.priority,
                                onClick = { priorityOpen = true },
                            )
                        } else {
                            NoPriorityChip(onClick = { priorityOpen = true })
                        }
                    }
                    PropertyRow(
                        iconRes = project.status.iconRes(),
                        label = stringResource(R.string.project_status_label),
                        onClick = { statusOpen = true },
                    ) {
                        StatusChip(
                            status = project.status,
                            onClick = { statusOpen = true },
                        )
                    }
                }
            }

            if (project.status == ProjectStatus.DONE && pending > 0) {
                item(key = "notice-pending") {
                    NoticeCard(
                        text = stringResource(R.string.project_done_with_pending, pending),
                        tone = NoticeTone.WARNING,
                    )
                }
            }

            if (progress.isComplete && project.status == ProjectStatus.ACTIVE) {
                item(key = "notice-complete") {
                    NoticeCard(
                        text = stringResource(R.string.project_all_done),
                        actionLabel = stringResource(R.string.project_mark_done),
                        onAction = { onSetStatus(project.id, ProjectStatus.DONE) },
                        tone = NoticeTone.SUCCESS,
                    )
                }
            }

            if (doneCount > 0) {
                item(key = "done-toggle") {
                    Row(
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { onShowDoneTasksChange(!showDoneTasks) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.done_toggle_count, doneCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = showDoneTasks,
                            onCheckedChange = onShowDoneTasksChange,
                        )
                    }
                }
            }

            if (rows.isEmpty()) {
                item(key = "empty") {
                    val allHidden = doneCount > 0 && !showDoneTasks
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(
                                if (allHidden) {
                                    R.string.done_all_hidden_title
                                } else {
                                    R.string.project_empty_tasks_title
                                },
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(
                                if (allHidden) {
                                    R.string.done_all_hidden_body
                                } else {
                                    R.string.project_empty_tasks_body
                                },
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                // Una fila por ítem: así el árbol se acomoda solo, sin saltos,
                // cuando aparece el selector de hechas o se va una tarea.
                rows.forEach { row ->
                    when (row) {
                        is DetailRow.TaskItem -> item(
                            key = "task-${row.task.id}",
                            contentType = "task",
                        ) {
                            TaskRow(
                                task = row.task,
                                depth = row.depth,
                                isLast = row.isLast,
                                onToggle = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (!row.task.done && !showDoneTasks) {
                                        completionReveal.reveal(row.task.id)
                                    }
                                    onToggleTask(row.task)
                                },
                                onOpen = { taskEditor = TaskEditor(row.owner.id, row.task) },
                                ancestorContinues = row.ancestorContinues,
                                modifier = Modifier.animateItem(),
                            )
                        }

                        is DetailRow.ProjectItem -> item(
                            key = "project-${row.project.id}",
                            contentType = "project",
                        ) {
                            SubprojectRow(
                                project = row.project,
                                progress = row.progress,
                                depth = row.depth,
                                isLast = row.isLast,
                                onClick = { onOpenProject(row.project.id) },
                                ancestorContinues = row.ancestorContinues,
                                hasChildren = row.hasChildren,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { taskEditor = TaskEditor(project.id, null) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.project_add_task),
            )
        }
    }

    if (menuOpen) {
        ProjectMenuSheet(
            project = project,
            canAddSubproject = canHaveSubproject,
            onDismiss = { menuOpen = false },
            onEdit = {
                menuOpen = false
                onEdit(project.id)
            },
            onChangeStatus = {
                menuOpen = false
                statusOpen = true
            },
            onAddSubproject = {
                menuOpen = false
                onNewSubproject(project.id)
            },
            onAddTask = {
                menuOpen = false
                taskEditor = TaskEditor(project.id, null)
            },
            onDelete = {
                menuOpen = false
                deleteOpen = true
            },
        )
    }

    if (statusOpen) {
        StatusPickerSheet(
            current = project.status,
            onDismiss = { statusOpen = false },
            onSelect = { status ->
                statusOpen = false
                if (status == ProjectStatus.DONE || status == ProjectStatus.CANCELLED) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onSetStatus(project.id, status)
            },
        )
    }

    if (priorityOpen) {
        PriorityPickerSheet(
            current = project.priority,
            onDismiss = { priorityOpen = false },
            onSelect = { priority ->
                priorityOpen = false
                onSetPriority(project.id, priority)
            },
        )
    }

    if (deleteOpen) {
        DeleteProjectDialog(
            project = project,
            onDismiss = { deleteOpen = false },
            onConfirm = {
                deleteOpen = false
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
            onSave = { title, dueDate, description, priority, targetProjectId, location ->
                if (editor.task == null) {
                    onCreateTask(targetProjectId, title, dueDate, description, priority, location)
                } else {
                    onUpdateTask(
                        editor.task.id,
                        title,
                        dueDate,
                        description,
                        priority,
                        targetProjectId,
                        location,
                    )
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

/**
 * Árbol completo del detalle. En cada nivel van las tareas (pendientes y,
 * si [showDone], las hechas en su mismo lugar, para que se vea la animación
 * de completado) y después los subproyectos, por prioridad, cada uno con su
 * subárbol. La raíz no se dibuja como fila.
 */
internal fun buildDetailRows(
    root: Project,
    projects: List<Project>,
    tasks: List<Task>,
    showDone: Boolean = true,
    extraVisibleIds: Set<String> = emptySet(),
): List<DetailRow> {
    val tasksByProject = tasks.groupBy { it.projectId }
    val childrenByParent = projects.groupBy { it.parentId }
    val rows = mutableListOf<DetailRow>()

    fun emitChildren(
        project: Project,
        depth: Int,
        ancestorContinues: List<Boolean>,
    ) {
        val own = tasksByProject[project.id].orEmpty()
            .filter { showDone || !it.done || it.id in extraVisibleIds }
            .sortedWith(TaskOrder)
        val items = buildList<Any> {
            addAll(own)
            addAll(childrenByParent[project.id].orEmpty().sortedWith(ProjectTree.byPriority))
        }
        items.forEachIndexed { index, item ->
            val isLast = index == items.lastIndex
            when (item) {
                is Task -> rows.add(
                    DetailRow.TaskItem(
                        task = item,
                        owner = project,
                        depth = depth,
                        isLast = isLast,
                        ancestorContinues = ancestorContinues,
                    ),
                )

                is Project -> {
                    rows.add(
                        DetailRow.ProjectItem(
                            project = item,
                            progress = ProgressCalculator.calculate(item.id, projects, tasks),
                            hasChildren = tasksByProject[item.id].orEmpty()
                                .any { showDone || !it.done || it.id in extraVisibleIds } ||
                                childrenByParent[item.id].orEmpty().isNotEmpty(),
                            depth = depth,
                            isLast = isLast,
                            ancestorContinues = ancestorContinues,
                        ),
                    )
                    // Para los hijos, este proyecto es un ancestro más: su
                    // vertical sigue sólo si él tiene un hermano después.
                    emitChildren(item, depth + 1, ancestorContinues + !isLast)
                }
            }
        }
    }

    emitChildren(root, depth = 1, ancestorContinues = emptyList())
    return rows
}
