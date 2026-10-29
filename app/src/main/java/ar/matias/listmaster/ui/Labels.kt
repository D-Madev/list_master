package ar.matias.listmaster.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ar.matias.listmaster.R
import ar.matias.listmaster.domain.model.Priority
import ar.matias.listmaster.domain.model.ProjectStatus
import ar.matias.listmaster.ui.theme.LocalExtendedColors

data class ChipColors(
    val content: Color,
    val container: Color,
)

@Composable
fun ProjectStatus.chipColors(): ChipColors {
    val extended = LocalExtendedColors.current
    return when (this) {
        ProjectStatus.ACTIVE -> ChipColors(
            content = MaterialTheme.colorScheme.primary,
            container = MaterialTheme.colorScheme.primaryContainer,
        )

        ProjectStatus.PAUSED -> ChipColors(extended.neutral, extended.neutralSoft)
        ProjectStatus.POSTPONED -> ChipColors(extended.warning, extended.warningSoft)
        ProjectStatus.DONE -> ChipColors(extended.success, extended.successSoft)
        ProjectStatus.CANCELLED -> ChipColors(extended.danger, extended.dangerSoft)
    }
}

@StringRes
fun ProjectStatus.labelRes(): Int = when (this) {
    ProjectStatus.ACTIVE -> R.string.status_active
    ProjectStatus.PAUSED -> R.string.status_paused
    ProjectStatus.POSTPONED -> R.string.status_postponed
    ProjectStatus.DONE -> R.string.status_done
    ProjectStatus.CANCELLED -> R.string.status_cancelled
}

@DrawableRes
fun ProjectStatus.iconRes(): Int = when (this) {
    ProjectStatus.ACTIVE -> R.drawable.ic_status_active
    ProjectStatus.PAUSED -> R.drawable.ic_status_paused
    ProjectStatus.POSTPONED -> R.drawable.ic_status_postponed
    ProjectStatus.DONE -> R.drawable.ic_status_done
    ProjectStatus.CANCELLED -> R.drawable.ic_status_cancelled
}

@Composable
fun Priority.chipColors(): ChipColors {
    val extended = LocalExtendedColors.current
    return when (this) {
        Priority.HIGH -> ChipColors(extended.danger, extended.dangerSoft)
        Priority.MEDIUM -> ChipColors(extended.warning, extended.warningSoft)
        Priority.LOW -> ChipColors(extended.neutral, extended.neutralSoft)
    }
}

@StringRes
fun Priority.labelRes(): Int = when (this) {
    Priority.HIGH -> R.string.priority_high
    Priority.MEDIUM -> R.string.priority_medium
    Priority.LOW -> R.string.priority_low
}

@DrawableRes
fun Priority.iconRes(): Int = R.drawable.ic_flag
