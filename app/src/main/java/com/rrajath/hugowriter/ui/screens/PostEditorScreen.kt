package com.rrajath.hugowriter.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rrajath.hugowriter.ui.components.MarkdownRenderer
import com.rrajath.hugowriter.viewmodel.PostEditorViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PostEditorScreen(
    postId: String?,
    onNavigateBack: () -> Unit,
    viewModel: PostEditorViewModel = viewModel()
) {
    val title by viewModel.title.collectAsState()
    val content by viewModel.content.collectAsState()
    val isPreviewMode by viewModel.isPreviewMode.collectAsState()
    val isPublishing by viewModel.isPublishing.collectAsState()
    val publishResult by viewModel.publishResult.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val titleBringIntoViewRequester = remember { BringIntoViewRequester() }
    val contentBringIntoViewRequester = remember { BringIntoViewRequester() }
    val context = LocalContext.current

    LaunchedEffect(postId) {
        viewModel.loadPost(postId)
    }

    LaunchedEffect(publishResult) {
        publishResult?.let { result ->
            result.onSuccess { url ->
                snackbarHostState.showSnackbar(
                    message = "Published successfully!",
                    duration = SnackbarDuration.Short
                )
            }.onFailure { error ->
                snackbarHostState.showSnackbar(
                    message = "Publish failed: ${error.message}",
                    duration = SnackbarDuration.Long
                )
            }
            viewModel.clearPublishResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(if (postId == null) "New Post" else "Edit Post")
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                scope.launch {
                                    viewModel.savePost()
                                    onNavigateBack()
                                }
                            },
                            onLongClick = {
                                Toast.makeText(context, "Back", Toast.LENGTH_SHORT).show()
                            }
                        )
                    ) {
                        IconButton(onClick = {
                            scope.launch {
                                viewModel.savePost()
                                onNavigateBack()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier.combinedClickable(
                            onClick = { viewModel.togglePreviewMode() },
                            onLongClick = {
                                Toast.makeText(
                                    context,
                                    if (isPreviewMode) "Edit" else "Preview",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    ) {
                        TextButton(onClick = { viewModel.togglePreviewMode() }) {
                            Text(if (isPreviewMode) "Edit" else "Preview")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.onTitleChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .bringIntoViewRequester(titleBringIntoViewRequester)
                    .onFocusEvent { focusState ->
                        if (focusState.isFocused) {
                            scope.launch {
                                delay(300)
                                titleBringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
                label = { Text("Title (max 80 characters)") },
                singleLine = true,
                supportingText = { Text("${title.length}/80") },
                isError = title.length > 80
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Content Area - Editor or Preview
            if (isPreviewMode) {
                // Preview Mode
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 300.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        if (content.isNotBlank()) {
                            MarkdownRenderer(markdown = content)
                        } else {
                            Text(
                                text = "Nothing to preview yet...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Editor Mode
                OutlinedTextField(
                    value = content,
                    onValueChange = { viewModel.onContentChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 300.dp)
                        .bringIntoViewRequester(contentBringIntoViewRequester)
                        .onFocusEvent { focusState ->
                            if (focusState.isFocused) {
                                scope.launch {
                                    delay(300)
                                    contentBringIntoViewRequester.bringIntoView()
                                }
                            }
                        },
                    label = { Text("Content (Markdown)") },
                    placeholder = { Text("Write your post content here...") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save as Draft Button
            OutlinedButton(
                onClick = {
                    scope.launch {
                        viewModel.savePost()
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving && title.isNotBlank() && content.isNotBlank(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isSaving) "Saving..." else "Save as Draft")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Publish Button
            Button(
                onClick = { viewModel.publishPost() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isPublishing && title.isNotBlank() && content.isNotBlank()
            ) {
                if (isPublishing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isPublishing) "Publishing..." else "Publish to GitHub")
            }
        }
    }
}
