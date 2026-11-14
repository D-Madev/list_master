package ar.matias.listmaster.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.matias.listmaster.R
import ar.matias.listmaster.data.ThemeMode
import ar.matias.listmaster.ui.screens.ListDetailScreen
import ar.matias.listmaster.ui.screens.ListFormScreen
import ar.matias.listmaster.ui.screens.ListsScreen
import ar.matias.listmaster.ui.screens.PendingScreen
import ar.matias.listmaster.ui.screens.ProjectDetailScreen
import ar.matias.listmaster.ui.screens.ProjectFormScreen
import ar.matias.listmaster.ui.screens.ProjectsScreen
import ar.matias.listmaster.ui.screens.SettingsScreen
import ar.matias.listmaster.ui.theme.ListMasterTheme
import java.time.LocalDate

private fun Screen.isTabScreen(): Boolean =
    this is Screen.Projects || this is Screen.Pending || this is Screen.Lists || this is Screen.Settings

private enum class Tab(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val screen: Screen,
) {
    Pending(R.string.nav_pending, R.drawable.ic_check_list, Screen.Pending),
    Projects(R.string.nav_projects, R.drawable.ic_folder, Screen.Projects),
    Lists(R.string.nav_lists, R.drawable.ic_list, Screen.Lists),
    Settings(R.string.nav_settings, R.drawable.ic_tune, Screen.Settings),
}

