package ar.matias.listmaster.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import ar.matias.listmaster.R
import ar.matias.listmaster.domain.ProjectTree
import ar.matias.listmaster.domain.model.MAX_SUBPROJECT_DEPTH
import ar.matias.listmaster.domain.model.MAX_TITLE_LENGTH
import androidx.compose.ui.text.input.KeyboardCapitalization
import ar.matias.listmaster.domain.model.Priority
import ar.matias.listmaster.domain.model.Project
import ar.matias.listmaster.domain.model.ProjectStatus
import ar.matias.listmaster.ui.components.DetailHeader
import ar.matias.listmaster.ui.components.PrimaryButton
import ar.matias.listmaster.ui.components.ProjectIcon
import ar.matias.listmaster.ui.components.SelectorRow
import ar.matias.listmaster.ui.components.projectIconLabelRes
import ar.matias.listmaster.ui.iconRes
import ar.matias.listmaster.ui.labelRes
import ar.matias.listmaster.ui.sheets.PriorityPickerSheet
import ar.matias.listmaster.ui.sheets.ProjectPickerSheet
import ar.matias.listmaster.ui.sheets.ProjectIconSheet
import ar.matias.listmaster.ui.sheets.StatusPickerSheet

@Composable
fun ProjectFormScreen(
    projectId: String?,
    parentId: String?,
    projects: List<Project>,
    onBack: () -> Unit,
    onCreate: (String, String?, ProjectStatus, Priority?, String?) -> Unit,
    onUpdate: (String, String, ProjectStatus, Priority?, String?, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val existing = projectId?.let { id -> projects.firstOrNull { it.id == id } }
    var title by remember(projectId) { mutableStateOf(existing?.title.orEmpty()) }
    var status by remember(projectId) { mutableStateOf(existing?.status ?: ProjectStatus.ACTIVE) }
    var priority by remember(projectId) { mutableStateOf(existing?.priority) }
    var icon by remember(projectId) { mutableStateOf(existing?.icon) }
    var titleError by remember { mutableStateOf<Int?>(null) }
    var statusOpen by remember { mutableStateOf(false) }
    var priorityOpen by remember { mutableStateOf(false) }
    var iconOpen by remember { mutableStateOf(false) }
    var parentOpen by remember { mutableStateOf(false) }
    var selectedParentId by remember(projectId) { mutableStateOf(existing?.parentId ?: parentId) }
    val parentOptions = remember(existing?.id, projects) {
        when {
            existing != null -> ProjectTree.validParents(existing.id, projects)
                .map { it to ProjectTree.depthOf(it.id, projects) }
            else -> projects
                .filter { ProjectTree.depthOf(it.id, projects) < MAX_SUBPROJECT_DEPTH }
                .map { it to ProjectTree.depthOf(it.id, projects) }
        }
    }
    val focusRequester = remember { FocusRequester() }
    val visibleTitleError = titleError ?: if (title.trim().length > MAX_TITLE_LENGTH) {
        R.string.title_too_long
    } else {
        null
    }
    val parentTitle = selectedParentId?.let { id -> projects.firstOrNull { it.id == id }?.title }

    LaunchedEffect(projectId) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        DetailHeader(
            title = stringResource(
                if (existing == null) R.string.project_new else R.string.project_edit,
            ),
            onBack = onBack,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = null
                },
                label = { Text(stringResource(R.string.project_title_label)) },
                placeholder = { Text(stringResource(R.string.project_title_placeholder)) },
                singleLine = true,
                isError = visibleTitleError != null,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.Sentences),
                supportingText = visibleTitleError?.let { error ->
                    { Text(stringResource(error)) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )

            Spacer(Modifier.height(16.dp))
            SelectorRow(
                iconRes = R.drawable.ic_folder,
                label = stringResource(R.string.project_icon_label),
                value = icon?.let { value ->
                    projectIconLabelRes(value)?.let { stringResource(it) } ?: value
                } ?: stringResource(R.string.icon_default),
                onClick = { iconOpen = true },
                leading = {
                    ProjectIcon(
                        icon = icon,
                        isSubproject = selectedParentId != null,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            SelectorRow(
                iconRes = status.iconRes(),
                label = stringResource(R.string.project_status_label),
                value = stringResource(status.labelRes()),
                onClick = { statusOpen = true },
            )
            SelectorRow(
                iconRes = R.drawable.ic_flag,
                label = stringResource(R.string.project_priority_label),
                value = priority?.let { stringResource(it.labelRes()) }
                    ?: stringResource(R.string.priority_none),
                onClick = { priorityOpen = true },
            )
            SelectorRow(
                iconRes = R.drawable.ic_layers,
                label = stringResource(R.string.project_parent_label),
                value = parentTitle ?: stringResource(R.string.project_parent_none),
                onClick = { parentOpen = true },
            )

            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = {
                    val clean = title.trim()
                    when {
                        clean.isEmpty() -> titleError = R.string.title_required
                        clean.length > MAX_TITLE_LENGTH -> titleError = R.string.title_too_long
                        existing == null -> onCreate(clean, selectedParentId, status, priority, icon)
                        else -> onUpdate(
                            existing.id,
                            clean,
                            status,
                            priority,
                            icon,
                            selectedParentId,
                        )
                    }
                },
                enabled = title.isNotBlank() && visibleTitleError == null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (statusOpen) {
        StatusPickerSheet(
            current = status,
            onDismiss = { statusOpen = false },
            onSelect = {
                status = it
                statusOpen = false
            },
        )
    }

    if (priorityOpen) {
        PriorityPickerSheet(
            current = priority,
            onDismiss = { priorityOpen = false },
            onSelect = {
                priority = it
                priorityOpen = false
            },
        )
    }

    if (parentOpen) {
        ProjectPickerSheet(
            title = stringResource(R.string.project_parent_pick_title),
            options = parentOptions,
            current = selectedParentId,
            onDismiss = { parentOpen = false },
            onSelect = {
                selectedParentId = it?.id
                parentOpen = false
            },
            noneLabel = stringResource(R.string.project_parent_none),
        )
    }

    if (iconOpen) {
        ProjectIconSheet(
            current = icon,
            onDismiss = { iconOpen = false },
            onSelect = {
                icon = it
                iconOpen = false
            },
        )
    }
}
