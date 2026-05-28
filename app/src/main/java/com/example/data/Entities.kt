package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String
)

@Entity(tableName = "revision_items")
data class RevisionItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val categoryId: Long,
    val tags: String, // Comma-separated tags, e.g., "重點,公式,錯題"
    val createdAt: Long = System.currentTimeMillis(),
    val intervals: String, // Comma-separated relative review days, e.g., "1,2,4,7,15"
    val isArchived: Boolean = false
)

@Entity(tableName = "review_sessions")
data class ReviewSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val scheduledDate: String, // Format: YYYY-MM-DD
    val isCompleted: Boolean = false,
    val completedDate: String? = null, // Format: YYYY-MM-DD
    val difficultyRating: String? = null // "EASY", "GOOD", "HARD"
)
