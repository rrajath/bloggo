package com.rrajath.bloggo.data

interface SecureStorage {
    fun getPat(): String
    fun setPat(value: String)
}
