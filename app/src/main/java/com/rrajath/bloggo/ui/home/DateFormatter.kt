package com.rrajath.bloggo.ui.home

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import com.rrajath.bloggo.domain.PostDraft
import com.rrajath.bloggo.domain.SyncState

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy")
    .withZone(ZoneOffset.UTC)

private val DATE_PARSER = DateTimeFormatter.ISO_DATE_TIME
    .withZone(ZoneOffset.UTC)

fun formatPostDate(rawFrontMatter: String): String {
    val dateLine = rawFrontMatter.lines().firstOrNull { it.trim().startsWith("date:") }
        ?: return ""
    val dateValue = dateLine.substringAfter("date:").trim().trim('"')
    return try {
        val instant = Instant.from(DATE_PARSER.parse(dateValue))
        DATE_FORMATTER.format(instant)
    } catch (e: DateTimeParseException) {
        try {
            val date = java.time.LocalDate.parse(dateValue)
            date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        } catch (e2: Exception) {
            dateValue
        }
    }
}

fun buildMetaText(post: PostDraft): String {
    val date = formatPostDate(post.rawFrontMatter)
    val notPushed = if (post.syncState == com.rrajath.bloggo.domain.SyncState.LOCAL_ONLY) "  ·  not pushed" else ""
    return "$date$notPushed"
}
