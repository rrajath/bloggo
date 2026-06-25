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

        val (frontMatterYaml, body) = splitFrontMatter(fileContent)

        if (frontMatterYaml == null) {
            return ParseResult(
                title = null,
                slug = null,
                draft = true,
                rawFrontMatter = "",
                body = fileContent,
                warnings = warnings,
            )
        }

        val map = parseRawMap(frontMatterYaml)

        val title = map.remove("title")?.toString()
        val slug = map.remove("slug")?.toString()
        val draft = parseDraft(map.remove("draft"))

        for (key in OWNED_KEYS) {
            @Suppress("UNUSED_VALUE")
            map.remove(key)
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

    private fun parseDraft(value: Any?): Boolean {
        return when (value) {
            is Boolean -> value
            is String -> value.toBooleanStrictOrNull() ?: (value.lowercase() != "false")
            null -> true
            else -> true
        }
    }

    private fun splitFrontMatter(content: String): Pair<String?, String> {
        val trimmed = content.trimStart()
        if (!trimmed.startsWith("---")) {
            return null to content
        }

        val lines = trimmed.lines()
        if (lines.isEmpty()) return null to content

        val endFenceIndex = (1 until lines.size).firstOrNull { lines[it].trim() == "---" }
            ?: return null to content

        val frontMatterYaml = lines.subList(1, endFenceIndex).joinToString("\n")
        val body = lines.subList(endFenceIndex + 1, lines.size).joinToString("\n").trimStart('\n')

        return frontMatterYaml to body
    }
}
