package ar.matias.listmaster.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import ar.matias.listmaster.R
import ar.matias.listmaster.domain.model.MAX_TITLE_LENGTH
import ar.matias.listmaster.domain.model.Project
import ar.matias.listmaster.domain.model.TaskList
import ar.matias.listmaster.ui.components.DetailHeader
import ar.matias.listmaster.ui.components.PrimaryButton
import ar.matias.listmaster.ui.components.SelectorRow
import ar.matias.listmaster.ui.sheets.ProjectPickerSheet

@Composable
fun ListFormScreen(
    listId: String?,
    lists: List<TaskList>,
    projects: List<Project>,
    onBack: () -> Unit,
    onCreate: (String, String?) -> Unit,
    onUpdate: (String, String, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val existing = listId?.let { id -> lists.firstOrNull { it.id == id } }
    var title by remember(listId) { mutableStateOf(existing?.title.orEmpty()) }
    var projectId by remember(listId) { mutableStateOf(existing?.projectId) }
    var titleError by remember { mutableStateOf<Int?>(null) }
    var pickerOpen by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val visibleTitleError = titleError ?: if (title.trim().length > MAX_TITLE_LENGTH) {
        R.string.title_too_long
    } else {
        null
    }
    val projectTitle = projectId?.let { id -> projects.firstOrNull { it.id == id }?.title }

    LaunchedEffect(listId) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        DetailHeader(
            title = stringResource(
                if (existing == null) R.string.list_new else R.string.list_edit,
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
                label = { Text(stringResource(R.string.list_title_label)) },
                placeholder = { Text(stringResource(R.string.list_title_placeholder)) },
                singleLine = true,
                isError = visibleTitleError != null,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.Words),
                supportingText = visibleTitleError?.let { error ->
                    { Text(stringResource(error)) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )

            Spacer(Modifier.height(16.dp))
            SelectorRow(
                iconRes = R.drawable.ic_layers,
                label = stringResource(R.string.list_project_label),
                value = projectTitle ?: stringResource(R.string.list_project_none),
                onClick = { pickerOpen = true },
            )

            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = {
                    val clean = title.trim()
                    when {
                        clean.isEmpty() -> titleError = R.string.title_required
                        clean.length > MAX_TITLE_LENGTH -> titleError = R.string.title_too_long
                        existing == null -> onCreate(clean, projectId)
                        else -> onUpdate(existing.id, clean, projectId)
                    }
                },
                enabled = title.isNotBlank() && visibleTitleError == null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (pickerOpen) {
        ProjectPickerSheet(
            projects = projects,
            current = projectId,
            onDismiss = { pickerOpen = false },
            onSelect = {
                projectId = it
                pickerOpen = false
            },
        )
    }
}
