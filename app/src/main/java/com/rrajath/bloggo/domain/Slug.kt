package com.rrajath.bloggo.domain

private val APOSTROPHES_QUOTES = Regex("[\u2019'\"]")
private val NON_ALNUM_RUN = Regex("[^a-z0-9]+")
private val DASH_RUN = Regex("-+")

fun slugify(title: String): String =
    title.trim().lowercase()
        .replace(APOSTROPHES_QUOTES, "")
        .replace(NON_ALNUM_RUN, "-")
        .replace(DASH_RUN, "-")
        .trim('-')

fun PostDraft.onTitleCommitted(newTitle: String): PostDraft {
    val derive = slugAutoDerive && syncState == SyncState.LOCAL_ONLY
    return copy(
        title = newTitle,
        slug = if (derive) slugify(newTitle) else slug,
        updatedAt = System.currentTimeMillis(),
    )
}

fun PostDraft.onSlugManuallyEdited(newSlug: String): PostDraft =
    copy(
        slug = newSlug,
        slugAutoDerive = false,
        updatedAt = System.currentTimeMillis(),
    )
