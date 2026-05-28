package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RevisionViewModel(private val repository: RevisionRepository) : ViewModel() {

    // Central state variables
    private val _selectedDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<Long?>(null)
    val selectedCategoryFilter: StateFlow<Long?> = _selectedCategoryFilter.asStateFlow()

    private val _selectedTagFilter = MutableStateFlow<String?>(null)
    val selectedTagFilter: StateFlow<String?> = _selectedTagFilter.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<String>("ALL") // ALL, PENDING, COMPLETED
    val selectedStatusFilter: StateFlow<String> = _selectedStatusFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow<String>("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Database flows
    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val revisionItems: StateFlow<List<RevisionItem>> = repository.allRevisionItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<ReviewSessionWithItem>> = repository.allSessionsWithItem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI State: Calendar monthly items count mapped by date (YYYY-MM-DD -> SessionsCount)
    val calendarTaskCountMap: StateFlow<Map<String, Int>> = allSessions
        .map { sessions ->
            sessions.groupBy { it.scheduledDate }
                .mapValues { (_, value) -> value.size }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // UI State: Calendar uncompleted item count mapped by date
    val calendarUncompletedCountMap: StateFlow<Map<String, Int>> = allSessions
        .map { sessions ->
            sessions.filter { !it.isCompleted }
                .groupBy { it.scheduledDate }
                .mapValues { (_, value) -> value.size }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // UI State: Filtered review sessions for the currently selected Day (or search parameters)
    val filteredSessionsForSelectedDate: StateFlow<List<ReviewSessionWithItem>> = combine(
        allSessions,
        _selectedDate,
        _selectedCategoryFilter,
        _selectedTagFilter,
        _selectedStatusFilter,
        _searchQuery
    ) { array ->
        val sessions = array[0] as List<ReviewSessionWithItem>
        val date = array[1] as LocalDate
        val categoryId = array[2] as Long?
        val tagStr = array[3] as String?
        val status = array[4] as String
        val search = array[5] as String

        val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        sessions.filter { session ->
            // Date filter (only matches day if search query is empty to make calendar responsive)
            val dateMatches = if (search.isEmpty()) session.scheduledDate == dateStr else true
            
            // Category filter
            val catMatches = categoryId == null || session.categoryId == categoryId
            
            // Tag filter
            val tagMatches = tagStr == null || session.itemTags.split(",").map { it.trim() }.contains(tagStr)
            
            // Status filter
            val statusMatches = when (status) {
                "PENDING" -> !session.isCompleted
                "COMPLETED" -> session.isCompleted
                else -> true
            }

            // Search query text matching
            val textMatches = search.isEmpty() || 
                session.itemTitle.contains(search, ignoreCase = true) ||
                session.itemDescription.contains(search, ignoreCase = true) ||
                session.itemTags.contains(search, ignoreCase = true)

            dateMatches && catMatches && tagMatches && statusMatches && textMatches
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI State: Distinct tags in use for filteringchips
    val allDistinctTags: StateFlow<List<String>> = allSessions
        .map { sessions ->
            sessions.flatMap { it.itemTags.split(",") }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ANALYTICS STATES
    // 1. Overall Completion rates
    val completionStats: StateFlow<Triple<Int, Int, Float>> = allSessions.map { sessions ->
        val total = sessions.size
        val completed = sessions.count { it.isCompleted }
        val rate = if (total > 0) (completed.toFloat() / total.toFloat()) else 0f
        Triple(completed, total, rate)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(0, 0, 0f))

    // 2. Classifications Distribution count
    val categoryDistribution: StateFlow<Map<Long, Int>> = allSessions.map { sessions ->
        sessions.groupBy { it.categoryId }.mapValues { (_, list) -> list.distinctBy { it.itemId }.size }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // 3. Weekly Memory Recovery Speed / Completion Trend (Past 7 days including today)
    val weeklyTrendData: StateFlow<List<ReviewTrendPoint>> = allSessions.map { sessions ->
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()
        val trendPoints = mutableListOf<ReviewTrendPoint>()

        for (i in 6 downTo 0) {
            val date = today.minusDays(i.toLong())
            val dateStr = date.format(formatter)
            val daySessions = sessions.filter { it.scheduledDate == dateStr }
            val total = daySessions.size
            val completed = daySessions.count { it.isCompleted }

            // Date label abbreviation (e.g. 05-27)
            val label = date.format(DateTimeFormatter.ofPattern("MM/dd"))

            trendPoints.add(ReviewTrendPoint(label = label, completed = completed, total = total))
        }
        trendPoints
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Trigger state configurations
    fun selectCalendarDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setCategoryFilter(categoryId: Long?) {
        _selectedCategoryFilter.value = categoryId
    }

    fun setTagFilter(tag: String?) {
        _selectedTagFilter.value = if (tag == "ALL" || tag?.isEmpty() == true) null else tag
    }

    fun setStatusFilter(status: String) {
        _selectedStatusFilter.value = status
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Database Actions
    fun addNewCategory(name: String, colorHex: String) {
        viewModelScope.launch {
            repository.addCategory(name, colorHex)
        }
    }

    fun removeCategory(id: Long) {
        viewModelScope.launch {
            repository.deleteCategory(id)
            if (_selectedCategoryFilter.value == id) {
                _selectedCategoryFilter.value = null
            }
        }
    }

    fun addNewRevisionItem(
        title: String,
        description: String,
        categoryId: Long,
        tags: String,
        startDate: LocalDate,
        intervals: List<Int>
    ) {
        viewModelScope.launch {
            val dateStr = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            repository.addRevisionItem(
                title = title,
                description = description,
                categoryId = categoryId,
                tags = tags,
                startDateStr = dateStr,
                intervals = intervals
            )
        }
    }

    fun deleteRevisionItem(itemId: Long) {
        viewModelScope.launch {
            repository.deleteRevisionItem(itemId)
        }
    }

    fun toggleSessionComplete(
        session: ReviewSessionWithItem,
        isCompleted: Boolean,
        difficultyRating: String? = null
    ) {
        viewModelScope.launch {
            val completedDateStr = if (isCompleted) {
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            } else {
                null
            }
            repository.updateSessionCompletion(
                sessionId = session.sessionId,
                itemId = session.itemId,
                scheduledDate = session.scheduledDate,
                isCompleted = isCompleted,
                difficultyRating = difficultyRating,
                completedDateStr = completedDateStr
            )
        }
    }

    fun rescheduleSession(session: ReviewSessionWithItem, newDate: LocalDate) {
        viewModelScope.launch {
            val newDateStr = newDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            // Cancel current session by deleting it, and schedule a new one
            repository.updateSessionCompletion(
                sessionId = session.sessionId,
                itemId = session.itemId,
                scheduledDate = newDateStr, // Update date
                isCompleted = session.isCompleted,
                difficultyRating = session.difficultyRating,
                completedDateStr = session.completedDate
            )
        }
    }

    fun addManualRevisionSession(itemId: Long, date: LocalDate) {
        viewModelScope.launch {
            val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            repository.addCustomSession(itemId, dateStr)
        }
    }
}

// Model representing a point in our revision trend chart
data class ReviewTrendPoint(
    val label: String,     // Day of week / date, e.g. "05/27"
    val completed: Int,    // Completed count
    val total: Int         // Total scheduled count
)

// Viewmodel factory helper
class RevisionViewModelFactory(private val repository: RevisionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RevisionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RevisionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
