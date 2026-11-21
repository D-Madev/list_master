package ar.matias.listmaster.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import ar.matias.listmaster.BuildConfig
import ar.matias.listmaster.R
import ar.matias.listmaster.data.ThemeMode
import ar.matias.listmaster.data.remote.WebSession
import ar.matias.listmaster.ui.components.PrimaryButton
import ar.matias.listmaster.ui.components.ScreenTitle
import ar.matias.listmaster.ui.components.SecondaryButton
import ar.matias.listmaster.ui.components.SectionHeader
import ar.matias.listmaster.ui.components.SurfaceBlock
import ar.matias.listmaster.ui.theme.LocalExtendedColors
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    webSession: WebSession.Snapshot?,
    onStartWebSession: (Int) -> Unit,
    onStopWebSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        ScreenTitle(
            text = stringResource(R.string.settings_title),
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )

        SectionHeader(
            text = stringResource(R.string.settings_theme),
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = stringResource(R.string.settings_theme_help),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        SurfaceBlock {
            ThemeRow(
                iconRes = R.drawable.ic_sun,
                label = stringResource(R.string.settings_theme_light),
                selected = themeMode == ThemeMode.LIGHT,
                onClick = { onThemeChange(ThemeMode.LIGHT) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ThemeRow(
                iconRes = R.drawable.ic_moon,
                label = stringResource(R.string.settings_theme_dark),
                selected = themeMode == ThemeMode.DARK,
                onClick = { onThemeChange(ThemeMode.DARK) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ThemeRow(
                iconRes = R.drawable.ic_phone,
                label = stringResource(R.string.settings_theme_system),
                selected = themeMode == ThemeMode.SYSTEM,
                onClick = { onThemeChange(ThemeMode.SYSTEM) },
            )
        }

        SectionHeader(
            text = stringResource(R.string.settings_data),
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = stringResource(R.string.settings_data_help),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        SurfaceBlock {
            DataRow(
                iconRes = R.drawable.ic_download,
                title = stringResource(R.string.action_export),
                body = stringResource(R.string.settings_export_help),
                onClick = onExport,
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            DataRow(
                iconRes = R.drawable.ic_upload,
                title = stringResource(R.string.action_import),
                body = stringResource(R.string.settings_import_help),
                onClick = onImport,
            )
        }

        SectionHeader(
            text = stringResource(R.string.settings_web_session),
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = stringResource(R.string.settings_web_session_help),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        WebSessionBlock(
            session = webSession,
            onStart = onStartWebSession,
            onStop = onStopWebSession,
        )

        SectionHeader(
            text = stringResource(R.string.settings_about),
            modifier = Modifier.padding(top = 24.dp),
        )
        SurfaceBlock {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.settings_about_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.settings_made),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.settings_offline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun WebSessionBlock(
    session: WebSession.Snapshot?,
    onStart: (Int) -> Unit,
    onStop: () -> Unit,
) {
    var duration by rememberSaveable { mutableIntStateOf(30) }
    SurfaceBlock {
        if (session == null) {
            Text(
                text = stringResource(R.string.web_session_duration),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DurationOption(15, duration, stringResource(R.string.web_session_15)) { duration = it }
                DurationOption(30, duration, stringResource(R.string.web_session_30)) { duration = it }
                DurationOption(60, duration, stringResource(R.string.web_session_60)) { duration = it }
                DurationOption(120, duration, stringResource(R.string.web_session_120)) { duration = it }
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                text = stringResource(R.string.web_session_start),
                onClick = { onStart(duration) },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            WebSessionActive(session = session, onStop = onStop)
        }
    }
}

@Composable
private fun DurationOption(
    minutes: Int,
    selected: Int,
    label: String,
    onSelect: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val isSelected = minutes == selected
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (isSelected) scheme.primary else LocalExtendedColors.current.surfaceAlt)
            .clickable { onSelect(minutes) }
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) scheme.onPrimary else scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WebSessionActive(session: WebSession.Snapshot, onStop: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val remaining by produceState(initialValue = session.remainsMillis(), key1 = session.expiresAt) {
        while (value > 0) {
            delay(1_000)
            value = session.remainsMillis()
        }
    }

    Text(
        text = stringResource(R.string.web_session_link_help),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    SelectionContainer {
        Text(
            text = session.url,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.web_session_code, session.code),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (remaining > 60_000) {
                stringResource(R.string.web_session_remaining_minutes, (remaining / 60_000).toInt() + 1)
            } else {
                stringResource(R.string.web_session_remaining_soon)
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SecondaryButton(
            text = if (copied) {
                stringResource(R.string.web_session_copied)
            } else {
                stringResource(R.string.web_session_copy)
            },
            onClick = {
                clipboard.setText(AnnotatedString(session.url))
                copied = true
            },
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onStop, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.web_session_stop))
        }
    }
}

@Composable
private fun ThemeRow(
    @DrawableRes iconRes: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LocalExtendedColors.current.surfaceAlt),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun DataRow(
    @DrawableRes iconRes: Int,
    title: String,
    body: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(LocalExtendedColors.current.surfaceAlt),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