@Composable
fun ListMasterApp(viewModel: AppViewModel = viewModel()) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    ListMasterTheme(darkTheme = darkTheme) {
        val data by viewModel.data.collectAsStateWithLifecycle()
        val showDoneTasks by viewModel.showDoneTasks.collectAsStateWithLifecycle()
        val showDoneListItems by viewModel.showDoneListItems.collectAsStateWithLifecycle()
        val projectFilter by viewModel.projectFilter.collectAsStateWithLifecycle()
        val loaded by viewModel.loaded.collectAsStateWithLifecycle()
        val screen = viewModel.navigation.current
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(viewModel) {
            viewModel.messages.collect { message ->
                snackbarHostState.showSnackbar(message)
            }
        }

        BackHandler(enabled = viewModel.navigation.canGoBack) {
            viewModel.back()
        }

        var importDialogOpen by remember { mutableStateOf(false) }
        var importAfterExport by remember { mutableStateOf(false) }

        val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) {
                viewModel.importFrom(uri)
            }
        }

        val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument(
                "application/json",
            ),
        ) { uri ->
            if (uri != null) {
                viewModel.exportTo(uri) { success ->
                    if (success && importAfterExport) {
                        importAfterExport = false
                        importLauncher.launch(arrayOf("*/*"))
                    } else {
                        importAfterExport = false
                    }
                }
            } else {
                importAfterExport = false
            }
        }

        fun launchExport() {
            exportLauncher.launch("list-master-${LocalDate.now()}.json")
        }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                val selected = when (screen) {
                    Screen.Projects -> Tab.Projects
                    Screen.Pending -> Tab.Pending
                    Screen.Lists -> Tab.Lists
                    Screen.Settings -> Tab.Settings
                    else -> null
                }
                if (selected != null) {
                    Column {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ) {
                            Tab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = selected == tab,
                                    onClick = { viewModel.switchTab(tab.screen) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(tab.iconRes),
                                            contentDescription = null,
                                        )
                                    },
                                    label = { Text(stringResource(tab.labelRes)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                )
                            }
                        }
                    }
                }
            },
        ) { innerPadding ->
            val contentModifier = Modifier.padding(innerPadding)
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    if (initialState.isTabScreen() && targetState.isTabScreen()) {
                        // Cambiar de pestaña no se anima: el movimiento no
                        // aporta y hace que la app se sienta más lenta.
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        fadeIn(tween(durationMillis = 220)) togetherWith
                            fadeOut(tween(durationMillis = 180))
                    }
                },
                label = "screen",
            ) { current ->
                if (!loaded) {
                    androidx.compose.foundation.layout.Box(contentModifier) {}
                } else {
                    when (current) {
                        Screen.Projects -> ProjectsScreen(
                            projects = data.projects,
                            tasks = data.tasks,
                            filter = projectFilter,
                            onFilterChange = viewModel::setProjectFilter,
                            onOpenProject = viewModel::openProject,
                            onNewProject = viewModel::newProject,
                            onEditProject = viewModel::editProject,
                            onSetStatus = viewModel::setProjectStatus,
                            onSetPriority = viewModel::setProjectPriority,
                            onDeleteProject = viewModel::deleteProject,
                            showDoneTasks = showDoneTasks,
                            onShowDoneTasksChange = viewModel::setShowDoneTasks,
                            onToggleTask = { task -> viewModel.setTaskDone(task.id, !task.done) },
                            onCreateTask = { projectId, title, dueDate, description, priority ->
                                viewModel.createTask(projectId, title, dueDate, description, priority)
                            },
                            onUpdateTask = { id, title, dueDate, description, priority ->
                                viewModel.updateTask(id, title, dueDate, description, priority)
                            },
                            onDeleteTask = viewModel::deleteTask,
                            modifier = contentModifier,
                        )

                        Screen.Pending -> PendingScreen(
                            projects = data.projects,
                            tasks = data.tasks,
                            onOpenProject = viewModel::openProject,
                            onToggleTask = { task -> viewModel.setTaskDone(task.id, !task.done) },
                            onCreateTask = { projectId, title, dueDate, description, priority ->
                                viewModel.createTask(projectId, title, dueDate, description, priority)
                            },
                            onUpdateTask = { id, title, dueDate, description, priority ->
                                viewModel.updateTask(id, title, dueDate, description, priority)
                            },
                            onDeleteTask = viewModel::deleteTask,
                            modifier = contentModifier,
                        )

                        Screen.Lists -> ListsScreen(
                            lists = data.lists,
                            projects = data.projects,
                            onOpenList = viewModel::openList,
                            onNewList = viewModel::newList,
                            onToggleItem = viewModel::setListItemDone,
                            showDoneItems = showDoneListItems,
                            onShowDoneItemsChange = viewModel::setShowDoneListItems,
                            modifier = contentModifier,
                        )

                        Screen.Settings -> SettingsScreen(
                            themeMode = themeMode,
                            onThemeChange = viewModel::setThemeMode,
                            onExport = { launchExport() },
                            onImport = { importDialogOpen = true },
                            modifier = contentModifier,
                        )

                        is Screen.ProjectDetail -> ProjectDetailScreen(
                            projectId = current.projectId,
                            projects = data.projects,
                            tasks = data.tasks,
                            onBack = viewModel::back,
                            onEdit = viewModel::editProject,
                            onNewSubproject = viewModel::newProject,
                            onOpenProject = viewModel::openProject,
                            onSetStatus = viewModel::setProjectStatus,
                            onSetPriority = viewModel::setProjectPriority,
                            onDeleteProject = viewModel::deleteProject,
                            showDoneTasks = showDoneTasks,
                            onShowDoneTasksChange = viewModel::setShowDoneTasks,
                            onToggleTask = { task -> viewModel.setTaskDone(task.id, !task.done) },
                            onCreateTask = { projectId, title, dueDate, description, priority ->
                                viewModel.createTask(projectId, title, dueDate, description, priority)
                            },
                            onUpdateTask = { id, title, dueDate, description, priority ->
                                viewModel.updateTask(id, title, dueDate, description, priority)
                            },
                            onDeleteTask = viewModel::deleteTask,
                            modifier = contentModifier,
                        )

                        is Screen.ProjectForm -> ProjectFormScreen(
                            projectId = current.projectId,
                            parentId = current.parentId,
                            projects = data.projects,
                            onBack = viewModel::back,
                            onCreate = { title, parentId, status, priority, icon ->
                                viewModel.createProject(title, parentId, status, priority, icon)
                            },
                            onUpdate = { id, title, status, priority, icon, newParentId ->
                                viewModel.updateProject(id, title, status, priority, icon, newParentId)
                            },
                            modifier = contentModifier,
                        )

                        is Screen.ListDetail -> ListDetailScreen(
                            listId = current.listId,
                            lists = data.lists,
                            projects = data.projects,
                            onBack = viewModel::back,
                            onEdit = viewModel::editList,
                            onDelete = viewModel::deleteList,
                            onSetProject = { id, projectId ->
                                val list = data.lists.firstOrNull { it.id == id }
                                if (list != null) {
                                    viewModel.updateList(id, list.title, projectId, list.parentId)
                                }
                            },
                            onOpenProject = viewModel::openProject,
                            showDoneItems = showDoneListItems,
                            onShowDoneItemsChange = viewModel::setShowDoneListItems,
                            onAddItem = viewModel::addListItem,
                            onToggleItem = viewModel::setListItemDone,
                            onUpdateItemTitle = viewModel::updateListItemTitle,
                            onDeleteItem = viewModel::deleteListItem,
                            onReorderItems = { listId, itemIds ->
                                viewModel.reorderListItems(listId, itemIds)
                            },
                            onAddSublist = { listId -> viewModel.newList(parentId = listId) },
                            onOpenSublist = viewModel::openList,
                            modifier = contentModifier,
                        )

                        is Screen.ListForm -> ListFormScreen(
                            listId = current.listId,
                            parentId = current.parentId,
                            lists = data.lists,
                            projects = data.projects,
                            onBack = viewModel::back,
                            onCreate = { title, projectId, newParentId ->
                                viewModel.createList(title, projectId, newParentId)
                            },
                            onUpdate = { id, title, projectId, newParentId ->
                                viewModel.updateList(id, title, projectId, newParentId)
                            },
                            modifier = contentModifier,
                        )
                    }
                }
            }
        }

        if (importDialogOpen) {
            AlertDialog(
                onDismissRequest = { importDialogOpen = false },
                title = { Text(stringResource(R.string.import_confirm_title)) },
                text = { Text(stringResource(R.string.import_confirm_body)) },
                confirmButton = {
                    androidx.compose.foundation.layout.Row {
                        TextButton(
                            onClick = {
                                importDialogOpen = false
                                importAfterExport = true
                                launchExport()
                            },
                        ) {
                            Text(stringResource(R.string.action_export_first))
                        }
                        TextButton(
                            onClick = {
                                importDialogOpen = false
                                importLauncher.launch(arrayOf("*/*"))
                            },
                        ) {
                            Text(stringResource(R.string.action_import_anyway))
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { importDialogOpen = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }
    }
}
