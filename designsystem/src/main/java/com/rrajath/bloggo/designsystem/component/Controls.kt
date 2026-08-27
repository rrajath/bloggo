package com.rrajath.bloggo.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rrajath.bloggo.designsystem.BloggoTheme
import com.rrajath.bloggo.designsystem.icon.BloggoIcon
import com.rrajath.bloggo.designsystem.icon.BloggoIconSize
import com.rrajath.bloggo.designsystem.icon.BloggoIcons

/** Filled is the commit action; ghost is the way out; danger is a destructive
 * ghost for actions like deleting, bordered and labeled in `--mark` red. One
 * filled button per view. */
enum class ButtonTone { Filled, Ghost, Danger }

@Composable
fun BloggoButton(
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  tone: ButtonTone = ButtonTone.Filled,
  icon: BloggoIcon? = null,
  enabled: Boolean = true,
) {
  val colors = BloggoTheme.colors
  val background = when {
    tone == ButtonTone.Ghost || tone == ButtonTone.Danger -> Color.Transparent
    enabled -> colors.ink
    else -> colors.rule
  }
  val content = when {
    tone == ButtonTone.Danger -> colors.mark
    tone == ButtonTone.Ghost -> colors.inkMuted
    else -> colors.paper
  }
  val borderColor = if (tone == ButtonTone.Danger) colors.mark else colors.rule

  Row(
    modifier = modifier
      .clip(BloggoTheme.shapes.button)
      .background(background)
      .then(
        if (tone == ButtonTone.Ghost || tone == ButtonTone.Danger) {
          Modifier.border(1.dp, borderColor, BloggoTheme.shapes.button)
        } else {
          Modifier
        }
      )
      .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 14.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (icon != null) {
      BloggoIcon(icon, contentDescription = null, size = BloggoIconSize.Small, tint = content)
    }
    Text(label, style = BloggoTheme.type.button, color = content)
  }
}

/** Circular icon button, the app bar's default action shape. */
@Composable
fun BloggoIconButton(
  icon: BloggoIcon,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  filled: Boolean = false,
  enabled: Boolean = true,
  tint: Color = if (filled) BloggoTheme.colors.paper else BloggoTheme.colors.inkMuted,
) {
  Box(
    modifier = modifier
      .size(BloggoTheme.spacing.touchTarget)
      .clip(CircleShape)
      .background(if (filled) BloggoTheme.colors.ink else Color.Transparent)
      .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    BloggoIcon(
      icon,
      contentDescription = contentDescription,
      tint = if (enabled) tint else BloggoTheme.colors.inkFaint,
    )
  }
}

/**
 * Pill segmented control. Used for theme, commit vs pull request, and edit vs
 * read. Never more than three segments: past that it is a list, not a switch.
 */
@Composable
fun <T> SegmentedControl(
  options: List<T>,
  selected: T,
  onSelect: (T) -> Unit,
  modifier: Modifier = Modifier,
  label: (T) -> String,
) {
  val colors = BloggoTheme.colors
  Row(
    modifier = modifier
      .clip(CircleShape)
      .background(colors.ink.copy(alpha = 0.08f))
      .padding(3.dp),
    horizontalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    options.forEach { option ->
      val isSelected = option == selected
      val background by animateColorAsState(
        if (isSelected) colors.paperRaised else Color.Transparent,
        label = "segmentBackground",
      )
      Box(
        modifier = Modifier
          .weight(1f)
          .clip(CircleShape)
          .background(background)
          .clickable(role = Role.RadioButton) { onSelect(option) }
          .padding(vertical = 6.dp, horizontal = 4.dp)
          .semantics { stateDescription = if (isSelected) "Selected" else "Not selected" },
        contentAlignment = Alignment.Center,
      ) {
        Text(
          label(option),
          style = BloggoTheme.type.chip,
          color = if (isSelected) colors.ink else colors.inkMuted,
        )
      }
    }
  }
}

