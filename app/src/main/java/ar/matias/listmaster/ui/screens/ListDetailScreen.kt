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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ar.matias.listmaster.R
import ar.matias.listmaster.domain.model.ListItem
import ar.matias.listmaster.domain.model.Project
import ar.matias.listmaster.domain.model.TaskList
import ar.matias.listmaster.ui.components.CenteredEmpty
import ar.matias.listmaster.ui.components.DetailHeader
import ar.matias.listmaster.ui.components.IconTile
import ar.matias.listmaster.ui.components.TaskCheck
import ar.matias.listmaster.ui.components.ThinProgressBar
import ar.matias.listmaster.ui.sheets.DeleteListDialog
import ar.matias.listmaster.ui.sheets.ListItemDialog
import ar.matias.listmaster.ui.sheets.ListMenuSheet
import ar.matias.listmaster.ui.theme.LocalExtendedColors

@Composable
fun ListDetailScreen(
    listId: String,
    lists: List<TaskList>,
    projects: List<Project>,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onSetProject: (String, String?) -> Unit,
    onOpenProject: (String) -> Unit,
    showDoneItems: Boolean,
    onShowDoneItemsChange: (Boolean) -> Unit,
    onAddItem: (String, String) -> Unit,
    onToggleItem: (String, Boolean) -> Unit,
    onUpdateItemTitle: (String, String) -> Unit,
    onDeleteItem: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val list = lists.firstOrNull { it.id == listId }
    var menuOpen by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ListItem?>(null) }
    var newItem by remember { mutableStateOf("") }
    val haptics = LocalHapticFeedback.current

    if (list == null) {
        Column(modifier = modifier.fillMaxSize()) {
            DetailHeader(
                title = stringResource(R.string.lists_title),
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
                    iconRes = R.drawable.ic_list,
                    title = stringResource(R.string.list_not_found),
                    body = "",
                )
            }
        }
        return
    }

    val done = list.items.count { it.done }
    val total = list.items.size
    val shownItems = list.items.filter { !it.done } + if (showDoneItems) list.items.filter { it.done } else emptyList()
    val projectTitle = list.projectId?.let { id -> projects.firstOrNull { it.id == id }?.title }
    val extended = LocalExtendedColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        DetailHeader(
            title = list.title,
            onBack = onBack,
            modifier = Modifier.padding(horizontal = 8.dp),
            actions = {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.cd_menu, list.title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "summary") {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(
                            iconRes = R.drawable.ic_list,
                            contentDescription = stringResource(R.string.cd_list_icon),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = list.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (total == 0) {
                                stringResource(R.string.list_no_items_short)
                            } else {
                                stringResource(R.string.list_progress_fraction, done, total)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (total > 0) {
                                extended.success
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                        if (total > 0) {
                            Spacer(Modifier.width(12.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                ThinProgressBar(
                                    fraction = done.toFloat() / total,
                                    color = extended.success,
                                )
                            }
                        }
                    }
                    if (projectTitle != null && list.projectId != null) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .clickable { onOpenProject(list.projectId) }
                                .padding(vertical = 4.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.list_linked_to, projectTitle),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (done > 0) {
                item(key = "done-toggle") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { onShowDoneItemsChange(!showDoneItems) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.done_toggle_items_count, done),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = showDoneItems,
                            onCheckedChange = onShowDoneItemsChange,
                        )
                    }
                }
            }

            if (list.items.isEmpty()) {
                item(key = "empty") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.list_empty_items),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.list_empty_items_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(shownItems, key = { it.id }) { item ->
                    ListItemRow(
                        item = item,
                        onToggle = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleItem(item.id, !item.done)
                        },
                        onOpen = { editingItem = item },
                    )
                }
            }

            item(key = "add") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = newItem,
                        onValueChange = { newItem = it },
                        placeholder = { Text(stringResource(R.string.list_item_placeholder)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (newItem.isNotBlank()) {
                                    onAddItem(list.id, newItem)
                                    newItem = ""
                                }
                            },
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            onAddItem(list.id, newItem)
                            newItem = ""
                        },
                        enabled = newItem.isNotBlank(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.cd_add_item),
                            tint = if (newItem.isNotBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }

    if (menuOpen) {
        ListMenuSheet(
            list = list,
            onDismiss = { menuOpen = false },
            onEdit = {
                menuOpen = false
                onEdit(list.id)
            },
            onToggleLink = {
                menuOpen = false
                if (list.projectId == null) {
                    onEdit(list.id)
                } else {
                    onSetProject(list.id, null)
                }
            },
            onDelete = {
                menuOpen = false
                deleteOpen = true
            },
        )
    }

    if (deleteOpen) {
        DeleteListDialog(
            list = list,
            onDismiss = { deleteOpen = false },
            onConfirm = {
                deleteOpen = false
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onDelete(list.id)
            },
        )
    }

    editingItem?.let { item ->
        ListItemDialog(
            item = item,
            onDismiss = { editingItem = null },
            onSave = { title ->
                onUpdateItemTitle(item.id, title)
                editingItem = null
            },
            onDelete = {
                onDeleteItem(item.id)
                editingItem = null
            },
        )
    }
}

@Composable
private fun ListItemRow(
    item: ListItem,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    val contentDescription = stringResource(
        if (item.done) R.string.item_done_description else R.string.item_pending_description,
        item.title,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TaskCheck(
            done = item.done,
            contentDescription = contentDescription,
            onToggle = onToggle,
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (item.done) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpen)
                .padding(vertical = 12.dp),
        )
    }
}
