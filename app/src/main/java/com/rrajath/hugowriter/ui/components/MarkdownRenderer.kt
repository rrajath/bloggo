package com.rrajath.hugowriter.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.ImageTransformer
import com.mikepenz.markdown.model.ImageData
import coil.compose.rememberAsyncImagePainter

@Composable
fun MarkdownRenderer(
    markdown: String,
    postId: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Check if markdown starts with frontmatter - support both --- (YAML) and +++ (TOML)
    val imageTransformer = object : ImageTransformer {
        @Composable
        override fun transform(link: String): ImageData {
            val transformedLink = if (postId != null && !link.startsWith("http") && !link.startsWith("https")) {
                val imageFile = File(File(File(context.filesDir, "images"), postId), link)
                if (imageFile.exists()) {
                    "file://${imageFile.absolutePath}"
                } else {
                    link
                }
            } else {
                link
            }
            return ImageData(rememberAsyncImagePainter(transformedLink))
        }
    }

    val yamlFrontmatterRegex = "^---\\n([\\s\\S]*?)\\n---\\n?".toRegex()
    val tomlFrontmatterRegex = "^\\+\\+\\+\\n([\\s\\S]*?)\\n\\+\\+\\+\\n?".toRegex()

    val yamlMatch = yamlFrontmatterRegex.find(markdown)
    val tomlMatch = tomlFrontmatterRegex.find(markdown)
    val frontmatterMatch = yamlMatch ?: tomlMatch

    if (frontmatterMatch != null) {
        // Has frontmatter - render separately
        val frontmatter = frontmatterMatch.value
        val content = markdown.substring(frontmatterMatch.range.last + 1)

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Frontmatter section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = frontmatter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content section using Markdown library
            Markdown(
                content = content,
                imageTransformer = imageTransformer,
                colors = markdownColor(
                    text = MaterialTheme.colorScheme.onSurface,
                    codeBackground = Color(0xFF2B2B2B)
                ),
                typography = markdownTypography(
                    h1 = MaterialTheme.typography.headlineLarge,
                    h2 = MaterialTheme.typography.headlineMedium,
                    h3 = MaterialTheme.typography.headlineSmall,
                    h4 = MaterialTheme.typography.titleLarge,
                    h5 = MaterialTheme.typography.titleMedium,
                    h6 = MaterialTheme.typography.titleSmall,
                    text = MaterialTheme.typography.bodyLarge,
                    code = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    quote = MaterialTheme.typography.bodyMedium,
                    paragraph = MaterialTheme.typography.bodyLarge,
                    ordered = MaterialTheme.typography.bodyLarge,
                    bullet = MaterialTheme.typography.bodyLarge,
                    list = MaterialTheme.typography.bodyLarge
                )
            )
        }
    } else {
        // No frontmatter - render normally
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Markdown(
                content = markdown,
                imageTransformer = imageTransformer,
                colors = markdownColor(
                    text = MaterialTheme.colorScheme.onSurface,
                    codeBackground = Color(0xFF2B2B2B)
                ),
                typography = markdownTypography(
                    h1 = MaterialTheme.typography.headlineLarge,
                    h2 = MaterialTheme.typography.headlineMedium,
                    h3 = MaterialTheme.typography.headlineSmall,
                    h4 = MaterialTheme.typography.titleLarge,
                    h5 = MaterialTheme.typography.titleMedium,
                    h6 = MaterialTheme.typography.titleSmall,
                    text = MaterialTheme.typography.bodyLarge,
                    code = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    quote = MaterialTheme.typography.bodyMedium,
                    paragraph = MaterialTheme.typography.bodyLarge,
                    ordered = MaterialTheme.typography.bodyLarge,
                    bullet = MaterialTheme.typography.bodyLarge,
                    list = MaterialTheme.typography.bodyLarge
                )
            )
        }
    }
}
