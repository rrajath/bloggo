package com.rrajath.bloggo.ui.editor

import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.noties.markwon.Markwon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    postId: String?,
    onBack: () -> Unit,
    onPublish: (com.rrajath.bloggo.domain.PostDraft) -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val publishState by viewModel.publishState.collectAsStateWithLifecycle()

    LaunchedEffect(postId) {
        viewModel.loadPost(postId)
    }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditorEvent.NavigateBack -> {
                    if (event.message != null) {
                        Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    }
                    onBack()
                }
                is EditorEvent.ShowMessage -> {
                    Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading...")
        }
        return
    }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var bodyFieldValue by remember { mutableStateOf(TextFieldValue(uiState.body)) }

    LaunchedEffect(uiState.body) {
        if (bodyFieldValue.text != uiState.body) {
            bodyFieldValue = TextFieldValue(uiState.body)
        }
    }

    val titleText = if (uiState.isNew) "New post" else "Edit post"

    fun handleBack() {
        if (uiState.dirty) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(titleText) },
                modifier = Modifier.statusBarsPadding(),
                navigationIcon = {
                    IconButton(onClick = { handleBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    EditPreviewToggle(
                        isPreview = uiState.isPreview,
                        onEdit = { viewModel.togglePreview() },
                        onPreview = { viewModel.togglePreview() },
                    )
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isPreview) {
            PreviewPane(
                body = uiState.body,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
            )
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                // bodyHeight is derived from the stable full content area (no Scaffold
                // bottomBar, so maxHeight doesn't change when keyboard opens).
                // Formula keeps Save/Publish visible in the viewport when keyboard is open.
                val bodyHeight = (maxHeight - 408.dp).coerceAtLeast(200.dp)
                val scrollState = rememberScrollState()
                var headerHeightPx by remember { mutableIntStateOf(0) }
                val density = LocalDensity.current
                val imeVisible = WindowInsets.ime.getBottom(density) > 0

                LaunchedEffect(imeVisible) {
                    if (imeVisible) scrollState.animateScrollTo(headerHeightPx)
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Scrollable content — weight(1f) gives up space to the toolbar below.
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp),
                    ) {
                        Column(
                            modifier = Modifier.onSizeChanged { headerHeightPx = it.height },
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = uiState.title,
                                onValueChange = viewModel::onTitleChange,
                                placeholder = { Text("Post title", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.headlineMedium,
                                singleLine = true,
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            SlugRow(
                                slug = uiState.displaySlug,
                                isFrozen = uiState.slugFrozen,
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            FrontMatterCard(
                                isOpen = uiState.isFrontMatterOpen,
                                onToggle = viewModel::toggleFrontMatter,
                                frontMatter = uiState.rawFrontMatter,
                                onFrontMatterChange = viewModel::onFrontMatterChange,
                                isDraft = uiState.draft,
                                onDraftChange = viewModel::setDraft,
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        OutlinedTextField(
                            value = bodyFieldValue,
                            onValueChange = { new ->
                                bodyFieldValue = new
                                viewModel.onBodyChange(new.text)
                            },
                            placeholder = { Text("Write in Markdown...", fontFamily = FontFamily.Monospace) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(bodyHeight),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SavePublishRow(
                            onSaveLocal = { viewModel.saveLocal() },
                            onPublish = {
                                if (viewModel.canPublish()) viewModel.startPublish()
                            },
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Formatting toolbar — lives outside the scroll so it sticks to the
                    // keyboard via imePadding() without affecting bodyHeight stability.
                    FormattingToolbar(
                        wordCount = uiState.wordCount,
                        onBold = {
                            bodyFieldValue = wrapSelection(bodyFieldValue, "**", "**", "bold text")
                            viewModel.onBodyChange(bodyFieldValue.text)
                        },
                        onItalic = {
                            bodyFieldValue = wrapSelection(bodyFieldValue, "*", "*", "italic text")
                            viewModel.onBodyChange(bodyFieldValue.text)
                        },
                        onLink = {
                            bodyFieldValue = insertLink(bodyFieldValue)
                            viewModel.onBodyChange(bodyFieldValue.text)
                        },
                        onImage = {
                            bodyFieldValue = insertImage(bodyFieldValue)
                            viewModel.onBodyChange(bodyFieldValue.text)
                        },
                        onHeading = { level ->
                            bodyFieldValue = applyHeading(bodyFieldValue, level)
                            viewModel.onBodyChange(bodyFieldValue.text)
                        },
                    )
                }
            }
        }
    }

    if (showDiscardDialog) {
        DiscardDialog(
            onKeepEditing = { showDiscardDialog = false },
            onDiscard = {
                showDiscardDialog = false
                viewModel.discardChanges()
            },
        )
    }

    if (publishState.showDraftFlip) {
        DraftFlipDialog(
            onKeepDraft = { viewModel.keepDraft() },
            onFlipAndContinue = { viewModel.confirmDraftFlip() },
        )
    }

    if (publishState.showPushConfirm) {
        var pushData by remember { mutableStateOf<PushConfirmData?>(null) }
        LaunchedEffect(publishState.showPushConfirm) {
            pushData = viewModel.getPushConfirmDataAsync()
        }
        pushData?.let { data ->
            PushConfirmSheet(
                data = data,
                isPushing = publishState.isPushing,
                error = publishState.error,
                onCancel = { viewModel.cancelPublish() },
                onPush = { viewModel.confirmPush() },
            )
        }
    }
}

@Composable
private fun EditPreviewToggle(
    isPreview: Boolean,
    onEdit: () -> Unit,
    onPreview: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(modifier = Modifier.padding(2.dp)) {
            ToggleSegment(
                label = "Edit",
                isSelected = !isPreview,
                onClick = onEdit,
            )
            ToggleSegment(
                label = "Preview",
                isSelected = isPreview,
                onClick = onPreview,
            )
        }
    }
}

@Composable
private fun ToggleSegment(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun SlugRow(
    slug: String,
    isFrozen: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "slug:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Text(
                text = slug.ifBlank { "—" },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontFamily = FontFamily.Monospace,
            )
        }
        if (isFrozen) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Slug frozen",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FrontMatterCard(
    isOpen: Boolean,
    onToggle: () -> Unit,
    frontMatter: String,
    onFrontMatterChange: (String) -> Unit,
    isDraft: Boolean,
    onDraftChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Front matter",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "title · slug · draft managed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isOpen) "▾" else "▸")
            }
            if (isOpen) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Draft",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                        Switch(
                            checked = isDraft,
                            onCheckedChange = onDraftChange,
                            modifier = Modifier.scale(0.7f),
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                OutlinedTextField(
                    value = frontMatter,
                    onValueChange = onFrontMatterChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    minLines = 4,
                )
            }
        }
    }
}

