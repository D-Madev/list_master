package ar.matias.listmaster.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ar.matias.listmaster.R
import ar.matias.listmaster.domain.model.MAX_ICON_LENGTH
import ar.matias.listmaster.domain.model.MAX_TITLE_LENGTH
import ar.matias.listmaster.domain.model.Priority
import ar.matias.listmaster.domain.model.Project
import ar.matias.listmaster.domain.model.ProjectStatus
import ar.matias.listmaster.domain.model.Task
import ar.matias.listmaster.ui.chipColors
import ar.matias.listmaster.ui.formatLong
import ar.matias.listmaster.ui.iconRes
import ar.matias.listmaster.ui.labelRes
import ar.matias.listmaster.ui.components.PrimaryButton
import ar.matias.listmaster.ui.components.ProjectIcon
import ar.matias.listmaster.ui.components.projectIconRes
import ar.matias.listmaster.ui.theme.LocalExtendedColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectMenuSheet(
    project: Project,
    canAddSubproject: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onChangeStatus: () -> Unit,
    onAddSubproject: () -> Unit,
    onAddTask: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = project.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            MenuRow(
                label = stringResource(R.string.action_edit),
                onClick = onEdit,
                icon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
            )
            MenuRow(
                label = stringResource(R.string.project_change_status),
                onClick = onChangeStatus,
                icon = { Icon(Icons.Outlined.Check, contentDescription = null) },
            )
            if (canAddSubproject) {
                MenuRow(
                    label = stringResource(R.string.project_add_subproject),
                    onClick = onAddSubproject,
                    icon = { Icon(painterResource(R.drawable.ic_layers), contentDescription = null) },
                )
            }
            MenuRow(
                label = stringResource(R.string.project_add_task),
                onClick = onAddTask,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            MenuRow(
                label = stringResource(R.string.action_delete),
                onClick = onDelete,
                destructive = true,
                icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            )
        }
    }
}

@Composable
fun MenuRow(
    label: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    destructive: Boolean = false,
) {
    val contentColor = if (destructive) {
        LocalExtendedColors.current.danger
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides contentColor,
        ) {
            icon()
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusPickerSheet(
    current: ProjectStatus,
    onDismiss: () -> Unit,
    onSelect: (ProjectStatus) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.project_change_status),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            ProjectStatus.entries.forEach { status ->
                val colors = status.chipColors()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clickable { onSelect(status) }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(status.iconRes()),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = colors.content,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(status.labelRes()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (status == current) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriorityPickerSheet(
    current: Priority?,
    onDismiss: () -> Unit,
    onSelect: (Priority?) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.project_change_priority),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            PriorityRow(
                label = stringResource(R.string.priority_none),
                selected = current == null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = { onSelect(null) },
            )
            Priority.entries.forEach { priority ->
                PriorityRow(
                    label = stringResource(priority.labelRes()),
                    selected = current == priority,
                    tint = priority.chipColors().content,
                    onClick = { onSelect(priority) },
                )
            }
        }
    }
}

@Composable
private fun PriorityRow(
    label: String,
    selected: Boolean,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_flag),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = tint,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun DeleteProjectDialog(
    project: Project,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.project_delete_title, project.title)) },
        text = { Text(stringResource(R.string.project_delete_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.action_delete),
                    color = LocalExtendedColors.current.danger,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFormSheet(
    task: Task?,
    onDismiss: () -> Unit,
    onSave: (String, LocalDate?) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var title by remember(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var dueDate by remember(task?.id) { mutableStateOf(task?.dueDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    val tooLong = title.trim().length > MAX_TITLE_LENGTH
    val extended = LocalExtendedColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            Text(
                text = stringResource(if (task == null) R.string.task_new else R.string.task_edit),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    showError = false
                },
                label = { Text(stringResource(R.string.task_title_label)) },
                placeholder = { Text(stringResource(R.string.task_title_placeholder)) },
                singleLine = true,
                isError = showError || tooLong,
                shape = RoundedCornerShape(14.dp),
                supportingText = when {
                    showError -> {
                        { Text(stringResource(R.string.title_required)) }
                    }

                    tooLong -> {
                        { Text(stringResource(R.string.title_too_long)) }
                    }

                    else -> null
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.DateRange,
                    contentDescription = stringResource(R.string.cd_calendar),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = dueDate?.formatLong() ?: stringResource(R.string.task_no_date),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (dueDate == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f),
                )
                if (dueDate != null) {
                    IconButton(onClick = { dueDate = null }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.action_clear_date),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = {
                    if (title.isBlank()) {
                        showError = true
                    } else if (!tooLong) {
                        onSave(title, dueDate)
                    }
                },
                enabled = title.isNotBlank() && !tooLong,
                modifier = Modifier.fillMaxWidth(),
            )
            if (onDelete != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = extended.danger,
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = dueDate
                ?.atStartOfDay(ZoneOffset.UTC)
                ?.toInstant()
                ?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dueDate = state.selectedDateMillis?.let { millis ->
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.action_accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProjectIconSheet(
    current: String?,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit,
) {
    var emoji by remember(current) {
        mutableStateOf(if (current != null && projectIconRes(current) == null) current else "")
    }
    val scheme = MaterialTheme.colorScheme
    val extended = LocalExtendedColors.current
    val choices = listOf(
        null to R.string.icon_default,
        "layers" to R.string.icon_layers,
        "star" to R.string.icon_star,
        "bolt" to R.string.icon_bolt,
        "heart" to R.string.icon_heart,
        "target" to R.string.icon_target,
        "book" to R.string.icon_book,
        "cart" to R.string.icon_cart,
        "home" to R.string.icon_home,
        "briefcase" to R.string.icon_briefcase,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = scheme.surface,
    ) {
        Column(
            modifier = Modifier
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.icon_pick_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                choices.forEach { (value, labelRes) ->
                    val selected = current == value
                    Column(
                        modifier = Modifier
                            .width(72.dp)
                            .height(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) scheme.primaryContainer else extended.surfaceAlt)
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) scheme.primary else scheme.outline,
                                shape = RoundedCornerShape(16.dp),
                            )
                            .clickable { onSelect(value) }
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        ProjectIcon(
                            icon = value,
                            isSubproject = false,
                            contentDescription = null,
                            size = 24.dp,
                            tint = if (selected) scheme.primary else scheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = emoji,
                onValueChange = { emoji = it.take(MAX_ICON_LENGTH) },
                label = { Text(stringResource(R.string.icon_emoji_label)) },
                placeholder = { Text(stringResource(R.string.icon_emoji_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                text = stringResource(R.string.icon_use_emoji),
                onClick = { onSelect(emoji.trim()) },
                enabled = emoji.isNotBlank() && emoji.trim().length <= MAX_ICON_LENGTH,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
