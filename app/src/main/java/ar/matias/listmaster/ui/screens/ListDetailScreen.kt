package ar.matias.listmaster.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ar.matias.listmaster.R
import ar.matias.listmaster.domain.ListTree
import ar.matias.listmaster.domain.model.ListItem
import ar.matias.listmaster.domain.model.MAX_SUBLIST_DEPTH
import ar.matias.listmaster.domain.model.Project
import ar.matias.listmaster.domain.model.TaskList
import ar.matias.listmaster.ui.components.CenteredEmpty
import ar.matias.listmaster.ui.components.CompletableText
import ar.matias.listmaster.ui.components.DetailHeader
import ar.matias.listmaster.ui.components.IconTile
import ar.matias.listmaster.ui.components.TaskCheck
import ar.matias.listmaster.ui.components.ThinProgressBar
import ar.matias.listmaster.ui.components.TreeRow
import ar.matias.listmaster.ui.sheets.DeleteListDialog
import ar.matias.listmaster.ui.sheets.ListItemDialog
import ar.matias.listmaster.ui.sheets.ListMenuSheet
import ar.matias.listmaster.ui.theme.LocalExtendedColors

@OptIn(ExperimentalFoundationApi::class)
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
    onReorderItems: (String, List<String>) -> Unit,
    onAddSublist: (String) -> Unit,
    onOpenSublist: (String) -> Unit,
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
    val projectTitle = list.projectId?.let { id -> projects.firstOrNull { it.id == id }?.title }
    val extended = LocalExtendedColors.current

    // Qué sublistas están replegadas (todas arrancan desplegadas).
    var collapsedIds by remember { mutableStateOf(setOf<String>()) }
    fun toggleCollapsed(id: String) {
        collapsedIds = if (id in collapsedIds) collapsedIds - id else collapsedIds + id
    }

    // Arrastre: se reordenan los hijos pendientes (ítems y sublistas mezclados)
    // de un único dueño a la vez, sea la lista misma o una de sus sublistas.
    var dragOwnerId by remember { mutableStateOf<String?>(null) }
    var draggedChildId by remember { mutableStateOf<String?>(null) }
    val dragChildIds = remember { mutableStateListOf<String>() }
    var dragDelta by remember { mutableStateOf(0f) }

    fun ownerById(ownerId: String): TaskList? =
        if (ownerId == list.id) list else lists.firstOrNull { it.id == ownerId }

    fun naturalPendingChildIds(ownerId: String): List<String> {
        val owner = ownerById(ownerId) ?: return emptyList()
        val pendingItems = owner.items.filter { !it.done }
        val sublists = lists.filter { it.parentId == ownerId }
        return (pendingItems + sublists)
            .sortedWith(
                compareBy(
                    { child -> if (child is ListItem) child.position else (child as TaskList).position },
                    { child -> if (child is ListItem) child.id else (child as TaskList).id },
                ),
            )
            .map { child -> if (child is ListItem) child.id else (child as TaskList).id }
    }

    fun startDrag(ownerId: String, childId: String) {
        dragOwnerId = ownerId
        draggedChildId = childId
        dragChildIds.clear()
        dragChildIds.addAll(naturalPendingChildIds(ownerId))
        dragDelta = 0f
    }

    fun endDrag() {
        val ownerId = dragOwnerId
        if (ownerId != null) onReorderItems(ownerId, dragChildIds.toList())
        dragOwnerId = null
        draggedChildId = null
        dragDelta = 0f
    }

    fun cancelDrag() {
        dragOwnerId = null
        draggedChildId = null
        dragDelta = 0f
    }

    val orderOverride = remember(dragOwnerId, dragChildIds.toList()) {
        dragOwnerId?.let { mapOf(it to dragChildIds.toList()) } ?: emptyMap()
    }
    val rows = remember(list, lists, showDoneItems, collapsedIds, orderOverride) {
        buildListDetailRows(
            root = list,
            lists = lists,
            showDone = showDoneItems,
            collapsedIds = collapsedIds,
            orderOverride = orderOverride,
        )
    }

    val lazyListState = rememberLazyListState()
    val density = LocalDensity.current

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
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            userScrollEnabled = draggedChildId == null,
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

            if (rows.isEmpty()) {
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
                items(
                    rows,
                    key = { row ->
                        when (row) {
                            is ListDetailRow.ItemRow -> "item-${row.item.id}"
                            is ListDetailRow.SublistItem -> "sublist-${row.list.id}"
                        }
                    },
                ) { row ->
                    when (row) {
                        is ListDetailRow.ItemRow -> {
                            val item = row.item
                            val isDraggingThis = draggedChildId == item.id
                            ListItemRow(
                                item = item,
                                depth = row.depth,
                                isLast = row.isLast,
                                ancestorContinues = row.ancestorContinues,
                                onToggle = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleItem(item.id, !item.done)
                                },
                                onOpen = { editingItem = item },
                                modifier = if (!isDraggingThis) Modifier.animateItem() else Modifier,
                                showDragHandle = !item.done,
                                isDragging = isDraggingThis,
                                dragHandleModifier = if (item.done) {
                                    Modifier
                                } else {
                                    Modifier.pointerInput(item.id) {
                                        val itemHeightPx = with(density) { 56.dp.toPx() }
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { _ ->
                                                startDrag(row.ownerId, item.id)
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragDelta += dragAmount.y
                                                val threshold = itemHeightPx * 0.5f
                                                while (dragDelta > threshold) {
                                                    val idx = dragChildIds.indexOf(item.id)
                                                    if (idx < 0 || idx >= dragChildIds.size - 1) break
                                                    dragChildIds.add(idx + 1, dragChildIds.removeAt(idx))
                                                    dragDelta -= itemHeightPx
                                                }
                                                while (dragDelta < -threshold) {
                                                    val idx = dragChildIds.indexOf(item.id)
                                                    if (idx <= 0) break
                                                    dragChildIds.add(idx - 1, dragChildIds.removeAt(idx))
                                                    dragDelta += itemHeightPx
                                                }
                                            },
                                            onDragEnd = { endDrag() },
                                            onDragCancel = { cancelDrag() },
                                        )
                                    }
                                },
                            )
                        }

                        is ListDetailRow.SublistItem -> {
                            val sublist = row.list
                            val isDraggingThis = draggedChildId == sublist.id
                            SublistHeaderRow(
                                list = sublist,
                                done = row.done,
                                total = row.total,
                                depth = row.depth,
                                isLast = row.isLast,
                                ancestorContinues = row.ancestorContinues,
                                hasChildren = row.hasChildren,
                                collapsed = row.collapsed,
                                isDragging = isDraggingThis,
                                modifier = if (!isDraggingThis) Modifier.animateItem() else Modifier,
                                onOpen = { onOpenSublist(sublist.id) },
                                onToggleCollapse = { toggleCollapsed(sublist.id) },
                                dragHandleModifier = Modifier.pointerInput(sublist.id) {
                                    val itemHeightPx = with(density) { 56.dp.toPx() }
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { _ ->
                                            startDrag(row.ownerId, sublist.id)
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragDelta += dragAmount.y
                                            val threshold = itemHeightPx * 0.5f
                                            while (dragDelta > threshold) {
                                                val idx = dragChildIds.indexOf(sublist.id)
                                                if (idx < 0 || idx >= dragChildIds.size - 1) break
                                                dragChildIds.add(idx + 1, dragChildIds.removeAt(idx))
                                                dragDelta -= itemHeightPx
                                            }
                                            while (dragDelta < -threshold) {
                                                val idx = dragChildIds.indexOf(sublist.id)
                                                if (idx <= 0) break
                                                dragChildIds.add(idx - 1, dragChildIds.removeAt(idx))
                                                dragDelta += itemHeightPx
                                            }
                                        },
                                        onDragEnd = { endDrag() },
                                        onDragCancel = { cancelDrag() },
                                    )
                                },
                            )
                        }
                    }
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
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            capitalization = KeyboardCapitalization.Sentences,
                        ),
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
            canAddSublist = ListTree.depthOf(list.id, lists) in 0 until MAX_SUBLIST_DEPTH,
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
            onAddSublist = {
                menuOpen = false
                onAddSublist(list.id)
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
    depth: Int,
    isLast: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    ancestorContinues: List<Boolean> = emptyList(),
    showDragHandle: Boolean = false,
    isDragging: Boolean = false,
    dragHandleModifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val bg = if (isDragging) scheme.primaryContainer.copy(alpha = 0.4f) else Color.Unspecified
    val contentDescription = stringResource(
        if (item.done) R.string.item_done_description else R.string.item_pending_description,
        item.title,
    )
    TreeRow(
        depth = depth,
        isLast = isLast,
        modifier = modifier
            .heightIn(min = 48.dp)
            .background(bg),
        ancestorContinues = ancestorContinues,
        connectorInset = 6.dp,
    ) {
        TaskCheck(
            done = item.done,
            contentDescription = contentDescription,
            onToggle = onToggle,
        )
        CompletableText(
            text = item.title,
            done = item.done,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpen)
                .padding(vertical = 12.dp),
        )
        if (showDragHandle) {
            Icon(
                painter = painterResource(R.drawable.ic_drag_handle),
                contentDescription = stringResource(R.string.cd_drag_handle),
                modifier = Modifier
                    .size(24.dp)
                    .then(dragHandleModifier),
                tint = scheme.onSurfaceVariant.copy(alpha = 0.35f),
            )
        }
    }
}

