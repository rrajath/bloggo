package com.rrajath.bloggo.ui.mastodon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rrajath.bloggo.data.SampleData
import com.rrajath.bloggo.designsystem.BloggoTheme
import com.rrajath.bloggo.designsystem.component.Banner
import com.rrajath.bloggo.designsystem.component.BannerTone
import com.rrajath.bloggo.designsystem.component.BloggoAppBar
import com.rrajath.bloggo.designsystem.component.BloggoButton
import com.rrajath.bloggo.designsystem.component.BloggoIconButton
import com.rrajath.bloggo.designsystem.component.BloggoSwitch
import com.rrajath.bloggo.designsystem.component.ButtonTone
import com.rrajath.bloggo.designsystem.component.Eyebrow
import com.rrajath.bloggo.designsystem.icon.BloggoIcon
import com.rrajath.bloggo.designsystem.icon.BloggoIcons
import com.rrajath.bloggo.model.MastodonAccount
import com.rrajath.bloggo.model.TootVisibility

/**
 * One post, several accounts.
 *
 * Character limits are per instance, which is the detail that makes naive
 * cross-posting fail: a message that fits your own server silently 500s on
 * mastodon.social. The counter therefore tracks the strictest *selected* account
 * while the text is shared, and the banner names the instances that would reject
 * it rather than just going red.
 *
 * No image is attached. Mastodon builds its preview card from the site's own Open
 * Graph tags, so whatever image the theme sets there reaches the timeline anyway.
 */
