package com.rrajath.bloggo.domain

enum class HomeSection {
    DRAFT,
    PUBLISHED,
}

fun PostDraft.section(): HomeSection =
    if (draft) HomeSection.DRAFT else HomeSection.PUBLISHED
