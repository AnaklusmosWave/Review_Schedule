package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// A helper structure to easily display review sessions with their parent items details
data class ReviewSessionWithItem(
    val sessionId: Long,
    val itemId: Long,
    val scheduledDate: String,
    val isCompleted: Boolean,
    val completedDate: String?,
    val difficultyRating: String?,
    val itemTitle: String,
    val itemDescription: String,
    val itemTags: String,
    val categoryId: Long
)

@Dao
interface AppDao {
    // --- Category Operations ---
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: Long)

    // --- RevisionItem Operations ---
    @Query("SELECT * FROM revision_items ORDER BY createdAt DESC")
    fun getAllRevisionItems(): Flow<List<RevisionItem>>

    @Query("SELECT * FROM revision_items WHERE id = :id")
    suspend fun getRevisionItemById(id: Long): RevisionItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevisionItem(item: RevisionItem): Long

    @Update
    suspend fun updateRevisionItem(item: RevisionItem)

    @Query("DELETE FROM revision_items WHERE id = :id")
    suspend fun deleteRevisionItem(id: Long)

    // --- ReviewSession Operations ---
    @Query("SELECT * FROM review_sessions")
    fun getAllSessions(): Flow<List<ReviewSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<ReviewSession>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ReviewSession): Long

    @Update
    suspend fun updateSession(session: ReviewSession)

    @Query("DELETE FROM review_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("DELETE FROM review_sessions WHERE itemId = :itemId")
    suspend fun deleteSessionsByItemId(itemId: Long)

    // --- Joined Session Operations ---
    @Query("""
        SELECT 
            s.id as sessionId, 
            s.itemId as itemId, 
            s.scheduledDate as scheduledDate, 
            s.isCompleted as isCompleted, 
            s.completedDate as completedDate, 
            s.difficultyRating as difficultyRating,
            i.title as itemTitle, 
            i.description as itemDescription, 
            i.tags as itemTags, 
            i.categoryId as categoryId
        FROM review_sessions s
        INNER JOIN revision_items i ON s.itemId = i.id
        ORDER BY s.scheduledDate ASC
    """)
    fun getAllSessionsWithItem(): Flow<List<ReviewSessionWithItem>>
}
