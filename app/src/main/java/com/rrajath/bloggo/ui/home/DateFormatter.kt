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

fun formatPostDate(rawFrontMatter: String, updatedAt: Long = 0L): String {
    val dateValue = extractDateValue(rawFrontMatter)
    if (dateValue != null) {
        return formatDateValue(dateValue, updatedAt)
    }
    return if (updatedAt > 0) {
        DATE_FORMATTER.format(Instant.ofEpochMilli(updatedAt))
    } else {
        ""
    }
}

fun parsePostDateInstant(rawFrontMatter: String, updatedAt: Long = 0L): Long {
    val dateValue = extractDateValue(rawFrontMatter)
    if (dateValue != null) {
        val parsed = parseDateToInstant(dateValue)
        if (parsed > 0) return parsed
    }
    return updatedAt
}

fun parsePostDateInstant(post: PostDraft): Long {
    val dateValue = post.postDate ?: extractDateValue(post.rawFrontMatter)
    if (dateValue != null) {
        val parsed = parseDateToInstant(dateValue)
        if (parsed > 0) return parsed
    }
    return post.updatedAt
}

private fun formatDateValue(dateValue: String, updatedAt: Long): String {
    return try {
        val instant = Instant.from(DATE_PARSER.parse(dateValue))
        DATE_FORMATTER.format(instant)
    } catch (e: DateTimeParseException) {
        try {
            val date = java.time.LocalDate.parse(dateValue)
            date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        } catch (e2: Exception) {
            if (updatedAt > 0) {
                DATE_FORMATTER.format(Instant.ofEpochMilli(updatedAt))
            } else {
                dateValue
            }
        }
    }
}

private fun parseDateToInstant(dateValue: String): Long {
    return try {
        Instant.from(DATE_PARSER.parse(dateValue)).toEpochMilli()
    } catch (e: Exception) {
        try {
            java.time.LocalDate.parse(dateValue)
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } catch (e2: Exception) {
            0L
        }
    }
}

private fun extractDateValue(rawFrontMatter: String): String? {
    val lines = rawFrontMatter.lines()
    val dateLine = lines.firstOrNull { it.trim().startsWith("date:") }
        ?: lines.firstOrNull { it.trim().startsWith("lastmod:") }
        ?: return null
    val key = if (dateLine.trim().startsWith("date:")) "date:" else "lastmod:"
    return dateLine.substringAfter(key).trim().trim('"')
}

fun buildMetaText(post: PostDraft): String {
    val dateValue = post.postDate ?: extractDateValue(post.rawFrontMatter)
    val date = if (dateValue != null) {
        formatDateValue(dateValue, post.updatedAt)
    } else if (post.updatedAt > 0) {
        DATE_FORMATTER.format(Instant.ofEpochMilli(post.updatedAt))
    } else {
        ""
    }
    val notPushed = if (post.syncState == SyncState.LOCAL_ONLY) "  ·  not pushed" else ""
    return "$date$notPushed"
}