/** Scrolling filter pills. Unlike [SegmentedControl] this can hold many options. */
@Composable
fun BloggoPill(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = BloggoTheme.colors
  Box(
    modifier = modifier
      .clip(CircleShape)
      .background(if (selected) colors.ink else Color.Transparent)
      .border(1.dp, if (selected) colors.ink else colors.rule, CircleShape)
      .clickable(role = Role.Tab, onClick = onClick)
      .padding(horizontal = 13.dp, vertical = 7.dp),
  ) {
    Text(
      label,
      style = BloggoTheme.type.chip.copy(fontSize = BloggoTheme.type.cellSubtitle.fontSize),
      color = if (selected) colors.paper else colors.inkMuted,
    )
  }
}

@Composable
fun BloggoSwitch(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  contentDescription: String? = null,
) {
  val colors = BloggoTheme.colors
  val offset by animateDpAsState(if (checked) 19.dp else 0.dp, label = "switchThumb")
  val track by animateColorAsState(if (checked) colors.accent else colors.rule, label = "switchTrack")

  Box(
    modifier = modifier
      .width(46.dp)
      .size(width = 46.dp, height = 27.dp)
      .clip(CircleShape)
      .background(track)
      .clickable(role = Role.Switch) { onCheckedChange(!checked) }
      .then(
        if (contentDescription != null) {
          Modifier.semantics { this.contentDescription = contentDescription }
        } else {
          Modifier
        }
      ),
    contentAlignment = Alignment.CenterStart,
  ) {
    Box(
      Modifier
        .padding(start = 3.dp)
        .offset(x = offset)
        .size(21.dp)
        .clip(CircleShape)
        .background(colors.paperRaised)
    )
  }
}

/**
 * Floating action button. Same dark-circle-on-paper language as
 * [BloggoTabBar]'s raised compose action — a solid `ink` circle with a
 * `paper`-tinted icon — just sized and positioned for a screen corner rather
 * than the tab bar's middle slot, since this app has no stock Material3 FAB
 * anywhere else to match instead.
 */
@Composable
fun BloggoFab(
  icon: BloggoIcon,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = BloggoTheme.colors
  Box(
    modifier = modifier
      .size(56.dp)
      .clip(CircleShape)
      .background(colors.ink)
      .clickable(role = Role.Button, onClick = onClick),
    contentAlignment = Alignment.Center,
  ) {
    BloggoIcon(icon, contentDescription = contentDescription, size = 22.dp, tint = colors.paper)
  }
}

/** A button in the editor's formatting toolbar. */
@Composable
fun ToolbarButton(
  onClick: () -> Unit,
  contentDescription: String,
  modifier: Modifier = Modifier,
  accent: Boolean = false,
  content: @Composable RowScope.() -> Unit,
) {
  Row(
    modifier = modifier
      .size(width = 44.dp, height = 44.dp)
      .clip(BloggoTheme.shapes.thumbnail)
      .clickable(role = Role.Button, onClick = onClick)
      .semantics { this.contentDescription = contentDescription },
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
    content = content,
  )
}

@Preview
@Composable
private fun ControlsPreview() {
  BloggoTheme {
    androidx.compose.foundation.layout.Column(
      Modifier.background(BloggoTheme.colors.paper).padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        BloggoButton("Later", {}, tone = ButtonTone.Ghost)
        BloggoButton("Queue commit", {}, icon = BloggoIcons.Push, modifier = Modifier.weight(1f))
      }
      SegmentedControl(
        options = listOf("Light", "Dark", "Auto"),
        selected = "Auto",
        onSelect = {},
        modifier = Modifier.width(200.dp),
        label = { it },
      )
      Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        BloggoPill("All", true, {})
        BloggoPill("Unused", false, {})
        BloggoPill("Covers", false, {})
      }
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        BloggoSwitch(true, {})
        BloggoSwitch(false, {})
        BloggoIconButton(BloggoIcons.Search, "Search", {})
        BloggoIconButton(BloggoIcons.Plus, "New", {}, filled = true)
        BloggoFab(BloggoIcons.Plus, "Capture a thought", {})
      }
    }
  }
}
