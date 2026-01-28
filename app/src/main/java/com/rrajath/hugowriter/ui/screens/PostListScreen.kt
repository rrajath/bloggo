package com.rrajath.hugowriter.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rrajath.hugowriter.data.Post
import com.rrajath.hugowriter.viewmodel.PostListViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PostListScreen(
    onNavigateToEditor: (String?) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: PostListViewModel = viewModel()
) {
    val posts by viewModel.posts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val deleteResult by viewModel.deleteResult.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncResult by viewModel.syncResult.collectAsState()

    var isUnpublishedExpanded by remember { mutableStateOf(true) }
    var isPublishedExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val unpublishedPosts = posts.filter { !it.isPublished }
    val publishedPosts = posts.filter { it.isPublished }
    val context = LocalContext.current

    // Refresh posts when screen is displayed
    LaunchedEffect(Unit) {
        viewModel.loadPosts()
    }

    // Handle delete result
    LaunchedEffect(deleteResult) {
        deleteResult?.let { result ->
            result.onSuccess {
                snackbarHostState.showSnackbar(
                    message = "Post deleted successfully",
                    duration = SnackbarDuration.Short
                )
            }.onFailure { error ->
                snackbarHostState.showSnackbar(
                    message = "Delete failed: ${error.message}",
                    duration = SnackbarDuration.Long
                )
            }
            viewModel.clearDeleteResult()
        }
    }

    // Handle sync result
    LaunchedEffect(syncResult) {
        syncResult?.let { result ->
            result.onSuccess { count ->
                snackbarHostState.showSnackbar(
                    message = "Synced $count posts from GitHub",
                    duration = SnackbarDuration.Short
                )
            }.onFailure { error ->
                snackbarHostState.showSnackbar(
                    message = "Sync failed: ${error.message}",
                    duration = SnackbarDuration.Long
                )
            }
            viewModel.clearSyncResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bloggo") },
                actions = {
                    Box(
                        modifier = Modifier.combinedClickable(
                            onClick = { if (!isSyncing) viewModel.syncFromGitHub() },
                            onLongClick = {
                                Toast.makeText(context, "Refresh", Toast.LENGTH_SHORT).show()
                            }
                        )
                    ) {
                        IconButton(
                            onClick = { viewModel.syncFromGitHub() },
                            enabled = !isSyncing
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Sync from GitHub")
                            }
                        }
                    }
                    Box(
                        modifier = Modifier.combinedClickable(
                            onClick = onNavigateToSettings,
                            onLongClick = {
                                Toast.makeText(context, "Settings", Toast.LENGTH_SHORT).show()
                            }
                        )
                    ) {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEditor(null) },
                modifier = Modifier.combinedClickable(
                    onClick = { onNavigateToEditor(null) },
                    onLongClick = {
                        Toast.makeText(context, "New Post", Toast.LENGTH_SHORT).show()
                    }
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Post")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search posts...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true
            )

            // Posts List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (posts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) {
                            "No posts found"
                        } else {
                            "No posts yet. Tap + to create one."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Unpublished Section
                    if (unpublishedPosts.isNotEmpty()) {
                        item {
                            CollapsibleSection(
                                title = "Unpublished (${unpublishedPosts.size})",
                                isExpanded = isUnpublishedExpanded,
                                onToggle = { isUnpublishedExpanded = !isUnpublishedExpanded }
                            )
                        }
                        if (isUnpublishedExpanded) {
                            items(unpublishedPosts) { post ->
                                PostListItem(
                                    post = post,
                                    onClick = { onNavigateToEditor(post.id) },
                                    onLongClick = { viewModel.deletePost(post.id) }
                                )
                            }
                        }
                    }

                    // Published Section
                    if (publishedPosts.isNotEmpty()) {
                        item {
                            CollapsibleSection(
                                title = "Published (${publishedPosts.size})",
                                isExpanded = isPublishedExpanded,
                                onToggle = { isPublishedExpanded = !isPublishedExpanded }
                            )
                        }
                        if (isPublishedExpanded) {
                            items(publishedPosts) { post ->
                                PostListItem(
                                    post = post,
                                    onClick = { onNavigateToEditor(post.id) },
                                    onLongClick = { viewModel.deletePost(post.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CollapsibleSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostListItem(
    post: Post,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Post") },
            text = {
                Column {
                    Text("Are you sure you want to delete \"${post.title}\"?")
                    Spacer(modifier = Modifier.height(8.dp))
                    if (post.isPublished) {
                        Text(
                            text = "This will delete the post from GitHub and local storage. This cannot be undone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = "This will delete the post from local storage only. This cannot be undone.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLongClick()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showDeleteDialog = true }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (post.isPublished) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDate(post.getFrontmatterDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${post.getWordCount()} words",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (post.isPublished) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Published",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
