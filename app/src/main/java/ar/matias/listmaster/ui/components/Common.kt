package ar.matias.listmaster.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ar.matias.listmaster.R
import ar.matias.listmaster.domain.ProjectProgress
import ar.matias.listmaster.domain.model.Priority
import ar.matias.listmaster.domain.model.ProjectStatus
import ar.matias.listmaster.ui.ChipColors
import ar.matias.listmaster.ui.chipColors
import ar.matias.listmaster.ui.iconRes
import ar.matias.listmaster.ui.labelRes
import ar.matias.listmaster.ui.theme.LocalExtendedColors
import kotlinx.coroutines.delay

val EaseOut: CubicBezierEasing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
val EaseStandard: CubicBezierEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(vertical = 8.dp),
    )
}

@Composable
fun EmptyState(
    @DrawableRes iconRes: Int,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.widthIn(max = 320.dp),
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
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            PrimaryButton(text = actionLabel, onClick = onAction)
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 120, easing = EaseOut),
        label = "buttonScale",
    )
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun ProgressPill(
    progress: ProjectProgress,
    modifier: Modifier = Modifier,
) {
    val extended = LocalExtendedColors.current
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(999.dp)
    val description = if (progress.hasTasks) {
        stringResource(
            R.string.progress_description,
            progress.done,
            progress.total,
            progress.percentage,
        )
    } else {
        stringResource(R.string.progress_no_tasks_description)
    }

    Row(
        modifier = modifier
            .clearAndSetSemantics { contentDescription = description }
            .clip(shape)
            .background(scheme.surface)
            .border(1.dp, scheme.outline, shape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_status_done),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(6.dp))
        if (!progress.hasTasks) {
            Text(
                text = stringResource(R.string.project_no_tasks),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(R.string.progress_fraction, progress.done, progress.total),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurface,
            )
            Spacer(Modifier.width(8.dp))
            val animated by animateFloatAsState(
                targetValue = progress.percentage / 100f,
                animationSpec = tween(durationMillis = 300, easing = EaseOut),
                label = "progressBar",
            )
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(5.dp)
                    .clip(shape)
                    .background(extended.surfaceSunken),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animated)
                        .background(extended.success),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${progress.percentage}%",
                style = MaterialTheme.typography.labelMedium,
                color = extended.success,
            )
        }
    }
}

@Composable
fun StatusChip(
    status: ProjectStatus,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Chip(
        label = stringResource(status.labelRes()),
        iconRes = status.iconRes(),
        colors = status.chipColors(),
        modifier = modifier,
        onClick = onClick,
    )
}

@Composable
fun PriorityChip(
    priority: Priority,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Chip(
        label = stringResource(priority.labelRes()),
        iconRes = priority.iconRes(),
        colors = priority.chipColors(),
        modifier = modifier,
        onClick = onClick,
    )
}

/** Chip equivalente para cuando el proyecto no tiene prioridad. */
@Composable
fun NoPriorityChip(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val extended = LocalExtendedColors.current
    Chip(
        label = stringResource(R.string.priority_none),
        iconRes = R.drawable.ic_flag,
        colors = ChipColors(extended.neutral, extended.neutralSoft),
        modifier = modifier,
        onClick = onClick,
    )
}

@Composable
private fun Chip(
    label: String,
    @DrawableRes iconRes: Int,
    colors: ChipColors,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(colors.container)
                .padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = colors.content,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.content,
            )
            if (onClick != null) {
                Spacer(Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = colors.content,
                )
            }
        }
    }
}

/**
 * Checkbox circular con una animación de completado: el círculo se rellena con
 * un rebote corto, el tilde se dibuja de izquierda a derecha y un halo se
 * expande y se apaga detrás. Si [done] ya viene en true al entrar no se anima.
 */
@Composable
fun TaskCheck(
    done: Boolean,
    contentDescription: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalExtendedColors.current.success,
) {
    val outline = MaterialTheme.colorScheme.outline
    val fillColor by animateColorAsState(
        targetValue = if (done) tint else Color.Transparent,
        animationSpec = tween(durationMillis = 220, easing = EaseStandard),
        label = "checkFill",
    )
    val borderColor by animateColorAsState(
        targetValue = if (done) tint else outline,
        animationSpec = tween(durationMillis = 220, easing = EaseStandard),
        label = "checkBorder",
    )

    val drawProgress = remember { Animatable(0f) }
    val pulse = remember { Animatable(1f) }
    val halo = remember { Animatable(1f) }
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(done) {
        if (!initialized) {
            initialized = true
            drawProgress.snapTo(if (done) 1f else 0f)
            return@LaunchedEffect
        }
        if (done) {
            drawProgress.snapTo(0f)
            drawProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 240, easing = EaseOut),
            )
            pulse.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = 300
                    1f at 0
                    1.18f at 110 using EaseOut
                    1f at 300 using EaseStandard
                },
            )
            halo.snapTo(0f)
            halo.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 300, easing = EaseOut),
            )
        } else {
            drawProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 100, easing = EaseStandard),
            )
            pulse.snapTo(1f)
            halo.snapTo(1f)
        }
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .toggleable(
                value = done,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(36.dp)
                .graphicsLayer {
                    val progress = halo.value
                    val scale = 0.72f + progress * 0.5f
                    scaleX = scale
                    scaleY = scale
                    alpha = (1f - progress) * 0.45f
                },
        ) {
            drawCircle(
                color = tint,
                radius = size.minDimension / 2f - 1.dp.toPx(),
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }
        Box(
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }
                .background(fillColor, CircleShape)
                .border(1.5.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val checkColor = MaterialTheme.colorScheme.surface
            Canvas(modifier = Modifier.size(14.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.14f, size.height * 0.52f)
                    lineTo(size.width * 0.4f, size.height * 0.78f)
                    lineTo(size.width * 0.88f, size.height * 0.22f)
                }
                val progress = drawProgress.value
                if (progress <= 0f) return@Canvas
                val measure = PathMeasure().apply { setPath(path, false) }
                val segment = Path()
                measure.getSegment(0f, measure.length * progress, segment, true)
                drawPath(
                    path = segment,
                    color = checkColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }
    }
}