/**
 * Cabecera de una sublista enlazada, con el mismo estilo de árbol que los
 * subproyectos: tocar el título abre su propio detalle, la flecha repliega o
 * despliega sus hijos acá mismo, y tiene su propio handle de arrastre para
 * moverla entre sus hermanos (ítems u otras sublistas).
 */
@Composable
private fun SublistHeaderRow(
    list: TaskList,
    done: Int,
    total: Int,
    depth: Int,
    isLast: Boolean,
    hasChildren: Boolean,
    collapsed: Boolean,
    onOpen: () -> Unit,
    onToggleCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    ancestorContinues: List<Boolean> = emptyList(),
    isDragging: Boolean = false,
    dragHandleModifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val extended = LocalExtendedColors.current
    val bg = if (isDragging) scheme.primaryContainer.copy(alpha = 0.4f) else Color.Unspecified
    val rotation by animateFloatAsState(
        targetValue = if (collapsed) 0f else 180f,
        animationSpec = tween(durationMillis = 180),
        label = "sublistChevron",
    )

    TreeRow(
        depth = depth,
        isLast = isLast,
        modifier = modifier
            .heightIn(min = 48.dp)
            .background(bg),
        ancestorContinues = ancestorContinues,
        connectorInset = (-8).dp,
        hasChildren = hasChildren && !collapsed,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_list),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = list.title,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpen),
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(extended.surfaceAlt)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = if (total > 0) {
                    stringResource(R.string.progress_fraction, done, total)
                } else {
                    stringResource(R.string.list_no_items_short)
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (total > 0 && done == total) extended.success else scheme.onSurfaceVariant,
            )
        }
        if (hasChildren) {
            IconButton(
                onClick = onToggleCollapse,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = stringResource(
                        if (collapsed) R.string.cd_expand_list else R.string.cd_collapse_list,
                        list.title,
                    ),
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer { rotationZ = rotation },
                    tint = scheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            painter = painterResource(R.drawable.ic_drag_handle),
            contentDescription = stringResource(R.string.cd_drag_handle),
            modifier = Modifier
                .size(24.dp)
                .then(dragHandleModifier),
            tint = scheme.onSurfaceVariant.copy(alpha = 0.35f),
        )
    }
}
