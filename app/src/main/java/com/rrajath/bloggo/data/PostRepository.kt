package com.rrajath.bloggo.data

import com.rrajath.bloggo.data.db.AutosaveDao
import com.rrajath.bloggo.data.db.PostDao
import com.rrajath.bloggo.domain.PostDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepository @Inject constructor(
    private val postDao: PostDao,
    private val autosaveDao: AutosaveDao,
) {
    fun observeAllPosts(): Flow<List<PostDraft>> =
        postDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    fun observePost(localId: String): Flow<PostDraft?> =
        postDao.observeById(localId).map { it?.toDomain() }

    suspend fun getPost(localId: String): PostDraft? =
        postDao.getById(localId)?.toDomain()

    suspend fun savePost(post: PostDraft) =
        postDao.upsert(post.toEntity())

    suspend fun deletePost(localId: String) =
        postDao.deleteById(localId)

    suspend fun count(): Int = postDao.count()

    suspend fun saveAutosave(post: PostDraft) =
        autosaveDao.save(post.toAutosaveEntity())

    suspend fun getAutosave(localId: String): PostDraft? =
        autosaveDao.get(localId)?.toDomain(localId)

    suspend fun deleteAutosave(localId: String) =
        autosaveDao.delete(localId)
}
