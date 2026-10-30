package ar.matias.listmaster.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ar.matias.listmaster.R
import ar.matias.listmaster.domain.ProjectProgress
import ar.matias.listmaster.domain.model.Project
import ar.matias.listmaster.domain.model.ProjectStatus
import ar.matias.listmaster.domain.model.Task
import ar.matias.listmaster.ui.formatShort
import ar.matias.listmaster.ui.theme.LocalExtendedColors
import java.time.LocalDate

private const val MAX_CARD_ROWS = 4

/**
 * Fila compacta del listado: nombre, recuento y barra. Al tocarla se despliega
 * la tarjeta completa; la tarjeta sigue abriendo el detalle.
 */
@Composable
fun ProjectCompactRow(
    project: Project,
    progress: ProjectProgress,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val extended = LocalExtendedColors.current
    val shape = RoundedCornerShape(16.dp)
    val darkTheme = scheme.surface.luminance() < 0.5f
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 180, easing = EaseOut),
        label = "compactChevron",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = if (darkTheme) 0.dp else 1.dp, shape = shape)
            .clip(shape)
            .background(scheme.surface)
            .border(1.dp, scheme.outlineVariant, shape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProjectIconTile(
            project = project,
            contentDescription = null,
            size = 32,
            modifier = Modifier.clip(RoundedCornerShape(10.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (project.status == ProjectStatus.CANCELLED) {
                        scheme.onSurfaceVariant
                    } else {
                        scheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (progress.hasTasks) {
                        stringResource(R.string.progress_fraction, progress.done, progress.total)
                    } else {
                        stringResource(R.string.project_no_tasks_short)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (progress.hasTasks) extended.success else scheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (progress.hasTasks) {
                ThinProgressBar(
                    fraction = progress.percentage / 100f,
                    color = extended.success,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(extended.surfaceSunken),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowDown,
            contentDescription = stringResource(
                if (expanded) R.string.cd_collapse_project else R.string.cd_expand_project,
                project.title,
            ),
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { rotationZ = rotation },
            tint = scheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ProjectCard(
    project: Project,
    progress: ProjectProgress,
    directTasks: List<Task>,
    subprojects: List<Project>,
    subprojectProgress: Map<String, ProjectProgress>,
    onOpen: () -> Unit,
    onToggleTask: (Task) -> Unit,
    onMenu: () -> Unit,
    onStatusClick: () -> Unit,
    onPriorityClick: () -> Unit,
    onOpenSubproject: (String) -> Unit,
    modifier: Modifier = Modifier,
    onCollapse: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val extended = LocalExtendedColors.current
    val shape = RoundedCornerShape(20.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.995f else 1f,
        animationSpec = tween(durationMillis = 120, easing = EaseOut),
        label = "cardScale",
    )
    val darkTheme = scheme.surface.luminance() < 0.5f

    val rows = buildList {
        // Primero lo que falta: si el proyecto tiene muchas tareas hechas, las
        // pendientes igual aparecen en la vista previa.
        directTasks
            .sortedWith(compareBy({ it.done }, { it.dueDate ?: LocalDate.MAX }))
            .forEach { add(CardRow.TaskRow(it)) }
        subprojects.forEach { add(CardRow.SubprojectRow(it)) }
    }
    val visibleRows = rows.take(MAX_CARD_ROWS)
    val hiddenCount = rows.size - visibleRows.size

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(elevation = if (darkTheme) 0.dp else 3.dp, shape = shape)
            .clip(shape)
            .background(scheme.surface)
            .border(1.dp, scheme.outlineVariant, shape)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onOpen,
            )
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProjectIconTile(
                project = project,
                contentDescription = stringResource(
                    if (project.parentId == null) R.string.cd_project_icon else R.string.cd_subproject_icon,
                ),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = project.title,
                style = MaterialTheme.typography.titleLarge,
                color = if (project.status == ProjectStatus.CANCELLED) scheme.onSurfaceVariant else scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (onCollapse != null) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = stringResource(
                            R.string.cd_collapse_project,
                            project.title,
                        ),
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.graphicsLayer { rotationZ = 180f },
                    )
                }
            }
            IconButton(onClick = onMenu) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = stringResource(R.string.cd_menu, project.title),
                    tint = scheme.onSurfaceVariant,
                )
            }
        }

        if (project.status != ProjectStatus.CANCELLED) {
            Spacer(Modifier.height(12.dp))
            ProgressPill(progress = progress)
        }

        if (visibleRows.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            visibleRows.forEachIndexed { index, row ->
                val isLast = index == visibleRows.lastIndex && hiddenCount == 0
                when (row) {
                    is CardRow.TaskRow -> TaskRow(
                        task = row.task,
                        depth = 1,
                        isLast = isLast,
                        onToggle = { onToggleTask(row.task) },
                        onOpen = null,
                    )

                    is CardRow.SubprojectRow -> SubprojectRow(
                        project = row.project,
                        progress = subprojectProgress[row.project.id] ?: ProjectProgress(0, 0),
                        depth = 1,
                        isLast = isLast,
                        onClick = { onOpenSubproject(row.project.id) },
                    )
                }
            }
            if (hiddenCount > 0) {
                TreeRow(depth = 1, isLast = true, connectorInset = (-8).dp) {
                    Text(
                        text = stringResource(R.string.project_more_rows, hiddenCount),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.primary,
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .clickable(onClick = onOpen)
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                    )
                }
            }
        } else if (project.status != ProjectStatus.CANCELLED) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.project_no_tasks_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp),
            )
        }

        Spacer(Modifier.height(8.dp))
        if (project.priority != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_flag),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = scheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.project_priority_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                PriorityChip(priority = project.priority, onClick = onPriorityClick)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_status_postponed),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.project_status_label),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            StatusChip(status = project.status, onClick = onStatusClick)
        }
    }
}