@Composable
private fun PreviewPane(
    body: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val markwon = remember { Markwon.create(context) }
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

    AndroidView(
        factory = { ctx ->
            android.widget.TextView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                setPadding(48, 32, 48, 32)
                textSize = 15f
                setLineSpacing(8f, 1f)
                setTextColor(textColor)
            }
        },
        update = { textView ->
            markwon.setMarkdown(textView, body)
            textView.setTextColor(textColor)
        },
        modifier = modifier,
    )
}

@Composable
private fun FormattingToolbar(
    wordCount: Int,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onLink: () -> Unit,
    onImage: () -> Unit,
    onHeading: (Int) -> Unit,
) {
    var showHeadingFlyout by remember { mutableStateOf(false) }

    // imePadding() makes the Surface grow upward when the keyboard appears,
    // so the toolbar row always sits directly above the keyboard.
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToolbarButton(onClick = onBold, icon = Icons.Default.FormatBold, desc = "Bold")
                ToolbarButton(onClick = onItalic, icon = Icons.Default.FormatItalic, desc = "Italic")
                ToolbarButton(onClick = onLink, icon = Icons.Default.Link, desc = "Link")
                ToolbarButton(onClick = onImage, icon = Icons.Default.Image, desc = "Image")
                TextToolbarButton(
                    onClick = { showHeadingFlyout = !showHeadingFlyout },
                    label = "H",
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "$wordCount words",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (showHeadingFlyout) {
                HeadingFlyout(
                    onPick = { level ->
                        onHeading(level)
                        showHeadingFlyout = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SavePublishRow(
    onSaveLocal: () -> Unit,
    onPublish: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = onSaveLocal,
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save local")
        }
        Button(
            onClick = onPublish,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Publish")
        }
    }
}

@Composable
private fun ToolbarButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = desc,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TextToolbarButton(onClick: () -> Unit, label: String) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun HeadingFlyout(
    onPick: (Int) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            for (level in 1..6) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    onClick = { onPick(level) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "#".repeat(level),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Heading $level",
                            fontSize = (20 - level).sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscardDialog(
    onKeepEditing: () -> Unit,
    onDiscard: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text("Discard changes?") },
        text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text("Discard", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepEditing) {
                Text("Keep editing")
            }
        },
    )
}

private fun wrapSelection(
    value: TextFieldValue,
    before: String,
    after: String,
    placeholder: String,
): TextFieldValue {
    val text = value.text
    val start = value.selection.start
    val end = value.selection.end
    val selectedText = if (start == end) placeholder else text.substring(start, end)
    val newText = text.substring(0, start) + before + selectedText + after + text.substring(end)
    val newSelection = if (start == end) {
        TextRange(start + before.length, start + before.length + selectedText.length)
    } else {
        TextRange(start + before.length, start + before.length + selectedText.length)
    }
    return TextFieldValue(newText, newSelection)
}

private fun insertLink(value: TextFieldValue): TextFieldValue {
    val text = value.text
    val start = value.selection.start
    val end = value.selection.end
    val selectedText = if (start == end) "link text" else text.substring(start, end)
    val newText = text.substring(0, start) + "[$selectedText](https://)" + text.substring(end)
    val urlStart = start + selectedText.length + 3
    return TextFieldValue(newText, TextRange(urlStart, urlStart + 8))
}

private fun insertImage(value: TextFieldValue): TextFieldValue {
    val text = value.text
    val start = value.selection.start
    val end = value.selection.end
    val selectedText = if (start == end) "alt text" else text.substring(start, end)
    val newText = text.substring(0, start) + "![$selectedText](https://)" + text.substring(end)
    val urlStart = start + selectedText.length + 4
    return TextFieldValue(newText, TextRange(urlStart, urlStart + 8))
}

private fun applyHeading(value: TextFieldValue, level: Int): TextFieldValue {
    val text = value.text
    var lineStart = value.selection.start
    while (lineStart > 0 && text[lineStart - 1] != '\n') lineStart--
    var lineEnd = lineStart
    while (lineEnd < text.length && text[lineEnd] != '\n') lineEnd++

    val line = text.substring(lineStart, lineEnd).replace(Regex("^#{1,6}\\s*"), "")
    val newLine = "#".repeat(level) + " " + line
    val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
    val cursor = lineStart + newLine.length
    return TextFieldValue(newText, TextRange(cursor))
}
