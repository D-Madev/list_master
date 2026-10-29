package ar.matias.listmaster.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    modifier: Modifier = Modifier,
) {
    val projectsById = projects.associateBy { it.id }

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
                    ScreenTitle(
                        text = stringResource(R.string.lists_title),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                items(lists, key = { it.id }) { list ->
                    ListCard(
                        list = list,
                        projectTitle = list.projectId?.let { projectsById[it]?.title },
                        onOpen = { onOpenList(list.id) },
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onNewList,
            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.list_new)) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        )
    }
}
