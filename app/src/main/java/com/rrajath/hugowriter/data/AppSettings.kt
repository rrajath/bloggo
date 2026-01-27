package com.rrajath.hugowriter.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppSettings(
    val isDarkMode: Boolean = false,
    val frontmatterTemplate: String = DEFAULT_FRONTMATTER_TEMPLATE
) {
    companion object {
        const val DEFAULT_FRONTMATTER_TEMPLATE = """---
title: "{TITLE}"
date: {DATE}
draft: true
tags: []
---

"""

        fun generateFrontmatter(title: String, template: String = DEFAULT_FRONTMATTER_TEMPLATE): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            val currentDate = dateFormat.format(Date())

            return template
                .replace("{TITLE}", title)
                .replace("{DATE}", currentDate)
        }
    }
}
