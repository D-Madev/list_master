package ar.matias.listmaster.ui.screens

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ar.matias.listmaster.R
import ar.matias.listmaster.domain.ListTree
import ar.matias.listmaster.domain.model.Project
import ar.matias.listmaster.domain.model.TaskList
import ar.matias.listmaster.ui.components.CenteredEmpty
import ar.matias.listmaster.ui.components.ListCard
import ar.matias.listmaster.ui.components.ScreenTitle

@Composable
fun ListsScreen(
    lists: List<TaskList>,
    projects: List<Project>,
    onOpenList: (String) -> Unit,
    onNewList: () -> Unit,
    onToggleItem: (String, Boolean) -> Unit,
    showDoneItems: Boolean,
    onShowDoneItemsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val projectsById = projects.associateBy { it.id }
    val roots = remember(lists) { ListTree.childrenOf(null, lists) }
    var expandedIds by rememberSaveable { mutableStateOf(arrayListOf<String>()) }

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
        if (lists.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
            ) {
                ScreenTitle(
                    text = stringResource(R.string.lists_title),
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CenteredEmpty(
                        iconRes = R.drawable.ic_list,
                        title = stringResource(R.string.lists_empty_title),
                        body = stringResource(R.string.lists_empty_body),
                        actionLabel = stringResource(R.string.lists_empty_action),
                        onAction = onNewList,
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
                            text = stringResource(R.string.lists_title),
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier
                                    .heightIn(min = 40.dp)
                                    .clickable { onShowDoneItemsChange(!showDoneItems) },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.done_toggle_short),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Switch(
                                    checked = showDoneItems,
                                    onCheckedChange = onShowDoneItemsChange,
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .scale(0.8f),
                                )
                            }
                            Text(
                                text = stringResource(
                                    if (allExpanded) {
                                        R.string.lists_collapse_all
                                    } else {
                                        R.string.lists_expand_all
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
                items(roots, key = { it.id }) { root ->
                    val rows = remember(lists, showDoneItems) {
                        buildListDetailRows(root, lists, showDone = showDoneItems)
                    }
                    ListCard(
                        list = root,
                        rows = rows,
                        projectTitle = root.projectId?.let { projectsById[it]?.title },
                        expanded = root.id in expandedIds,
                        onToggle = { toggleExpanded(root.id) },
                        onOpen = { onOpenList(root.id) },
                        onToggleItem = { item, done -> onToggleItem(item.id, done) },
                        onOpenSublist = onOpenList,
                        showDoneItems = showDoneItems,
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onNewList,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.list_new),
            )
        }
    }
}

internal sealed interface ListDetailRow {
    val ownerId: String
    val depth: Int
    val isLast: Boolean
    val ancestorContinues: List<Boolean>

    data class ItemRow(
        val item: ar.matias.listmaster.domain.model.ListItem,
        override val ownerId: String,
        override val depth: Int,
        override val isLast: Boolean,
        override val ancestorContinues: List<Boolean>,
    ) : ListDetailRow

    data class SublistItem(
        val list: TaskList,
        val done: Int,
        val total: Int,
        val hasChildren: Boolean,
        val collapsed: Boolean,
        override val ownerId: String,
        override val depth: Int,
        override val isLast: Boolean,
        override val ancestorContinues: List<Boolean>,
    ) : ListDetailRow
}

private fun childIdOf(child: Any): String = when (child) {
    is ar.matias.listmaster.domain.model.ListItem -> child.id
    is TaskList -> child.id
    else -> ""
}

private fun childPositionOf(child: Any): Int = when (child) {
    is ar.matias.listmaster.domain.model.ListItem -> child.position
    is TaskList -> child.position
    else -> 0
}

/**
 * Árbol de una lista: sus ítems pendientes y sus sublistas comparten el mismo
 * orden (se pueden intercalar), y al final van los ítems hechos, si
 * [showDone]. Cada sublista trae su propio subárbol debajo, salvo que esté en
 * [collapsedIds]. [orderOverride] permite mostrar, para un dueño puntual, el
 * orden que se está arrastrando en vivo en vez del que viene de los datos. La
 * raíz no se dibuja como fila; arranca en profundidad 1, igual que con
 * proyectos.
 */
internal fun buildListDetailRows(
    root: TaskList,
    lists: List<TaskList>,
    showDone: Boolean = true,
    collapsedIds: Set<String> = emptySet(),
    orderOverride: Map<String, List<String>> = emptyMap(),
): List<ListDetailRow> {
    val childrenByParent = lists.groupBy { it.parentId }
    val rows = mutableListOf<ListDetailRow>()

    fun emitChildren(owner: TaskList, depth: Int, ancestorContinues: List<Boolean>) {
        val pendingItems = owner.items.filter { !it.done }
        val doneItems = owner.items.filter { it.done }
        val sublists = childrenByParent[owner.id].orEmpty()
        val override = orderOverride[owner.id]
        val pendingChildren: List<Any> = if (override != null) {
            val byId = (pendingItems + sublists).associateBy(::childIdOf)
            override.mapNotNull { byId[it] }
        } else {
            (pendingItems + sublists).sortedWith(compareBy(::childPositionOf, ::childIdOf))
        }
        val items = buildList<Any> {
            addAll(pendingChildren)
            if (showDone) addAll(doneItems)
        }
        items.forEachIndexed { index, item ->
            val isLast = index == items.lastIndex
            when (item) {
                is ar.matias.listmaster.domain.model.ListItem -> rows.add(
                    ListDetailRow.ItemRow(
                        item = item,
                        ownerId = owner.id,
                        depth = depth,
                        isLast = isLast,
                        ancestorContinues = ancestorContinues,
                    ),
                )

                is TaskList -> {
                    val collapsed = item.id in collapsedIds
                    rows.add(
                        ListDetailRow.SublistItem(
                            list = item,
                            done = item.items.count { it.done },
                            total = item.items.size,
                            hasChildren = item.items.any { showDone || !it.done } ||
                                childrenByParent[item.id].orEmpty().isNotEmpty(),
                            collapsed = collapsed,
                            ownerId = owner.id,
                            depth = depth,
                            isLast = isLast,
                            ancestorContinues = ancestorContinues,
                        ),
                    )
                    if (!collapsed) {
                        emitChildren(item, depth + 1, ancestorContinues + !isLast)
                    }
                }
            }
        }
    }

    emitChildren(root, depth = 1, ancestorContinues = emptyList())
    return rows
}
