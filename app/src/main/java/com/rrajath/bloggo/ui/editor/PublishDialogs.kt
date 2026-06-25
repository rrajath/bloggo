package com.rrajath.bloggo.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DraftFlipDialog(
    onKeepDraft: () -> Unit,
    onFlipAndContinue: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onKeepDraft,
        title = { Text("Publish this post?") },
        text = {
            Text(
                "This post is marked as draft. Publishing will flip it to " +
                    "draft: false so it goes live on your blog.",
            )
        },
        confirmButton = {
            Button(onClick = onFlipAndContinue) {
                Text("Flip & continue")
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepDraft) {
                Text("Keep as draft")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PushConfirmSheet(
    data: PushConfirmData,
    isPushing: Boolean,
    error: String?,
    onCancel: () -> Unit,
    onPush: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = data.title,
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = data.targetPath,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Commit message",
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Text(
                    text = data.commitMessage,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (data.isNew) "New file — full contents" else "Changes vs. GitHub",
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    data.diffLines.forEach { line ->
                        DiffLineRow(line)
                    }
                }
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    enabled = !isPushing,
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = onPush,
                    modifier = Modifier.weight(1.3f),
                    enabled = !isPushing,
                ) {
                    if (isPushing) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp).width(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (data.isNew) "Push to GitHub" else "Push changes")
                }
            }
        }
    }
}

@Composable
private fun DiffLineRow(line: DiffLine) {
    val (bg, fg) = when (line.type) {
        DiffType.ADDED -> MaterialTheme.colorScheme.run {
            androidx.compose.ui.graphics.Color(0x223B6939) to androidx.compose.ui.graphics.Color(0xFF3B6939)
        }
        DiffType.REMOVED -> MaterialTheme.colorScheme.run {
            androidx.compose.ui.graphics.Color(0x22BA1A1A) to androidx.compose.ui.graphics.Color(0xFFBA1A1A)
        }
        DiffType.CONTEXT -> androidx.compose.ui.graphics.Color.Transparent to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = line.text,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = fg,
        )
    }
}

@Composable
fun DeleteConfirmDialog(
    postTitle: String,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Delete draft?") },
        text = { Text("\"$postTitle\" will be permanently deleted.") },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        },
    )
}