/**
 * Título de tarea o ítem que se tacha al completarse: el color cruza al verde
 * y el tachado se dibuja de izquierda a derecha, línea por línea.
 */
@Composable
fun CompletableText(
    text: String,
    done: Boolean,
    style: TextStyle,
    modifier: Modifier = Modifier,
    pendingColor: Color = MaterialTheme.colorScheme.onSurface,
    doneColor: Color = LocalExtendedColors.current.success,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val color by animateColorAsState(
        targetValue = if (done) doneColor else pendingColor,
        animationSpec = tween(durationMillis = 240, easing = EaseStandard),
        label = "textColor",
    )
    val strike = remember { Animatable(if (done) 1f else 0f) }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    LaunchedEffect(done) {
        strike.animateTo(
            targetValue = if (done) 1f else 0f,
            animationSpec = tween(
                durationMillis = if (done) 300 else 140,
                easing = if (done) EaseOut else EaseStandard,
            ),
        )
    }

    Text(
        text = text,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = { if (layoutResult !== it) layoutResult = it },
        modifier = modifier.drawWithContent {
            drawContent()
            val layout = layoutResult ?: return@drawWithContent
            val progress = strike.value
            if (progress <= 0f) return@drawWithContent
            val total = (0 until layout.lineCount).sumOf { line ->
                (layout.getLineRight(line) - layout.getLineLeft(line)).toDouble()
            }.toFloat()
            if (total <= 0f) return@drawWithContent
            var remaining = total * progress
            val strokeWidth = 1.4.dp.toPx()
            for (line in 0 until layout.lineCount) {
                if (remaining <= 0f) break
                val left = layout.getLineLeft(line)
                val width = layout.getLineRight(line) - left
                if (width <= 0f) continue
                val drawn = width.coerceAtMost(remaining)
                val top = layout.getLineTop(line)
                val bottom = layout.getLineBottom(line)
                val y = top + (bottom - top) * 0.56f
                drawLine(
                    color = doneColor,
                    start = Offset(left, y),
                    end = Offset(left + drawn, y),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
                remaining -= drawn
            }
        },
    )
}

/**
 * Fila con las guías del árbol dibujadas.
 *
 * Cada ancestro dibuja su vertical sólo si su rama sigue abierta. El nivel
 * propio baja desde arriba, hace un codo redondeado hacia el contenido y, si
 * quedan hermanos, sigue bajando. Así las líneas se conectan entre filas y no
 * quedan cortadas en los bordes.
 *
 * [connectorInset] estira la horizontal para que toque el checkbox de la tarea
 * sin meterse debajo del icono de un subproyecto.
 */
@Composable
fun TreeRow(
    depth: Int,
    isLast: Boolean,
    modifier: Modifier = Modifier,
    ancestorContinues: List<Boolean> = emptyList(),
    connectorInset: Dp = 0.dp,
    hasChildren: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val guideColor = MaterialTheme.colorScheme.outline
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                if (depth == 0) return@drawBehind
                val step = 16.dp.toPx()
                val stroke = 1.2.dp.toPx()
                val radius = 8.dp.toPx()
                val middle = size.height / 2f
                val lineStyle = Stroke(width = stroke, cap = StrokeCap.Round)

                // Ancestros: la vertical sigue mientras su rama tenga hermanos.
                for (level in 1 until depth) {
                    if (ancestorContinues.getOrNull(level - 1) != true) continue
                    val x = (level - 1) * step + step / 2f
                    drawLine(
                        color = guideColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }

                val x = (depth - 1) * step + step / 2f
                val endX = x + step + connectorInset.toPx()
                if (isLast) {
                    // La rama termina acá: vertical hasta el codo y curva al contenido.
                    val path = Path().apply {
                        moveTo(x, 0f)
                        lineTo(x, (middle - radius).coerceAtLeast(0f))
                        quadraticBezierTo(x, middle, x + radius, middle)
                        lineTo(endX, middle)
                    }
                    drawPath(path, guideColor, style = lineStyle)
                } else {
                    // Pasa de largo y deja un codo suave hacia el contenido.
                    drawLine(
                        color = guideColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                    val path = Path().apply {
                        moveTo(x, middle - radius)
                        quadraticBezierTo(x, middle, x + radius, middle)
                        lineTo(endX, middle)
                    }
                    drawPath(path, guideColor, style = lineStyle)
                }

                // Si la fila es un proyecto con hijos, la guía baja desde el
                // icono hasta el borde para encontrarse con la primera subtarea.
                if (hasChildren) {
                    val childX = depth * step + step / 2f
                    drawLine(
                        color = guideColor,
                        start = Offset(childX, middle),
                        end = Offset(childX, size.height),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
            }
            .padding(start = (depth * 16).dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * Deja visible una fila recién completada unos instantes aunque "Ver hechas"
 * esté apagado, para que se alcancen a ver el check y el tachado antes de que
 * la fila desaparezca. Después de [durationMillis] se limpia sola.
 */
class CompletionReveal(
    val ids: Set<String>,
    val reveal: (String) -> Unit,
)

@Composable
fun rememberCompletionReveal(durationMillis: Long = 380L): CompletionReveal {
    var ids by remember { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(ids) {
        if (ids.isNotEmpty()) {
            delay(durationMillis)
            ids = emptySet()
        }
    }
    return CompletionReveal(ids) { id -> ids = ids + id }
}
