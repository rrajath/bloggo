package com.rrajath.hugowriter.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun FormattingToolbar(
    onBoldClick: () -> Unit,
    onItalicClick: () -> Unit,
    onLinkClick: () -> Unit,
    onCodeClick: () -> Unit,
    onQuoteClick: () -> Unit,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            ToolbarButton(
                icon = Icons.Filled.FormatBold,
                contentDescription = "Bold",
                onClick = onBoldClick
            )
            ToolbarButton(
                icon = Icons.Filled.FormatItalic,
                contentDescription = "Italic",
                onClick = onItalicClick
            )
            ToolbarButton(
                icon = Icons.Filled.Link,
                contentDescription = "Link",
                onClick = onLinkClick
            )
            ToolbarButton(
                icon = Icons.Filled.Code,
                contentDescription = "Code",
                onClick = onCodeClick
            )
            ToolbarButton(
                icon = Icons.Filled.FormatQuote,
                contentDescription = "Quote",
                onClick = onQuoteClick
            )
            ToolbarButton(
                icon = Icons.Filled.Image,
                contentDescription = "Add Image",
                onClick = onImageClick
            )
        }
    }
}

@Composable
private fun ToolbarButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
