package com.rrajath.bloggo.ui.editor

import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Functions
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rrajath.bloggo.domain.slugify
import io.noties.markwon.Markwon
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    postId: String?,
    onBack: () -> Unit,
    onPublish: (com.rrajath.bloggo.domain.PostDraft) -> Unit,
    onInsertImage: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val publishState by viewModel.publishState.collectAsStateWithLifecycle()

    LaunchedEffect(postId) {
        viewModel.loadPost(postId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditorEvent.NavigateBack -> onBack()
                is EditorEvent.ShowMessage -> {}
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
        bottomBar = {
            if (!uiState.isPreview) {
                EditorBottomBar(
                    wordCount = uiState.wordCount,
                    onSaveLocal = { viewModel.saveLocal() },
                    onPublish = {
                        if (viewModel.canPublish()) {
                            viewModel.startPublish()
                        }
                    },
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
                    onImage = onInsertImage,
                    onHeading = { level ->
                        bodyFieldValue = applyHeading(bodyFieldValue, level)
                        viewModel.onBodyChange(bodyFieldValue.text)
                    },
                )
            }
        },
    ) { innerPadding ->
        if (uiState.isPreview) {
            PreviewPane(
                body = uiState.body,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
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
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = bodyFieldValue,
                    onValueChange = { new ->
                        bodyFieldValue = new
                        viewModel.onBodyChange(new.text)
                    },
                    placeholder = { Text("Write in Markdown...", fontFamily = FontFamily.Monospace) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(80.dp))
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
                    .padding(16.dp),
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
                IconButton(onClick = onToggle) {
                    Text(if (isOpen) "▾" else "▸")
                }
            }
            if (isOpen) {
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val markwon = remember { Markwon.create(context) }

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
            }
        },
        update = { textView ->
            markwon.setMarkdown(textView, body)
        },
        modifier = modifier,
    )
}

@Composable
private fun EditorBottomBar(
    wordCount: Int,
    onSaveLocal: () -> Unit,
    onPublish: () -> Unit,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onLink: () -> Unit,
    onImage: () -> Unit,
    onHeading: (Int) -> Unit,
) {
    var showHeadingFlyout by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
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
                ToolbarButton(
                    onClick = { showHeadingFlyout = !showHeadingFlyout },
                    icon = Icons.Default.Functions,
                    desc = "Heading",
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "$wordCount words",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
                    modifier = Modifier.weight(1.3f),
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
