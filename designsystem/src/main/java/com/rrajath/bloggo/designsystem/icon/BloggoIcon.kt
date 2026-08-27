package com.rrajath.bloggo.designsystem.icon

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * An icon as the prototype stores it: one or more SVG path strings on a 24 unit
 * grid, stroked rather than filled.
 *
 * Holding the path data instead of a prebuilt [ImageVector] is what lets stroke
 * width vary with display size, the way the prototype's `.ic`, `.ic-sm` and
 * `.ic-xs` rules do.
 */
@Immutable
data class BloggoIcon(val name: String, val paths: List<String>)

/** The three sizes the prototype uses, in the same order as its CSS. */
object BloggoIconSize {
  /** `.ic` */
  val Medium: Dp = 20.dp
  /** `.ic.sm` */
  val Small: Dp = 16.dp
  /** `.ic.xs` */
  val Tiny: Dp = 13.dp
}

object BloggoIconDefaults {
  const val Stroke = 1.6f
  /** The prototype thickens the stroke at `.ic-xs` so small icons hold up. */
  const val TinyStroke = 1.9f

  fun strokeFor(size: Dp): Float = if (size <= BloggoIconSize.Tiny) TinyStroke else Stroke
}

/**
 * Builds the vector. Not composable so it can be used from previews, tests, and
 * anywhere a raw [ImageVector] is required.
 */
fun BloggoIcon.toImageVector(strokeWidth: Float = BloggoIconDefaults.Stroke): ImageVector =
  ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
  ).apply {
    paths.forEach { data ->
      addPath(
        pathData = addPathNodes(data),
        fill = null,
        // Tinted by Icon's colour filter; the literal colour here never shows.
        stroke = SolidColor(Color.Black),
        strokeLineWidth = strokeWidth,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
      )
    }
  }.build()

@Composable
fun BloggoIcon(
  icon: BloggoIcon,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  size: Dp = BloggoIconSize.Medium,
  tint: Color = LocalContentColor.current,
) {
  val stroke = BloggoIconDefaults.strokeFor(size)
  val vector = remember(icon, stroke) { icon.toImageVector(stroke) }
  Icon(
    imageVector = vector,
    contentDescription = contentDescription,
    modifier = modifier.size(size),
    tint = tint,
  )
}
