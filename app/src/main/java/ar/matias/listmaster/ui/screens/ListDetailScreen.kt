package ar.matias.listmaster.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ar.matias.listmaster.R
import ar.matias.listmaster.domain.ListTree
import ar.matias.listmaster.domain.model.ListItem
import ar.matias.listmaster.domain.model.MAX_SUBLIST_DEPTH
import ar.matias.listmaster.domain.model.Project
import ar.matias.listmaster.domain.model.TaskList
import ar.matias.listmaster.ui.components.CenteredEmpty
import ar.matias.listmaster.ui.components.CompletableText
import ar.matias.listmaster.ui.components.DetailHeader
import ar.matias.listmaster.ui.components.EaseOut
import ar.matias.listmaster.ui.components.IconTile
import ar.matias.listmaster.ui.components.TaskCheck
import ar.matias.listmaster.ui.components.rememberCompletionReveal
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
    val completionReveal = rememberCompletionReveal()

    // Qué sublistas están replegadas (todas arrancan desplegadas).
    var collapsedIds by remember { mutableStateOf(setOf<String>()) }
    fun toggleCollapsed(id: String) {
        collapsedIds = if (id in collapsedIds) collapsedIds - id else collapsedIds + id
    }

    // Arrastre: se reordenan los hijos pendientes (ítems y sublistas mezclados)
    // de un único dueño a la vez, sea la lista misma o una de sus sublistas.
    // Los ítems hechos no se arrastran: su lugar queda fijo y los pendientes se
    // permutan alrededor. Mientras dure el arrastre se mantiene el orden nuevo
    // como override hasta que la base lo confirme, para que no parpadee.
    var dragState by remember { mutableStateOf<ListDragState?>(null) }
    var dropSettle by remember { mutableStateOf<DropSettle?>(null) }
    val settleProgress = remember { Animatable(1f) }
    var overrideOwnerId by remember { mutableStateOf<String?>(null) }
    val overrideIds = remember { mutableStateListOf<String>() }
    var listTopPx by remember { mutableFloatStateOf(0f) }

    val lazyListState = rememberLazyListState()
    val density = LocalDensity.current

    fun ownerById(ownerId: String): TaskList? =
        if (ownerId == list.id) list else lists.firstOrNull { it.id == ownerId }

    fun naturalChildIds(ownerId: String): List<String> {
        val owner = ownerById(ownerId) ?: return emptyList()
        val items = owner.items.map { it.id to it.position }
        val sublists = lists.filter { it.parentId == ownerId }.map { it.id to it.position }
        return (items + sublists)
            .sortedWith(compareBy({ it.second }, { it.first }))
            .map { it.first }
    }

    fun naturalPendingChildIds(ownerId: String): List<String> {
        val owner = ownerById(ownerId) ?: return emptyList()
        val pendingItems = owner.items.filter { !it.done }.map { it.id to it.position }
        val sublists = lists.filter { it.parentId == ownerId }.map { it.id to it.position }
        return (pendingItems + sublists)
            .sortedWith(compareBy({ it.second }, { it.first }))
            .map { it.first }
    }

    fun rowKeyOf(ownerId: String, childId: String): String {
        val owner = ownerById(ownerId)
        return if (owner?.items?.any { it.id == childId } == true) {
            "item-$childId"
        } else {
            "sublist-$childId"
        }
    }

    fun startDrag(rowKey: String, ownerId: String, childId: String) {
        if (dragState != null) return
        val pending = naturalPendingChildIds(ownerId)
        if (childId !in pending) return
        val info = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == rowKey } ?: return
        overrideOwnerId = ownerId
        overrideIds.clear()
        overrideIds.addAll(pending)
        dropSettle = null
        dragState = ListDragState(
            ownerId = ownerId,
            childId = childId,
            rowKey = rowKey,
            startTopPx = listTopPx + info.offset,
            rowHeightPx = info.size.toFloat(),
            childIds = overrideIds,
            keysById = naturalChildIds(ownerId).associateWith { rowKeyOf(ownerId, it) },
        )
    }

    fun maybeReorder(state: ListDragState) {
        val layout = lazyListState.layoutInfo
        if (layout.visibleItemsInfo.none { it.key == state.rowKey }) return
        val currentIndex = state.childIds.indexOf(state.childId)
        if (currentIndex < 0) return
        val visualCenter = state.startTopPx + state.dragDy + state.rowHeightPx / 2f
        val indexById = HashMap<String, Int>(state.childIds.size)
        state.childIds.forEachIndexed { index, id -> indexById[id] = index }
        val idByKey = state.keysById.entries.associate { (id, key) -> key to id }
        var target = currentIndex
        layout.visibleItemsInfo.forEach { item ->
            val id = idByKey[item.key] ?: return@forEach
            if (id == state.childId) return@forEach
            val index = indexById[id] ?: return@forEach
            val center = listTopPx + item.offset + item.size / 2f
            if (index > currentIndex && visualCenter > center) {
                target = maxOf(target, index)
            } else if (index < currentIndex && visualCenter < center) {
                target = minOf(target, index)
            }
        }
        if (target != currentIndex) {
            state.childIds.removeAt(currentIndex)
            state.childIds.add(target.coerceIn(0, state.childIds.size), state.childId)
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun endDrag() {
        val state = dragState ?: return
        val info = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == state.rowKey }
        val currentTop = info?.let { listTopPx + it.offset }
        val translation = if (currentTop != null) {
            state.startTopPx + state.dragDy - currentTop
        } else {
            0f
        }
        dragState = null
        onReorderItems(state.ownerId, state.childIds.toList())
        dropSettle = DropSettle(state.childId, translation)
    }

    fun cancelDrag() {
        if (dragState == null) return
        dragState = null
        dropSettle = null
        overrideOwnerId = null
        overrideIds.clear()
    }

    // Auto-scroll: cuanto más cerca del borde superior o inferior está el dedo,
    // más rápido se desplaza la lista, para poder soltar lejos sin perderla.
    LaunchedEffect(dragState?.childId) {
        val state = dragState ?: return@LaunchedEffect
        val zone = with(density) { 80.dp.toPx() }
        val maxSpeed = with(density) { 13.dp.toPx() }
        while (true) {
            withFrameNanos { }
            if (dragState !== state) break
            maybeReorder(state)
            val viewportHeight = lazyListState.layoutInfo.viewportSize.height.toFloat()
            if (viewportHeight <= 0f) continue
            val viewportTop = listTopPx
            val pointerY = state.startTopPx + state.dragDy + state.rowHeightPx / 2f
            val fromTop = pointerY - viewportTop
            val fromBottom = viewportTop + viewportHeight - pointerY
            val scroll = when {
                fromTop < zone -> -maxSpeed * edgeIntensity(fromTop, zone)
                fromBottom < zone -> maxSpeed * edgeIntensity(fromBottom, zone)
                else -> 0f
            }
            if (scroll != 0f) {
                lazyListState.scrollBy(scroll)
            }
        }
    }

    LaunchedEffect(dropSettle) {
        if (dropSettle == null) return@LaunchedEffect
        settleProgress.snapTo(1f)
        settleProgress.animateTo(0f, tween(durationMillis = 180))
        dropSettle = null
    }

    // El override se suelta recién cuando la base ya tiene el orden nuevo.
    LaunchedEffect(overrideOwnerId, lists, dragState) {
        val ownerId = overrideOwnerId ?: return@LaunchedEffect
        if (dragState != null) return@LaunchedEffect
        if (naturalPendingChildIds(ownerId) == overrideIds.toList()) {
            overrideOwnerId = null
            overrideIds.clear()
        }
    }

    val orderOverride = remember(overrideOwnerId, overrideIds.toList()) {
        overrideOwnerId?.let { mapOf(it to overrideIds.toList()) } ?: emptyMap()
    }
    val rows = remember(
        list,
        lists,
        showDoneItems,
        collapsedIds,
        orderOverride,
        completionReveal.ids,
    ) {
        buildListDetailRows(
            root = list,
            lists = lists,
            showDone = showDoneItems,
            collapsedIds = collapsedIds,
            orderOverride = orderOverride,
            extraVisibleIds = completionReveal.ids,
        )
    }

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
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { listTopPx = it.positionInRoot().y },
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            userScrollEnabled = dragState == null,
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
                            val rowKey = "item-${item.id}"
                            val isDraggingThis = dragState?.childId == item.id
                            ListItemRow(
                                item = item,
                                depth = row.depth,
                                isLast = row.isLast,
                                ancestorContinues = row.ancestorContinues,
                                onToggle = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (!item.done && !showDoneItems) completionReveal.reveal(item.id)
                                    onToggleItem(item.id, !item.done)
                                },
                                onOpen = { editingItem = item },
                                modifier = rowModifier(
                                    rowKey = rowKey,
                                    rowId = item.id,
                                    isDragging = isDraggingThis,
                                    dragState = dragState,
                                    dropSettle = dropSettle,
                                    settleProgress = settleProgress,
                                    lazyListState = lazyListState,
                                    listTop = { listTopPx },
                                ),
                                showDragHandle = !item.done,
                                isDragging = isDraggingThis,
                                dragHandleModifier = if (item.done) {
                                    Modifier
                                } else {
                                    Modifier.pointerInput(item.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { _ ->
                                                startDrag(rowKey, row.ownerId, item.id)
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val state = dragState
                                                if (state?.childId == item.id) {
                                                    state.dragDy += dragAmount.y
                                                    maybeReorder(state)
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
                            val rowKey = "sublist-${sublist.id}"
                            val isDraggingThis = dragState?.childId == sublist.id
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
                                modifier = rowModifier(
                                    rowKey = rowKey,
                                    rowId = sublist.id,
                                    isDragging = isDraggingThis,
                                    dragState = dragState,
                                    dropSettle = dropSettle,
                                    settleProgress = settleProgress,
                                    lazyListState = lazyListState,
                                    listTop = { listTopPx },
                                ),
                                onOpen = { onOpenSublist(sublist.id) },
                                onToggleCollapse = { toggleCollapsed(sublist.id) },
                                dragHandleModifier = Modifier.pointerInput(sublist.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { _ ->
                                            startDrag(rowKey, row.ownerId, sublist.id)
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val state = dragState
                                            if (state?.childId == sublist.id) {
                                                state.dragDy += dragAmount.y
                                                maybeReorder(state)
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

/**
 * Estado del arrastre vivo: qué hijo se mueve, dónde empezó y el orden de los
 * hijos arrastrables que se está mostrando. [childIds] es la misma lista que
 * usa el override de la pantalla, así que moverla acá mueve la fila.
 */
private class ListDragState(
    val ownerId: String,
    val childId: String,
    val rowKey: String,
    val startTopPx: Float,
    val rowHeightPx: Float,
    val childIds: SnapshotStateList<String>,
    val keysById: Map<String, String>,
) {
    var dragDy by mutableFloatStateOf(0f)
}

/** Animación corta al soltar: la fila baja desde el dedo hasta su lugar. */
private class DropSettle(
    val childId: String,
    val startTranslation: Float,
)

private fun edgeIntensity(distance: Float, zone: Float): Float {
    val progress = ((zone - distance) / zone).coerceIn(0f, 1f)
    return progress * progress
}

/**
 * Modifier de una fila reordenable: las demás se acomodan con [animateItem] y
 * la que se arrastra se dibuja encima, siguiendo el dedo aunque la lista se
 * desplace sola cerca de los bordes.
 */
@OptIn(ExperimentalFoundationApi::class)
private fun LazyItemScope.rowModifier(
    rowKey: String,
    rowId: String,
    isDragging: Boolean,
    dragState: ListDragState?,
    dropSettle: DropSettle?,
    settleProgress: Animatable<Float, AnimationVector1D>,
    lazyListState: LazyListState,
    listTop: () -> Float,
): Modifier {
    val base = if (isDragging) {
        Modifier
    } else {
        Modifier.animateItem(
            fadeInSpec = null,
            fadeOutSpec = null,
            placementSpec = tween(durationMillis = 180, easing = EaseOut),
        )
    }
    val settleStart = if (!isDragging && dropSettle?.childId == rowId) {
        dropSettle.startTranslation
    } else {
        null
    }
    return base
        .zIndex(if (isDragging) 1f else 0f)
        .graphicsLayer {
            val state = dragState
            if (isDragging && state != null) {
                val info = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == rowKey }
                if (info != null) {
                    translationY = state.startTopPx + state.dragDy - (listTop() + info.offset)
                }
                scaleX = 1.015f
                scaleY = 1.015f
                shadowElevation = 10.dp.toPx()
            } else if (settleStart != null) {
                translationY = settleStart * settleProgress.value
                shadowElevation = 10.dp.toPx() * settleProgress.value
            }
        }
}
