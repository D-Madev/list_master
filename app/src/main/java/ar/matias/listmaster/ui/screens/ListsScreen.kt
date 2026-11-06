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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ar.matias.listmaster.R
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
    var expandedIds by rememberSaveable { mutableStateOf(arrayListOf<String>()) }

    val allExpanded = lists.isNotEmpty() && lists.all { it.id in expandedIds }

    fun toggleExpanded(id: String) {
        expandedIds = ArrayList(expandedIds).also { ids ->
            if (!ids.remove(id)) ids.add(id)
        }
    }

    fun setAllExpanded(expanded: Boolean) {
        expandedIds = if (expanded) {
            ArrayList(lists.map { it.id })
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
                items(lists, key = { it.id }) { list ->
                    ListCard(
                        list = list,
                        projectTitle = list.projectId?.let { projectsById[it]?.title },
                        expanded = list.id in expandedIds,
                        onToggle = { toggleExpanded(list.id) },
                        onOpen = { onOpenList(list.id) },
                        onToggleItem = { item, done -> onToggleItem(item.id, done) },
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
