package ar.matias.listmaster.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ar.matias.listmaster.R
import ar.matias.listmaster.domain.model.Project
import ar.matias.listmaster.ui.theme.LocalExtendedColors

/** Claves de los íconos incluidos. Cualquier otro valor se muestra como emoji. */
private val BUILT_IN_ICONS = mapOf(
    "layers" to R.drawable.ic_layers,
    "star" to R.drawable.ic_star,
    "bolt" to R.drawable.ic_bolt,
    "heart" to R.drawable.ic_heart,
    "target" to R.drawable.ic_target,
    "book" to R.drawable.ic_book,
    "cart" to R.drawable.ic_cart,
    "home" to R.drawable.ic_home,
    "briefcase" to R.drawable.ic_briefcase,
)

@DrawableRes
fun projectIconRes(icon: String?): Int? = BUILT_IN_ICONS[icon]

@StringRes
fun projectIconLabelRes(icon: String?): Int? = when (icon) {
    "layers" -> R.string.icon_layers
    "star" -> R.string.icon_star
    "bolt" -> R.string.icon_bolt
    "heart" -> R.string.icon_heart
    "target" -> R.string.icon_target
    "book" -> R.string.icon_book
    "cart" -> R.string.icon_cart
    "home" -> R.string.icon_home
    "briefcase" -> R.string.icon_briefcase
    else -> null
}

/**
 * Dibuja el ícono del proyecto: uno de los incluidos, un emoji o el
 * predeterminado (carpeta para proyectos, capas para subproyectos).
 */
@Composable
fun ProjectIcon(
    icon: String?,
    isSubproject: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    val res = projectIconRes(icon)
    when {
        res != null -> Icon(
            painter = painterResource(res),
            contentDescription = contentDescription,
            modifier = modifier.size(size),
            tint = tint,
        )

        !icon.isNullOrBlank() -> {
            val fontSize = with(LocalDensity.current) { (size * 0.8f).toSp() }
            Box(
                modifier = modifier.size(size),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = icon,
                    fontSize = fontSize,
                    lineHeight = fontSize,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }

        else -> Icon(
            painter = painterResource(
                if (isSubproject) R.drawable.ic_layers else R.drawable.ic_folder,
            ),
            contentDescription = contentDescription,
            modifier = modifier.size(size),
            tint = tint,
        )
    }
}

/** Tile con el ícono del proyecto, igual al de la tarjeta de referencia. */
@Composable
fun ProjectIconTile(
    project: Project,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Int = 40,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(LocalExtendedColors.current.surfaceAlt),
        contentAlignment = Alignment.Center,
    ) {
        ProjectIcon(
            icon = project.icon,
            isSubproject = project.parentId != null,
            contentDescription = contentDescription,
            size = (size * 0.55f).dp,
        )
    }
}
