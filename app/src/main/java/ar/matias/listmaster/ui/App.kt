package ar.matias.listmaster.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ar.matias.listmaster.R

enum class AppSection(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    Projects(R.string.nav_projects, R.drawable.ic_folder),
    Lists(R.string.nav_lists, R.drawable.ic_list),
    Settings(R.string.nav_settings, R.drawable.ic_tune),
}

@Composable
fun ListMasterApp() {
    var section by rememberSaveable { mutableStateOf(AppSection.Projects) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                )
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    AppSection.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = section == destination,
                            onClick = { section = destination },
                            icon = {
                                Icon(
                                    painter = painterResource(destination.iconRes),
                                    contentDescription = null,
                                )
                            },
                            label = { Text(stringResource(destination.labelRes)) },
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
        },
    ) { innerPadding ->
        when (section) {
            AppSection.Projects -> SectionPlaceholder(
                titleRes = R.string.projects_title,
                iconRes = R.drawable.ic_folder,
                emptyTitleRes = R.string.projects_empty_title,
                emptyBodyRes = R.string.projects_empty_body,
                modifier = Modifier.padding(innerPadding),
            )

            AppSection.Lists -> SectionPlaceholder(
                titleRes = R.string.lists_title,
                iconRes = R.drawable.ic_list,
                emptyTitleRes = R.string.lists_empty_title,
                emptyBodyRes = R.string.lists_empty_body,
                modifier = Modifier.padding(innerPadding),
            )

            AppSection.Settings -> SectionPlaceholder(
                titleRes = R.string.settings_title,
                iconRes = R.drawable.ic_tune,
                emptyTitleRes = R.string.settings_placeholder_title,
                emptyBodyRes = R.string.settings_placeholder_body,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun SectionPlaceholder(
    @StringRes titleRes: Int,
    @DrawableRes iconRes: Int,
    @StringRes emptyTitleRes: Int,
    @StringRes emptyBodyRes: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 320.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(emptyTitleRes),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(emptyBodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
