package ar.matias.listmaster.ui.components

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
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
import ar.matias.listmaster.domain.model.ListItem
import ar.matias.listmaster.domain.model.TaskList
import ar.matias.listmaster.ui.theme.LocalExtendedColors

private const val MAX_LIST_ROWS = 6

@Composable
fun ListCard(
    list: TaskList,
    projectTitle: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onToggleItem: (ListItem, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showDoneItems: Boolean = false,
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
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 180, easing = EaseOut),
        label = "listChevron",
    )
    val darkTheme = scheme.surface.luminance() < 0.5f
    val done = list.items.count { it.done }
    val total = list.items.size
    val fraction = if (total == 0) 0f else done.toFloat() / total
    val shownItems = list.items.filter { !it.done } + if (showDoneItems) list.items.filter { it.done } else emptyList()
    val visibleItems = shownItems.take(MAX_LIST_ROWS)
    val hiddenCount = shownItems.size - visibleItems.size

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
                onClick = onToggle,
            )
            .animateContentSize()
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
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = stringResource(
                    if (expanded) R.string.cd_collapse_list else R.string.cd_expand_list,
                    list.title,
                ),
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { rotationZ = rotation },
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
                color = if (total > 0) extended.success else scheme.onSurface,
            )
            if (total > 0) {
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    ThinProgressBar(fraction = fraction, color = extended.success)
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

        if (expanded) {
            if (visibleItems.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                visibleItems.forEach { item ->
                    ListPreviewRow(
                        item = item,
                        onToggle = { onToggleItem(item, !item.done) },
                        onOpen = onOpen,
                    )
                }
                if (hiddenCount > 0) {
                    Text(
                        text = stringResource(R.string.list_more_items, hiddenCount),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.primary,
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .clickable(onClick = onOpen)
                            .padding(horizontal = 4.dp, vertical = 10.dp),
                    )
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        if (total > 0) R.string.done_all_hidden_title else R.string.list_empty_items,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clickable(onClick = onOpen)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.list_open_detail),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = scheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ListPreviewRow(
    item: ListItem,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    val contentDescription = stringResource(
        if (item.done) R.string.item_done_description else R.string.item_pending_description,
        item.title,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TaskCheck(
            done = item.done,
            contentDescription = contentDescription,
            onToggle = onToggle,
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (item.done) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpen)
                .padding(vertical = 8.dp),
        )
    }
}
