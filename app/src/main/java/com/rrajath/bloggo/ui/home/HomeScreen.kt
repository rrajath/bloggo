package com.rrajath.bloggo.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rrajath.bloggo.ui.theme.bloggoColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNewPost: () -> Unit,
    onOpenPost: (String) -> Unit,
    onSettings: () -> Unit,
    onViewLive: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bloggo",
                        style = MaterialTheme.typography.displayLarge,
                    )
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !uiState.refreshing,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = if (uiState.refreshing) {
                                Modifier.rotate(0f)
                            } else {
                                Modifier
                            },
                        )
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewPost,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New post") },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item {
                SearchField(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::setSearch,
                    onClear = viewModel::clearSearch,
                )
            }

            if (uiState.refreshing) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                    )
                }
            }

            uiState.banner?.let { banner ->
                item {
                    HomeBanner(
                        banner = banner,
                        onDismiss = viewModel::dismissBanner,
                        onAction = {
                            if (banner.actionLabel == "Retry") {
                                viewModel.refresh()
                            } else {
                                viewModel.dismissBanner()
                            }
                        },
                    )
                }
            }

            if (uiState.isEmpty) {
                item { EmptyState() }
            } else {
                if (uiState.draftPosts.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Draft",
                            count = uiState.draftPosts.size,
                        )
                    }
                    items(
                        items = uiState.draftPosts,
                        key = { it.post.localId },
                    ) { row ->
                        PostRowCard(
                            row = row,
                            onClick = { onOpenPost(row.post.localId) },
                            onDelete = { viewModel.deletePost(row.post.localId) },
                            onViewLive = null,
                        )
                    }
                }

                if (uiState.publishedPosts.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Published",
                            count = uiState.publishedPosts.size,
                        )
                    }
                    items(
                        items = uiState.publishedPosts,
                        key = { it.post.localId },
                    ) { row ->
                        PostRowCard(
                            row = row,
                            onClick = { onOpenPost(row.post.localId) },
                            onDelete = null,
                            onViewLive = onViewLive,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            androidx.compose.material3.TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search posts...", fontSize = 14.sp) },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.TextFieldDefaults.colors().copy(
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear, modifier = Modifier.size(20.dp)) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeBanner(
    banner: BannerUi,
    onDismiss: () -> Unit,
    onAction: () -> Unit,
) {
    val (bg, fg) = when (banner.type) {
        BannerType.SUCCESS -> MaterialTheme.bloggoColors.successContainer to MaterialTheme.bloggoColors.onSuccessContainer
        BannerType.WARN -> MaterialTheme.bloggoColors.warnContainer to MaterialTheme.bloggoColors.onWarnContainer
        BannerType.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = bg,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = banner.text,
                color = fg,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onAction) {
                Text(banner.actionLabel, color = fg)
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Text(
                text = count.toString(),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PostRowCard(
    row: PostRow,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
    onViewLive: ((String) -> Unit)?,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = row.post.title.ifBlank { "Untitled" },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = row.metaText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (row.isSynced) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.bloggoColors.successContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.bloggoColors.onSuccessContainer,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Synced",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.bloggoColors.onSuccessContainer,
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Local",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (row.isEdited) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    MaterialTheme.bloggoColors.warn,
                                    CircleShape,
                                ),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "edited",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.bloggoColors.warn,
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (onViewLive != null && row.post.slug.isNotBlank()) {
                    TextButton(onClick = { onViewLive(row.post.slug) }) {
                        Text("Live")
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "View live",
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(84.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "B",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No posts yet",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Tap \"+ New post\" to write your first post.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