@Composable
fun MastodonScreen(
  accounts: List<MastodonAccount>,
  initialText: String,
  onBack: () -> Unit,
  onToast: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val colors = BloggoTheme.colors
  val state = remember { accounts.toMutableStateList() }
  val perAccount = remember { mutableStateMapOf<String, String>() }
  var sharedText by remember { mutableStateOf(initialText) }
  var sameText by remember { mutableStateOf(true) }
  var activeTab by remember { mutableStateOf(accounts.first { it.selected }.id) }
  var contentWarning by remember { mutableStateOf<String?>(null) }

  val selected = state.filter { it.selected }
  if (selected.none { it.id == activeTab } && selected.isNotEmpty()) activeTab = selected.first().id

  fun textFor(id: String) = if (sameText) sharedText else perAccount[id] ?: initialText

  val tightest = selected.minByOrNull { it.characterLimit }
  val currentText = if (sameText) sharedText else textFor(activeTab)
  val limit = if (sameText) tightest?.characterLimit ?: 500
  else state.first { it.id == activeTab }.characterLimit
  val overLimit = selected.filter { textFor(it.id).length > it.characterLimit }

  Column(modifier.fillMaxSize()) {
    BloggoAppBar(
      title = "Share to Mastodon",
      subtitle = "${selected.size} of ${state.size} accounts",
      onBack = onBack,
      actions = { BloggoIconButton(BloggoIcons.Plus, "Add an account", { onToast("Authorise a new instance") }) },
    )

    Column(
      Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp)
    ) {
      Eyebrow("Post as")
      Column(
        Modifier
          .fillMaxWidth()
          .clip(BloggoTheme.shapes.medium)
          .background(colors.paperRaised)
          .border(1.dp, colors.ruleSoft, BloggoTheme.shapes.medium)
      ) {
        state.forEachIndexed { index, account ->
          AccountRow(
            account = account,
            onToggle = { state[index] = account.copy(selected = !account.selected) },
            onCycleVisibility = {
              val next = TootVisibility.entries[(account.visibility.ordinal + 1) % TootVisibility.entries.size]
              state[index] = account.copy(visibility = next)
            },
            showDivider = index != state.lastIndex,
          )
        }
      }

      Eyebrow("Message")

      if (!sameText && selected.size > 1) {
        Row(
          Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 9.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          selected.forEach { account ->
            val over = textFor(account.id).length > account.characterLimit
            Row(
              Modifier
                .clip(CircleShape)
                .background(if (account.id == activeTab) colors.ink else androidx.compose.ui.graphics.Color.Transparent)
                .border(1.dp, if (account.id == activeTab) colors.ink else colors.rule, CircleShape)
                .clickable { activeTab = account.id }
                .padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                account.instance,
                style = BloggoTheme.type.meta,
                color = if (account.id == activeTab) colors.paper else colors.inkMuted,
              )
              if (over) Box(Modifier.size(5.dp).clip(CircleShape).background(colors.amber))
            }
          }
        }
      }

      Column(
        Modifier
          .fillMaxWidth()
          .clip(BloggoTheme.shapes.medium)
          .background(colors.paperRaised)
          .border(1.dp, colors.rule, BloggoTheme.shapes.medium)
          .padding(horizontal = 14.dp, vertical = 13.dp)
      ) {
        BasicTextField(
          value = currentText,
          onValueChange = { if (sameText) sharedText = it else perAccount[activeTab] = it },
          textStyle = BloggoTheme.type.captureBody.copy(color = colors.ink),
          cursorBrush = SolidColor(colors.accent),
          modifier = Modifier.fillMaxWidth().height(126.dp),
        )

        if (contentWarning != null) {
          Box(
            Modifier
              .fillMaxWidth()
              .padding(top = 9.dp)
              .clip(BloggoTheme.shapes.medium)
              .background(colors.amberTint)
              .padding(horizontal = 14.dp, vertical = 11.dp)
          ) {
            BasicTextField(
              value = contentWarning.orEmpty(),
              onValueChange = { contentWarning = it },
              textStyle = BloggoTheme.type.cellTitle.copy(color = colors.amber),
              cursorBrush = SolidColor(colors.amber),
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.ruleSoft).padding(top = 10.dp))

        Row(
          Modifier.fillMaxWidth().padding(top = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          BloggoIconButton(
            BloggoIcons.Warning,
            "Content warning",
            { contentWarning = if (contentWarning == null) "Long post about AI tooling" else null },
            modifier = Modifier.size(36.dp),
          )
          BloggoIconButton(
            BloggoIcons.Hash,
            "Append the post tags",
            {
              val tags = "\n\n#ai #tooling #craft"
              if (sameText) sharedText += tags else perAccount[activeTab] = textFor(activeTab) + tags
            },
            modifier = Modifier.size(36.dp),
          )
          Text(
            "${currentText.length} / $limit",
            style = BloggoTheme.type.meta,
            color = if (currentText.length > limit) colors.mark else colors.inkFaint,
            modifier = Modifier.weight(1f),
          )
        }
      }

      Row(
        Modifier.fillMaxWidth().padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(Modifier.weight(1f)) {
          Text("Same text everywhere", style = BloggoTheme.type.cellTitle, color = colors.ink)
          Text(
            if (sameText) "One message, posted to every selected account"
            else "Tailor the wording per instance",
            style = BloggoTheme.type.cellSubtitle,
            color = colors.inkFaint,
          )
        }
        BloggoSwitch(
          checked = sameText,
          onCheckedChange = { next ->
            if (!next) selected.forEach { perAccount.putIfAbsent(it.id, sharedText) }
            sameText = next
          },
          contentDescription = "Same text everywhere",
        )
      }

      Banner(
        text = if (overLimit.isEmpty()) {
          "Mastodon builds the preview card from your site, so no image is attached."
        } else {
          "Too long for ${overLimit.joinToString { it.instance }}. Shorten it or turn off shared text."
        },
        tone = if (overLimit.isEmpty()) BannerTone.Info else BannerTone.Warning,
        modifier = Modifier.padding(top = 14.dp, bottom = 22.dp),
      )
    }

    Row(
      Modifier
        .fillMaxWidth()
        .background(colors.paper)
        .padding(horizontal = 18.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
      BloggoButton("Save", { onToast("Saved as a draft toot") }, tone = ButtonTone.Ghost)
      BloggoButton(
        label = if (selected.isEmpty()) "Pick an account" else "Post to ${selected.size} account${if (selected.size > 1) "s" else ""}",
        onClick = { onToast("Posted to ${selected.size} accounts") },
        modifier = Modifier.weight(1f),
        icon = BloggoIcons.Send,
        enabled = selected.isNotEmpty() && overLimit.isEmpty(),
      )
    }
  }
}

@Composable
private fun AccountRow(
  account: MastodonAccount,
  onToggle: () -> Unit,
  onCycleVisibility: () -> Unit,
  showDivider: Boolean,
) {
  val colors = BloggoTheme.colors
  Column {
    Row(
      Modifier
        .fillMaxWidth()
        .clickable(onClick = onToggle)
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        Modifier
          .size(34.dp)
          .clip(BloggoTheme.shapes.thumbnail)
          .background(if (account.selected) colors.accent else colors.ink.copy(alpha = 0.09f)),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          account.avatarLetter,
          style = BloggoTheme.type.displaySmall,
          color = if (account.selected) colors.paper else colors.inkMuted,
        )
      }

      Column(Modifier.weight(1f)) {
        Text(
          account.handle,
          style = BloggoTheme.type.monoField,
          color = if (account.selected) colors.ink else colors.inkFaint,
          maxLines = 1,
        )
        Row(
          Modifier.padding(top = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            account.visibility.label,
            style = BloggoTheme.type.chip,
            color = colors.inkMuted,
            modifier = Modifier.clickable(onClick = onCycleVisibility),
          )
          Text("${account.characterLimit} chars", style = BloggoTheme.type.meta, color = colors.inkFaint)
        }
      }

      Box(
        Modifier
          .size(20.dp)
          .clip(CircleShape)
          .background(if (account.selected) colors.accent else androidx.compose.ui.graphics.Color.Transparent)
          .border(1.5.dp, if (account.selected) colors.accent else colors.rule, CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        if (account.selected) {
          BloggoIcon(BloggoIcons.Check, contentDescription = null, size = 13.dp, tint = colors.paper)
        }
      }
    }
    if (showDivider) {
      Box(Modifier.fillMaxWidth().height(1.dp).background(colors.ruleSoft))
    }
  }
}

private fun List<MastodonAccount>.toMutableStateList() = mutableStateListOf(*toTypedArray())

@Preview(heightDp = 900)
@Composable
private fun MastodonPreview() {
  BloggoTheme {
    MastodonScreen(
      accounts = SampleData.accounts,
      initialText = SampleData.defaultToot,
      onBack = {},
      onToast = {},
    )
  }
}
