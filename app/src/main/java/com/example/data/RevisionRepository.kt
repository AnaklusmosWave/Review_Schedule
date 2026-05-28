package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RevisionRepository(private val appDao: AppDao) {

    val allCategories: Flow<List<Category>> = appDao.getAllCategories()
        .onEach { list ->
            // If empty, auto-prepopulate in a non-blocking background context
            if (list.isEmpty()) {
                prepopulateCategories()
            }
        }

    val allRevisionItems: Flow<List<RevisionItem>> = appDao.getAllRevisionItems()

    val allSessionsWithItem: Flow<List<ReviewSessionWithItem>> = appDao.getAllSessionsWithItem()

    val allRawSessions: Flow<List<ReviewSession>> = appDao.getAllSessions()

    private suspend fun prepopulateCategories() {
        val defaults = listOf(
            Category(name = "數理科", colorHex = "#FFA726"),   // Orange
            Category(name = "英文文法", colorHex = "#29B6F6"),  // Blue
            Category(name = "國語文", colorHex = "#66BB6A"),    // Light Green
            Category(name = "社會科", colorHex = "#AB47BC"),    // Purple
            Category(name = "日常其他", colorHex = "#8D6E63")   // Brown
        )
        for (cat in defaults) {
            appDao.insertCategory(cat)
        }
    }

    suspend fun addCategory(name: String, colorHex: String): Long {
        return appDao.insertCategory(Category(name = name, colorHex = colorHex))
    }

    suspend fun deleteCategory(id: Long) {
        appDao.deleteCategory(id)
    }

    /**
     * Creates a revision item and automatically schedules review sessions 
     * based on the custom intervals (relative days from startDate).
     */
    suspend fun addRevisionItem(
        title: String,
        description: String,
        categoryId: Long,
        tags: String,
        startDateStr: String, // format "yyyy-MM-dd"
        intervals: List<Int>
    ): Long {
        // 1. Insert main item
        val item = RevisionItem(
            title = title,
            description = description,
            categoryId = categoryId,
            tags = tags,
            intervals = intervals.joinToString(",")
        )
        val itemId = appDao.insertRevisionItem(item)

        // 2. Generate review sessions
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val startDate = try {
            LocalDate.parse(startDateStr, formatter)
        } catch (e: Exception) {
            LocalDate.now()
        }

        val sessions = intervals.map { interval ->
            val scheduledDate = startDate.plusDays(interval.toLong()).format(formatter)
            ReviewSession(
                itemId = itemId,
                scheduledDate = scheduledDate,
                isCompleted = false
            )
        }
        
        if (sessions.isNotEmpty()) {
            appDao.insertSessions(sessions)
        }
        
        return itemId
    }

    suspend fun deleteRevisionItem(itemId: Long) {
        // Cascade delete reviews & main item
        appDao.deleteSessionsByItemId(itemId)
        appDao.deleteRevisionItem(itemId)
    }

    suspend fun updateRevisionItem(item: RevisionItem) {
        appDao.updateRevisionItem(item)
    }

    /**
     * Mark a review session as completed or active, recording evaluation rating
     */
    suspend fun updateSessionCompletion(
        sessionId: Long,
        itemId: Long,
        scheduledDate: String,
        isCompleted: Boolean,
        difficultyRating: String? = null,
        completedDateStr: String? = null
    ) {
        val session = ReviewSession(
            id = sessionId,
            itemId = itemId,
            scheduledDate = scheduledDate,
            isCompleted = isCompleted,
            completedDate = completedDateStr,
            difficultyRating = difficultyRating
        )
        appDao.updateSession(session)
    }

    suspend fun addCustomSession(itemId: Long, scheduledDateStr: String) {
        appDao.insertSession(
            ReviewSession(
                itemId = itemId,
                scheduledDate = scheduledDateStr,
                isCompleted = false
            )
        )
    }
}
