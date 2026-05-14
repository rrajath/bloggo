package com.rrajath.hugowriter.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration

class MarkdownVisualTransformation(
    private val primaryColor: Color,
    private val secondaryColor: Color
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            highlightMarkdown(text.text),
            OffsetMapping.Identity
        )
    }

    private fun highlightMarkdown(text: String): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            
            // Bold **text**
            val boldRegex = "\\*\\*.*?\\*\\*".toRegex()
            boldRegex.findAll(text).forEach { match ->
                addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = primaryColor), match.range.first, match.range.last + 1)
            }
            
            // Italic *text*
            val italicRegex = "(?<!\\*)\\*[^\\*]+\\*(?!\\*)".toRegex()
            italicRegex.findAll(text).forEach { match ->
                addStyle(SpanStyle(fontStyle = FontStyle.Italic, color = primaryColor), match.range.first, match.range.last + 1)
            }
            
            // Link [text](url)
            val linkRegex = "\\[.*?\\]\\(.*?\\)".toRegex()
            linkRegex.findAll(text).forEach { match ->
                addStyle(SpanStyle(color = secondaryColor, textDecoration = TextDecoration.Underline), match.range.first, match.range.last + 1)
            }
            
            // Code `code`
            val codeRegex = "`.*?`".toRegex()
            codeRegex.findAll(text).forEach { match ->
                addStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = primaryColor.copy(alpha = 0.1f)), match.range.first, match.range.last + 1)
            }
            
            // Quote > text
            val quoteRegex = "(?m)^>.*$".toRegex()
            quoteRegex.findAll(text).forEach { match ->
                addStyle(SpanStyle(color = Color.Gray, fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
            }
            
            // Header # Header
            val headerRegex = "(?m)^#+.*$".toRegex()
            headerRegex.findAll(text).forEach { match ->
                addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = primaryColor), match.range.first, match.range.last + 1)
            }

            // Frontmatter --- or +++
            val frontmatterRegex = "(?s)^---.*?---|^\\+\\+\\+.*?\\+\\+\\+".toRegex()
            frontmatterRegex.find(text)?.let { match ->
                addStyle(SpanStyle(color = Color.Gray.copy(alpha = 0.7f), fontFamily = FontFamily.Monospace), match.range.first, match.range.last + 1)
            }
        }
    }
}
