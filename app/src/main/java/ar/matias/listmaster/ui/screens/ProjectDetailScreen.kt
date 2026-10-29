package ar.matias.listmaster.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import ar.matias.listmaster.ui.components.CenteredEmpty
import ar.matias.listmaster.ui.components.DetailHeader
import ar.matias.listmaster.ui.components.IconTile
import ar.matias.listmaster.ui.components.NoticeCard
import ar.matias.listmaster.ui.components.NoticeTone
import ar.matias.listmaster.ui.components.PrimaryButton
import ar.matias.listmaster.ui.components.ProgressPill
import ar.matias.listmaster.ui.components.SecondaryButton
import ar.matias.listmaster.ui.components.SectionHeader
import ar.matias.listmaster.ui.components.StatusChip
import ar.matias.listmaster.ui.components.PriorityChip
import ar.matias.listmaster.ui.components.SubprojectRow
import ar.matias.listmaster.ui.components.TaskRow
import ar.matias.listmaster.ui.sheets.DeleteProjectDialog
import ar.matias.listmaster.ui.sheets.PriorityPickerSheet
import ar.matias.listmaster.ui.sheets.ProjectMenuSheet
import ar.matias.listmaster.ui.sheets.StatusPickerSheet
import ar.matias.listmaster.ui.sheets.TaskFormSheet
import java.time.LocalDate

private data class TaskEditor(
    val projectId: String,
    val task: Task?,
)

private sealed interface DetailRow {
    val depth: Int
    val isLast: Boolean

    data class TaskItem(
        val task: Task,
        val owner: Project,
        override val depth: Int,
        override val isLast: Boolean,
    ) : DetailRow

    data class ProjectItem(
        val project: Project,
        val progress: ProjectProgress,
        override val depth: Int,
        override val isLast: Boolean,
    ) : DetailRow
}

@OptIn(ExperimentalLayoutApi::class)
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
    onToggleTask: (Task) -> Unit,
    onCreateTask: (String, String, LocalDate?) -> Unit,
    onUpdateTask: (String, String, LocalDate?) -> Unit,
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
    val rows = remember(project.id, projects, tasks) {
        buildDetailRows(project, projects, tasks)
    }
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
                    DetailHeader(
                        title = project.title,
                        onBack = onBack,
                        modifier = Modifier.padding(start = 0.dp),
                        actions = {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = stringResource(R.string.cd_menu, project.title),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                    )
                    if (ancestors.isNotEmpty()) {
                        Text(
                            text = ancestors.joinToString(" / ") { it.title },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
            }

            item(key = "summary") {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(
                            iconRes = if (project.parentId == null) {
                                R.drawable.ic_folder
                            } else {
                                R.drawable.ic_layers
                            },
                            contentDescription = stringResource(
                                if (project.parentId == null) {
                                    R.string.cd_project_icon
                                } else {
                                    R.string.cd_subproject_icon
                                },
                            ),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = project.title,
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (project.status == ProjectStatus.CANCELLED) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                    if (project.status != ProjectStatus.CANCELLED) {
                        Spacer(Modifier.height(12.dp))
                        ProgressPill(progress = progress)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StatusChip(
                            status = project.status,
                            onClick = { statusOpen = true },
                        )
                        if (project.priority != null) {
                            PriorityChip(
                                priority = project.priority,
                                onClick = { priorityOpen = true },
                            )
                        } else {
                            TextButton(onClick = { priorityOpen = true }) {
                                Text(
                                    text = stringResource(R.string.priority_none),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
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

            item(key = "actions") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PrimaryButton(
                        text = stringResource(R.string.project_add_task),
                        onClick = { taskEditor = TaskEditor(project.id, null) },
                    )
                    if (canHaveSubproject) {
                        SecondaryButton(
                            text = stringResource(R.string.project_add_subproject),
                            onClick = { onNewSubproject(project.id) },
                        )
                    }
                }
            }

            if (rows.isEmpty()) {
                item(key = "empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.project_empty_tasks_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.project_empty_tasks_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                item(key = "tree-title") {
                    SectionHeader(
                        stringResource(R.string.project_tree_title),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(rows) { row ->
                    when (row) {
                        is DetailRow.TaskItem -> TaskRow(
                            task = row.task,
                            depth = row.depth,
                            isLast = row.isLast,
                            projectDone = row.owner.status == ProjectStatus.DONE,
                            onToggle = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleTask(row.task)
                            },
                            onOpen = { taskEditor = TaskEditor(row.owner.id, row.task) },
                        )

                        is DetailRow.ProjectItem -> SubprojectRow(
                            project = row.project,
                            progress = row.progress,
                            depth = row.depth,
                            isLast = row.isLast,
                            onClick = { onOpenProject(row.project.id) },
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { taskEditor = TaskEditor(project.id, null) },
            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.project_add_task)) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        )
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

/**
 * Árbol completo del detalle: tareas directas primero, después cada subproyecto
 * con su propio subárbol. La raíz no se dibuja como fila.
 */
private fun buildDetailRows(
    root: Project,
    projects: List<Project>,
    tasks: List<Task>,
): List<DetailRow> {
    val tasksByProject = tasks.groupBy { it.projectId }
    val childrenByParent = projects.groupBy { it.parentId }
    val rows = mutableListOf<DetailRow>()

    fun emitChildren(project: Project, depth: Int) {
        val items = buildList<Any> {
            addAll(tasksByProject[project.id].orEmpty())
            addAll(childrenByParent[project.id].orEmpty())
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
                    ),
                )

                is Project -> {
                    rows.add(
                        DetailRow.ProjectItem(
                            project = item,
                            progress = ProgressCalculator.calculate(item.id, projects, tasks),
                            depth = depth,
                            isLast = isLast,
                        ),
                    )
                    emitChildren(item, depth + 1)
                }
            }
        }
    }

    emitChildren(root, depth = 1)
    return rows
}