private sealed interface CardRow {
    data class TaskRow(val task: Task) : CardRow
    data class SubprojectRow(val project: Project) : CardRow
}

@Composable
fun TaskRow(
    task: Task,
    depth: Int,
    isLast: Boolean,
    onToggle: () -> Unit,
    onOpen: (() -> Unit)?,
    modifier: Modifier = Modifier,
    ancestorContinues: List<Boolean> = emptyList(),
) {
    val scheme = MaterialTheme.colorScheme
    val contentDescription = stringResource(
        if (task.done) R.string.task_done_description else R.string.task_pending_description,
        task.title,
    )

    TreeRow(
        depth = depth,
        isLast = isLast,
        modifier = modifier.heightIn(min = 48.dp),
        ancestorContinues = ancestorContinues,
        connectorInset = 6.dp,
    ) {
        TaskCheck(
            done = task.done,
            contentDescription = contentDescription,
            onToggle = onToggle,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (onOpen != null) {
                        Modifier.clickable(onClick = onOpen)
                    } else {
                        Modifier
                    },
                )
                .padding(vertical = 4.dp),
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (task.done) scheme.onSurfaceVariant else scheme.onSurface,
                maxLines = 2,
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

@Composable
fun SubprojectRow(
    project: Project,
    progress: ProjectProgress,
    depth: Int,
    isLast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ancestorContinues: List<Boolean> = emptyList(),
    hasChildren: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val extended = LocalExtendedColors.current

    TreeRow(
        depth = depth,
        isLast = isLast,
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        ancestorContinues = ancestorContinues,
        connectorInset = (-8).dp,
        hasChildren = hasChildren,
    ) {
        ProjectIcon(
            icon = project.icon,
            isSubproject = true,
            contentDescription = stringResource(R.string.cd_subproject_icon),
            size = 18.dp,
            tint = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = project.title,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(extended.surfaceAlt)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = if (progress.hasTasks) {
                    stringResource(R.string.progress_fraction, progress.done, progress.total)
                } else {
                    stringResource(R.string.project_no_tasks_short)
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (progress.isComplete) extended.success else scheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier
                .padding(start = 2.dp)
                .size(16.dp),
            tint = scheme.onSurfaceVariant,
        )
    }
}
