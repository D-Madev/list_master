package ar.matias.listmaster.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ar.matias.listmaster.R
import ar.matias.listmaster.domain.model.TaskList
import ar.matias.listmaster.ui.theme.LocalExtendedColors

@Composable
fun ListCard(
    list: TaskList,
    projectTitle: String?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val extended = LocalExtendedColors.current
    val shape = RoundedCornerShape(20.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.995f else 1f,
        animationSpec = tween(durationMillis = 120, easing = EaseOut),
        label = "listCardScale",
    )
    val darkTheme = scheme.surface.luminance() < 0.5f
    val done = list.items.count { it.done }
    val total = list.items.size
    val fraction = if (total == 0) 0f else done.toFloat() / total

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(elevation = if (darkTheme) 0.dp else 3.dp, shape = shape)
            .clip(shape)
            .background(scheme.surface)
            .border(1.dp, scheme.outlineVariant, shape)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onOpen,
            )
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(extended.surfaceAlt),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_list),
                    contentDescription = stringResource(R.string.cd_list_icon),
                    modifier = Modifier.size(22.dp),
                    tint = scheme.primary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = list.title,
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = scheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (total == 0) {
                    stringResource(R.string.list_no_items_short)
                } else {
                    stringResource(R.string.list_progress_fraction, done, total)
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (total > 0 && done == total) extended.success else scheme.onSurface,
            )
            if (total > 0) {
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    ThinProgressBar(
                        fraction = fraction,
                        color = if (done == total) extended.success else extended.highlight,
                    )
                }
            }
        }

        if (projectTitle != null) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(scheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = stringResource(R.string.list_linked_chip, projectTitle),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
