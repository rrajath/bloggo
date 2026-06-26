package com.rrajath.bloggo.domain

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.representer.Representer

data class FrontMatterWarning(val message: String)

data class AssembleResult(
    val content: String,
    val warnings: List<FrontMatterWarning>,
)

data class ParseResult(
    val title: String?,
    val slug: String?,
    val draft: Boolean,
    val rawFrontMatter: String,
    val body: String,
    val warnings: List<FrontMatterWarning>,
    val date: String? = null,
    val lastmod: String? = null,
)

object FrontMatter {
    val OWNED_KEYS = setOf("title", "slug", "draft")

    private val yaml: Yaml by lazy {
        val loaderOptions = LoaderOptions()
        val dumperOptions = DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
            indent = 2
        }
        val constructor = SafeConstructor(loaderOptions)
        val representer = Representer(dumperOptions)
        Yaml(constructor, representer, dumperOptions, loaderOptions)
    }

    private enum class FrontMatterFormat { YAML, TOML }

    private data class SplitResult(
        val frontMatter: String?,
        val body: String,
        val format: FrontMatterFormat?,
    )

    fun assemble(draft: PostDraft): AssembleResult {
        val warnings = mutableListOf<FrontMatterWarning>()
        val output = LinkedHashMap<String, Any?>()

        output["title"] = draft.title.ifBlank { "Untitled" }
        output["slug"] = draft.slug.ifBlank { slugify(draft.title) }
        output["draft"] = draft.draft

        val rawMap = parseRawMap(draft.rawFrontMatter)
        for (key in OWNED_KEYS) {
            if (rawMap.containsKey(key)) {
                warnings.add(
                    FrontMatterWarning(
                        "Owned key '$key' was found in raw front matter and stripped; " +
                            "the structured field value is used instead.",
                    ),
                )
                rawMap.remove(key)
            }
        }
        output.putAll(rawMap)

        val yamlContent = yaml.dumpAsMap(output).trimEnd()
        val body = draft.body.trimStart('\n')
        val content = buildString {
            append("---\n")
            append(yamlContent)
            append("\n---\n")
            if (body.isNotEmpty()) {
                append("\n")
                append(body)
            }
        }

        return AssembleResult(content, warnings)
    }

    fun parse(fileContent: String): ParseResult {
        val warnings = mutableListOf<FrontMatterWarning>()

        val split = splitFrontMatter(fileContent)
        val frontMatterRaw = split.frontMatter
        val body = split.body

        if (frontMatterRaw == null) {
            return ParseResult(
                title = null,
                slug = null,
                draft = false,
                rawFrontMatter = "",
                body = fileContent,
                warnings = warnings,
            )
        }

        val map = when (split.format) {
            FrontMatterFormat.TOML -> parseTomlMap(frontMatterRaw)
            else -> parseRawMap(frontMatterRaw)
        }

        // Extract date/lastmod from the raw text BEFORE SnakeYAML processes the map,
        // because SnakeYAML auto-converts YAML timestamp literals to java.util.Date and
        // re-serializes them in a format that DateFormatter cannot parse.
        val date = extractRawValue(frontMatterRaw, "date")
        val lastmod = extractRawValue(frontMatterRaw, "lastmod")

        val title = map.remove("title")?.toString()
        val slug = map.remove("slug")?.toString()
        val draft = parseDraft(map.remove("draft"))

        for (key in OWNED_KEYS) {
            @Suppress("UNUSED_VALUE")
            map.remove(key)
        }

        // SnakeYAML auto-converts YAML timestamp literals (e.g. `date: 2026-06-21`) to
        // java.util.Date. Normalize them back to ISO-8601 strings before re-serializing,
        // so that rawFrontMatter stays in a format DateFormatter can parse.
        for ((key, value) in map.entries.toList()) {
            when (value) {
                is java.util.Date -> map[key] = java.time.Instant.ofEpochMilli(value.time).toString()
                is java.util.Calendar -> map[key] = java.time.Instant.ofEpochMilli(value.timeInMillis).toString()
            }
        }

        val rawFrontMatter = if (map.isEmpty()) {
            ""
        } else {
            yaml.dumpAsMap(map).trimEnd()
        }

        return ParseResult(
            title = title,
            slug = slug,
            draft = draft,
            rawFrontMatter = rawFrontMatter,
            body = body,
            warnings = warnings,
            date = date,
            lastmod = lastmod,
        )
    }

    fun validate(rawFrontMatter: String): List<FrontMatterWarning> {
        val warnings = mutableListOf<FrontMatterWarning>()
        try {
            val map = parseRawMap(rawFrontMatter)
            for (key in OWNED_KEYS) {
                if (map.containsKey(key)) {
                    warnings.add(
                        FrontMatterWarning(
                            "Owned key '$key' is managed by the app and will be " +
                                "overridden by the structured field on save.",
                        ),
                    )
                }
            }
        } catch (e: Exception) {
            warnings.add(FrontMatterWarning("Malformed YAML: ${e.message}"))
        }
        return warnings
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseRawMap(yamlString: String): MutableMap<String, Any?> {
        if (yamlString.isBlank()) return mutableMapOf()
        val loaded = yaml.load<Any>(yamlString)
        return when (loaded) {
            is Map<*, *> -> loaded.entries.associate { it.key.toString() to it.value } as MutableMap<String, Any?>
            null -> mutableMapOf()
            else -> mutableMapOf()
        }
    }

    private fun parseTomlMap(tomlString: String): MutableMap<String, Any?> {
        if (tomlString.isBlank()) return mutableMapOf()
        val map = mutableMapOf<String, Any?>()
        for (line in tomlString.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            val eqIndex = trimmed.indexOf('=')
            if (eqIndex < 0) continue
            val key = trimmed.substring(0, eqIndex).trim().trim('"', '\'')
            val rawValue = stripTomlInlineComment(trimmed.substring(eqIndex + 1).trim())
            map[key] = parseTomlValue(rawValue)
        }
        return map
    }

    private fun stripTomlInlineComment(value: String): String {
        var inString = false
        var quoteChar = ' '
        for (i in value.indices) {
            val c = value[i]
            if (inString) {
                if (c == quoteChar) inString = false
            } else {
                if (c == '"' || c == '\'') {
                    inString = true
                    quoteChar = c
                } else if (c == '#') {
                    return value.substring(0, i).trim()
                }
            }
        }
        return value.trim()
    }

    private fun parseTomlValue(value: String): Any? {
        if (value.isEmpty()) return null
        return when (value[0]) {
            '"' -> value.trimStart('"').trimEnd('"')
            '\'' -> value.trimStart('\'').trimEnd('\'')
            '[' -> {
                val inner = value.trimStart('[').trimEnd(']').trim()
                if (inner.isEmpty()) {
                    emptyList<String>()
                } else {
                    inner.split(",").map { it.trim().trim('"', '\'') }
                }
            }
            else -> when (value) {
                "true" -> true
                "false" -> false
                else -> value
            }
        }
    }

    private fun extractRawValue(text: String, key: String): String? {
        val line = text.lines().firstOrNull { line ->
            val t = line.trim()
            t.startsWith("$key:") || t.startsWith("$key =") || t.startsWith("$key=")
        } ?: return null
        val value = if ('=' in line && ':' !in line.substringBefore('=')) {
            line.substringAfter('=')
        } else {
            line.substringAfter(':')
        }
        return value.trim().trim('"', '\'').ifBlank { null }
    }

    private fun parseDraft(value: Any?): Boolean {
        return when (value) {
            is Boolean -> value
            is String -> value.toBooleanStrictOrNull() ?: (value.lowercase() != "false")
            null -> false
            else -> false
        }
    }

    private fun splitFrontMatter(content: String): SplitResult {
        val trimmed = content.trimStart()

        if (trimmed.startsWith("+++")) {
            val lines = trimmed.lines()
            if (lines.isEmpty()) return SplitResult(null, content, null)
            val endFenceIndex = (1 until lines.size).firstOrNull { lines[it].trim() == "+++" }
                ?: return SplitResult(null, content, null)
            val frontMatterToml = lines.subList(1, endFenceIndex).joinToString("\n")
            val body = lines.subList(endFenceIndex + 1, lines.size).joinToString("\n").trimStart('\n')
            return SplitResult(frontMatterToml, body, FrontMatterFormat.TOML)
        }

        if (trimmed.startsWith("---")) {
            val lines = trimmed.lines()
            if (lines.isEmpty()) return SplitResult(null, content, null)
            val endFenceIndex = (1 until lines.size).firstOrNull { lines[it].trim() == "---" }
                ?: return SplitResult(null, content, null)
            val frontMatterYaml = lines.subList(1, endFenceIndex).joinToString("\n")
            val body = lines.subList(endFenceIndex + 1, lines.size).joinToString("\n").trimStart('\n')
            return SplitResult(frontMatterYaml, body, FrontMatterFormat.YAML)
        }

        return SplitResult(null, content, null)
    }
}
