package ar.matias.listmaster.ui.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.Add
import ar.matias.listmaster.R
import ar.matias.listmaster.domain.ProjectTree
import ar.matias.listmaster.domain.model.ListItem
import ar.matias.listmaster.domain.model.Project
import ar.matias.listmaster.domain.model.TaskList
import ar.matias.listmaster.ui.theme.LocalExtendedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListMenuSheet(
    list: TaskList,
    canAddSublist: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onToggleLink: () -> Unit,
    onAddSublist: () -> Unit,
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
                text = list.title,
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
                label = if (list.projectId == null) {
                    stringResource(R.string.list_link)
                } else {
                    stringResource(R.string.list_unlink)
                },
                onClick = onToggleLink,
                icon = { Icon(painterResource(R.drawable.ic_layers), contentDescription = null) },
            )
            if (canAddSublist) {
                MenuRow(
                    label = stringResource(R.string.list_add_sublist),
                    onClick = onAddSublist,
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                )
            }
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
fun DeleteListDialog(
    list: TaskList,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.list_delete_title, list.title)) },
        text = { Text(stringResource(R.string.list_delete_body)) },
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

@Composable
fun ListItemDialog(
    item: ListItem,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var title by remember(item.id) { mutableStateOf(item.title) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.list_item_edit)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.list_item_title_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.list_item_delete),
                        color = LocalExtendedColors.current.danger,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title) },
                enabled = title.isNotBlank(),
            ) {
                Text(stringResource(R.string.action_save))
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
fun ProjectPickerSheet(
    projects: List<Project>,
    current: String?,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit,
) {
    val flattened = flattenProjects(projects)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.list_project_label),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            PickerRow(
                label = stringResource(R.string.list_project_none),
                depth = 0,
                selected = current == null,
                iconRes = R.drawable.ic_list,
                onClick = { onSelect(null) },
            )
            flattened.forEach { (project, depth) ->
                PickerRow(
                    label = project.title,
                    depth = depth,
                    selected = current == project.id,
                    iconRes = if (depth == 0) R.drawable.ic_folder else R.drawable.ic_layers,
                    onClick = { onSelect(project.id) },
                )
            }
        }
    }
}

/**
 * Elige una lista padre con sangría por nivel. Con [noneLabel] aparece una
 * opción al inicio para indicar que no se quiere lista padre.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListPickerSheet(
    title: String,
    options: List<Pair<TaskList, Int>>,
    current: String?,
    onDismiss: () -> Unit,
    onSelect: (TaskList?) -> Unit,
    noneLabel: String? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            if (noneLabel != null) {
                PickerRow(
                    label = noneLabel,
                    depth = 0,
                    selected = current == null,
                    iconRes = R.drawable.ic_list,
                    onClick = { onSelect(null) },
                )
            }
            options.forEach { (list, depth) ->
                PickerRow(
                    label = list.title,
                    depth = depth,
                    selected = current == list.id,
                    iconRes = if (depth == 0) R.drawable.ic_list else R.drawable.ic_layers,
                    onClick = { onSelect(list) },
                )
            }
        }
    }
}

@Composable
private fun PickerRow(
    label: String,
    depth: Int,
    selected: Boolean,
    iconRes: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick)
            .padding(start = (depth * 16).dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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

/** Proyectos en orden de árbol, con su nivel, para los selectores. */
fun flattenProjects(projects: List<Project>): List<Pair<Project, Int>> {
    val childrenByParent = projects.groupBy { it.parentId }
    val result = mutableListOf<Pair<Project, Int>>()

    fun visit(parentId: String?, depth: Int) {
        childrenByParent[parentId].orEmpty().forEach { project ->
            result.add(project to depth)
            visit(project.id, depth + 1)
        }
    }

    // Por si algún día entra un proyecto con padre inexistente: igual se ve.
    visit(null, 0)
    val orphans = projects.filter { project ->
        project.parentId != null && ProjectTree.depthOf(project.id, projects) < 0
    }
    orphans.forEach { orphan ->
        if (result.none { it.first.id == orphan.id }) {
            result.add(orphan to 0)
        }
    }
    return result
}
