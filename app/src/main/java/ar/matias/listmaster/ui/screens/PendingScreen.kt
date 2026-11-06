package ar.matias.listmaster.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ar.matias.listmaster.R
import ar.matias.listmaster.domain.PendingGroup
import ar.matias.listmaster.domain.PendingTasks
import ar.matias.listmaster.domain.ProjectTree
import ar.matias.listmaster.domain.model.Priority
import ar.matias.listmaster.domain.model.Project
import ar.matias.listmaster.domain.model.Task
import ar.matias.listmaster.ui.components.CenteredEmpty
import ar.matias.listmaster.ui.components.ProjectIcon
import ar.matias.listmaster.ui.components.ScreenTitle
import ar.matias.listmaster.ui.components.TaskCheck
import ar.matias.listmaster.ui.formatShort
import ar.matias.listmaster.ui.sheets.ProjectPickerSheet
import ar.matias.listmaster.ui.sheets.TaskFormSheet
import java.time.LocalDate

private data class PendingEditor(
    val projectId: String,
    val task: Task?,
)

/**
 * Todo lo pendiente en una sola lista, por proyecto: el proyecto principal con
 * sus tareas y, abajo y sangrados, sus subproyectos. Desde acá se completan,
 * editan, agregan y borran tareas sin entrar a cada proyecto.
 */
@Composable
fun PendingScreen(
    projects: List<Project>,
    tasks: List<Task>,
    onOpenProject: (String) -> Unit,
    onToggleTask: (Task) -> Unit,
    onCreateTask: (String, String, LocalDate?, String, Priority?) -> Unit,
    onUpdateTask: (String, String, LocalDate?, String, Priority?) -> Unit,
    onDeleteTask: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    var editor by remember { mutableStateOf<PendingEditor?>(null) }
    var addingFor by remember { mutableStateOf<String?>(null) }
    var pickerOpen by remember { mutableStateOf(false) }

    val groups = remember(projects, tasks) { PendingTasks.groups(projects, tasks) }
    // Un bloque por proyecto principal, con sus subproyectos adentro.
    val blocks = remember(groups) {
        buildList<MutableList<PendingGroup>> {
            groups.forEach { group ->
                if (group.depth == 0) add(mutableListOf())
                lastOrNull()?.add(group)
            }
        }
    }
    val pendingCount = PendingTasks.count(groups)

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                ScreenTitle(
                    text = stringResource(R.string.pending_title),
                    subtitle = if (pendingCount == 0) {
                        null
                    } else {
                        pluralStringResource(
                            R.plurals.pending_summary,
                            pendingCount,
                            pendingCount,
                            blocks.size,
                        )
                    },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            if (blocks.isEmpty()) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CenteredEmpty(
                            iconRes = R.drawable.ic_check_list,
                            title = stringResource(R.string.pending_empty_title),
                            body = stringResource(R.string.pending_empty_body),
                        )
                    }
                }
            }

            items(blocks, key = { it.first().project.id }) { block ->
                PendingBlock(
                    block = block,
                    addingFor = addingFor,
                    onOpenProject = onOpenProject,
                    onStartAdd = { addingFor = if (addingFor == it) null else it },
                    onAdd = { projectId, title -> onCreateTask(projectId, title, null, "", null) },
                    onToggleTask = { task ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleTask(task)
                    },
                    onOpenTask = { project, task -> editor = PendingEditor(project.id, task) },
                )
            }
        }

        FloatingActionButton(
            onClick = { pickerOpen = true },
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

    if (pickerOpen) {
        val options = remember(projects) { openProjectsInOrder(projects) }
        ProjectPickerSheet(
            title = stringResource(R.string.pending_pick_project),
            options = options,
            current = null,
            onDismiss = { pickerOpen = false },
            onSelect = { project ->
                pickerOpen = false
                editor = PendingEditor(project.id, null)
            },
        )
    }

    editor?.let { current ->
        TaskFormSheet(
            task = current.task,
            onDismiss = { editor = null },
            onSave = { title, dueDate, description, priority ->
                if (current.task == null) {
                    onCreateTask(current.projectId, title, dueDate, description, priority)
                } else {
                    onUpdateTask(current.task.id, title, dueDate, description, priority)
                }
                editor = null
            },
            onDelete = current.task?.let { task ->
                {
                    onDeleteTask(task.id)
                    editor = null
                }
            },
        )
    }
}

/** Proyectos no cerrados, cada principal seguido de sus subproyectos, con su nivel. */
private fun openProjectsInOrder(projects: List<Project>): List<Pair<Project, Int>> {
    val byParent = projects.groupBy { it.parentId }
    val result = mutableListOf<Pair<Project, Int>>()
    fun visit(project: Project, depth: Int) {
        if (project.status.isClosed) return
        result.add(project to depth)
        byParent[project.id].orEmpty()
            .sortedWith(ProjectTree.byPriority)
            .forEach { visit(it, depth + 1) }
    }
    byParent[null].orEmpty().sortedWith(ProjectTree.byPriority).forEach { visit(it, 0) }
    return result
}

@Composable
private fun PendingBlock(
    block: List<PendingGroup>,
    addingFor: String?,
    onOpenProject: (String) -> Unit,
    onStartAdd: (String) -> Unit,
    onAdd: (String, String) -> Unit,
    onToggleTask: (Task) -> Unit,
    onOpenTask: (Project, Task) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    val darkTheme = scheme.surface.luminance() < 0.5f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = if (darkTheme) 0.dp else 2.dp, shape = shape)
            .clip(shape)
            .background(scheme.surface)
            .border(1.dp, scheme.outlineVariant, shape)
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 8.dp),
    ) {
        block.forEach { group ->
            val project = group.project
            val indent = (group.depth * 16).dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(start = indent),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenProject(project.id) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProjectIcon(
                        icon = project.icon,
                        isSubproject = project.parentId != null,
                        contentDescription = stringResource(R.string.pending_open_project, project.title),
                        size = if (group.depth == 0) 22.dp else 18.dp,
                        tint = if (group.depth == 0) scheme.onSurface else scheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = project.title,
                        style = if (group.depth == 0) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.labelLarge
                        },
                        color = if (group.depth == 0) scheme.onSurface else scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { onStartAdd(project.id) }) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.pending_add_to, project.title),
                        tint = scheme.onSurfaceVariant,
                    )
                }
            }

            group.tasks.forEach { task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(start = indent),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TaskCheck(
                        done = false,
                        contentDescription = stringResource(R.string.task_pending_description, task.title),
                        onToggle = { onToggleTask(task) },
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenTask(project, task) }
                            .padding(vertical = 6.dp),
                    ) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (task.dueDate != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = scheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = task.dueDate.formatShort(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = scheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            if (addingFor == project.id) {
                InlineAdd(
                    modifier = Modifier.padding(start = indent + 4.dp, end = 8.dp, bottom = 4.dp),
                    onAdd = { title -> onAdd(project.id, title) },
                )
            }
        }
    }
}

/** Campo para anotar tareas seguidas: Enter agrega y queda listo para la próxima. */
@Composable
private fun InlineAdd(
    onAdd: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        placeholder = { Text(stringResource(R.string.pending_add_placeholder)) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                val clean = text.trim()
                if (clean.isNotEmpty()) {
                    onAdd(clean)
                    text = ""
                }
            },
        ),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
    )
}
