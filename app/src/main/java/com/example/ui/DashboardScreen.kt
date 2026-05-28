package com.example.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Category
import com.example.data.ReviewSessionWithItem
import com.example.data.RevisionItem
import com.example.receiver.AlarmReceiver
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: RevisionViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Navigation index state: 0 = 月曆, 1 = 複習項目, 2 = 數據圖表, 3 = 管理設定
    var currentTab by remember { mutableStateOf(0) }
    
    // Bottom Sheet / Dialog Dialog states
    var showAddItemDialog by remember { mutableStateOf(false) }
    var ratingDialogSession by remember { mutableStateOf<ReviewSessionWithItem?>(null) }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Spa, 
                            contentDescription = "綠意圖示",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = when(currentTab) {
                                0 -> "日曆複習"
                                1 -> "項目管理"
                                2 -> "記憶曲線數據"
                                else -> "系統設定"
                            },
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "月曆") },
                    label = { Text("月曆檢視") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_calendar")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = "項目") },
                    label = { Text("複習項目") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_tasks")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.QueryStats, contentDescription = "統計") },
                    label = { Text("數據圖表") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_charts")
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "設定") },
                    label = { Text("分類設定") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        },
        floatingActionButton = {
            if (currentTab == 0 || currentTab == 1) {
                ExtendedFloatingActionButton(
                    text = { Text("添加複習", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Add, contentDescription = "新增項目") },
                    onClick = { showAddItemDialog = true },
                    shape = RoundedCornerShape(24.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add_item")
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        val isDark = isSystemInDarkTheme()
        val appBgBrush = remember(isDark) {
            if (isDark) {
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF132A1C),
                        Color(0xFF070D09)
                    ),
                    center = Offset(300f, 300f),
                    radius = 1200f
                )
            } else {
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFE8F5E9),
                        Color(0xFFF7FBF7)
                    ),
                    center = Offset(200f, 200f),
                    radius = 1000f
                )
            }
        }
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = appBgBrush)
                .padding(innerPadding),
            color = Color.Transparent
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "page_transition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> CalendarViewTab(
                        viewModel = viewModel,
                        onCompleteSession = { session -> ratingDialogSession = session }
                    )
                    1 -> TasksListTab(
                        viewModel = viewModel,
                        onCompleteSession = { session -> ratingDialogSession = session }
                    )
                    2 -> AnalyticsTab(
                        viewModel = viewModel
                    )
                    3 -> SettingsTab(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
    
    // Dialog to generate a new revision item
    if (showAddItemDialog) {
        AddItemDialog(
            viewModel = viewModel,
            onDismiss = { showAddItemDialog = false }
        )
    }
    
    // Evaluation rating dialog
    ratingDialogSession?.let { session ->
        RatingEvaluationDialog(
            session = session,
            onDismiss = { ratingDialogSession = null },
            onRatingSelected = { rating ->
                viewModel.toggleSessionComplete(session, isCompleted = true, difficultyRating = rating)
                ratingDialogSession = null
                
                // Fire instant custom push notification to notify success! (Push Simulation)
                AlarmReceiver.triggerInstantNotification(context, reviewCount = 0)
            }
        )
    }
}

// ==========================================
// COLUMN 1: Calendar View Tab (月曆檢視)
// ==========================================
@Composable
fun CalendarViewTab(
    viewModel: RevisionViewModel,
    onCompleteSession: (ReviewSessionWithItem) -> Unit
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val calendarTaskCountMap by viewModel.calendarTaskCountMap.collectAsStateWithLifecycle()
    val calendarUncompletedCountMap by viewModel.calendarUncompletedCountMap.collectAsStateWithLifecycle()
    val filteredSessions by viewModel.filteredSessionsForSelectedDate.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "記憶曲線自訂排程日曆",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        
        // Month Grid Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Month Row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentYearMonth = currentYearMonth.minusMonths(1) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "上個月")
                        }
                        
                        Text(
                            text = "${currentYearMonth.year}年 ${currentYearMonth.monthValue}月",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        IconButton(onClick = { currentYearMonth = currentYearMonth.plusMonths(1) }) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "下個月")
                        }
                    }
                    
                    // Weekday Titles Row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")
                        weekdays.forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                    
                    // Month Days Grid Math
                    val weeks = remember(currentYearMonth) { getWeeksInMonth(currentYearMonth) }
                    
                    weeks.forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            week.forEach { date ->
                                if (date != null) {
                                    val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                    val totalSessions = calendarTaskCountMap[dateStr] ?: 0
                                    val pendingSessions = calendarUncompletedCountMap[dateStr] ?: 0
                                    
                                    val isSelected = date == selectedDate
                                    val isToday = date == LocalDate.now()
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .border(
                                                width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                                                color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { viewModel.selectCalendarDate(date) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = date.dayOfMonth.toString(),
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                            
                                            // Badges count dots
                                            if (totalSessions > 0) {
                                                Row(
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(top = 1.dp)
                                                ) {
                                                    if (pendingSessions > 0) {
                                                        // Soft red/orange dot for pending active work
                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                                                    else MaterialTheme.colorScheme.secondary
                                                                )
                                                        )
                                                    } else {
                                                        // Green check dot for fully revision completed!
                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .clip(CircleShape)
                                                                .background(Color(0xFF4CAF50))
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Section header for selected date reviews
        item {
            val formattedSelected = selectedDate.format(DateTimeFormatter.ofPattern("M月d日 (EEEE)", Locale.CHINESE))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$formattedSelected 複習項目",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        text = "${filteredSessions.size} 項",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        
        // List items
        if (filteredSessions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle, 
                            contentDescription = "無複習任務",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "這天沒有排定的複習項目！",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "給大腦放個假，或是添加新學習資訊吧 🌱",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(filteredSessions) { session ->
                val matchingCategory = categories.find { it.id == session.categoryId }
                ReviewSessionRow(
                    session = session,
                    category = matchingCategory,
                    onToggleComplete = { isCompleted ->
                        if (isCompleted) {
                            onCompleteSession(session)
                        } else {
                            viewModel.toggleSessionComplete(session, isCompleted = false)
                        }
                    },
                    modifier = Modifier.animateItem()
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// Helpers for calendar calculation
private fun getWeeksInMonth(yearMonth: YearMonth): List<List<LocalDate?>> {
    val firstOfMonth = yearMonth.atDay(1)
    val lastOfMonth = yearMonth.atEndOfMonth()
    
    // Day of week: Monday is 1, Sunday is 7.
    val startDayOfWeek = firstOfMonth.dayOfWeek.value
    
    val weeks = mutableListOf<List<LocalDate?>>()
    var currentWeek = mutableListOf<LocalDate?>()
    
    // Fill in placeholders prior to the 1st of the month
    for (i in 1 until startDayOfWeek) {
        currentWeek.add(null)
    }
    
    var currentDate = firstOfMonth
    while (!currentDate.isAfter(lastOfMonth)) {
        currentWeek.add(currentDate)
        if (currentWeek.size == 7) {
            weeks.add(currentWeek)
            currentWeek = mutableListOf()
        }
        currentDate = currentDate.plusDays(1)
    }
    
    // Pad the last week
    if (currentWeek.isNotEmpty()) {
        while (currentWeek.size < 7) {
            currentWeek.add(null)
        }
        weeks.add(currentWeek)
    }
    return weeks
}

// ==========================================
// COLUMN 2: Tasks List / Filters Tab (複習項目)
// ==========================================
@Composable
fun TasksListTab(
    viewModel: RevisionViewModel,
    onCompleteSession: (ReviewSessionWithItem) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedStatusFilter by viewModel.selectedStatusFilter.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val selectedTagFilter by viewModel.selectedTagFilter.collectAsStateWithLifecycle()
    
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val allDistinctTags by viewModel.allDistinctTags.collectAsStateWithLifecycle()
    val filteredSessionsWithItem by viewModel.filteredSessionsForSelectedDate.collectAsStateWithLifecycle()
    val allRevisionItems by viewModel.revisionItems.collectAsStateWithLifecycle()
    
    // Let's compute a list of revision items satisfying current category/tag filter
    val filteredItems = remember(allRevisionItems, selectedCategoryFilter, selectedTagFilter, searchQuery) {
        allRevisionItems.filter { item ->
            val matchesCategory = selectedCategoryFilter == null || item.categoryId == selectedCategoryFilter
            val tagsList = item.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val matchesTag = selectedTagFilter == null || tagsList.contains(selectedTagFilter)
            val matchesSearch = searchQuery.isEmpty() || 
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true) ||
                item.tags.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesTag && matchesSearch
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("search_text_input"),
            placeholder = { Text("搜尋項目、複習內容與狀態標籤") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜尋") },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "清除搜尋")
                    }
                }
            } else null,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        
        // Category Filter row
        Text(
            text = "依科目或分類篩選：",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, start = 4.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategoryFilter == null,
                    onClick = { viewModel.setCategoryFilter(null) },
                    label = { Text("全部科目") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
            items(categories) { cat ->
                val catColor = try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.secondary }
                FilterChip(
                    selected = selectedCategoryFilter == cat.id,
                    onClick = { viewModel.setCategoryFilter(cat.id) },
                    label = { Text(cat.name) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(catColor)
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
        
        // Tag filter chips row
        if (allDistinctTags.isNotEmpty()) {
            Text(
                text = "依標籤進行篩選：",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp, start = 4.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedTagFilter == null,
                        onClick = { viewModel.setTagFilter(null) },
                        label = { Text("全標籤") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                items(allDistinctTags) { tag ->
                    FilterChip(
                        selected = selectedTagFilter == tag,
                        onClick = { viewModel.setTagFilter(tag) },
                        label = { Text("#$tag") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.surfaceVariant)
        
        // Items list
        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilterListOff, 
                        contentDescription = "無相符項目",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "找不到相符的複習項目！",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "調整上面的分類或搜尋看看 🌱",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp)
            ) {
                items(filteredItems) { item ->
                    val matchingCategory = categories.find { it.id == item.categoryId }
                    RevisionItemCard(
                        item = item,
                        category = matchingCategory,
                        onDelete = { viewModel.deleteRevisionItem(item.id) },
                        onPostpone = { viewModel.addManualRevisionSession(item.id, LocalDate.now().plusDays(3)) }
                    )
                }
            }
        }
    }
}

// Item card display (detailed view mapping)
@Composable
fun RevisionItemCard(
    item: RevisionItem,
    category: Category?,
    onDelete: () -> Unit,
    onPostpone: () -> Unit
) {
    val catColor = remember(category) {
        try { Color(android.graphics.Color.parseColor(category?.colorHex ?: "#8D6E63")) } catch (e: Exception) { Color.Gray }
    }
    
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Category tag + header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { /* info chip only */ },
                    label = { Text(category?.name ?: "未分類", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = catColor.copy(alpha = 0.15f),
                        labelColor = catColor
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "選項")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Row {
                                Icon(Icons.Default.AddAlarm, contentDescription = "速推", modifier = Modifier.padding(end = 6.dp))
                                Text("3天後增加複習(速推)")
                            }},
                            onClick = { 
                                onPostpone()
                                showMenu = false 
                            }
                        )
                        DropdownMenuItem(
                            text = { Row {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "刪除", tint = Color.Red, modifier = Modifier.padding(end = 6.dp))
                                Text("刪除此項目", color = Color.Red)
                            }},
                            onClick = { 
                                onDelete()
                                showMenu = false 
                            }
                        )
                    }
                }
            }
            
            Text(
                text = item.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Text(
                text = item.description.ifEmpty { "尚無詳細複習說明。" },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            // Sub information: intervals & tags
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scheduled Intervals
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timeline, 
                        contentDescription = "間隔",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = " 規律：每第 " + item.intervals.replace(",", ", ") + " 天複習",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Tags
                Row(horizontalArrangement = Arrangement.End) {
                    item.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(3).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// Individual review item record row (mapped for specific date)
@Composable
fun ReviewSessionRow(
    session: ReviewSessionWithItem,
    category: Category?,
    onToggleComplete: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val catColor = remember(category) {
        try { Color(android.graphics.Color.parseColor(category?.colorHex ?: "#8D6E63")) } catch (e: Exception) { Color.Gray }
    }
    
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (session.isCompleted) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (session.isCompleted) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            IconButton(
                onClick = { onToggleComplete(!session.isCompleted) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (session.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = "標記複習狀態",
                    tint = if (session.isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Text values
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(catColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = category?.name ?: "其他科目",
                        fontSize = 11.sp,
                        color = catColor,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Difficulty assessment tags if completed
                    if (session.isCompleted && session.difficultyRating != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when (session.difficultyRating) {
                                        "EASY" -> Color(0xFFE8F5E9)
                                        "GOOD" -> Color(0xFFE3F2FD)
                                        else -> Color(0xFFFFEBEE)
                                    }
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = when (session.difficultyRating) {
                                    "EASY" -> "很簡單 🌱"
                                    "GOOD" -> "適中 🌿"
                                    else -> "困難 🪵"
                                },
                                fontSize = 9.sp,
                                color = when (session.difficultyRating) {
                                    "EASY" -> Color(0xFF2E7D32)
                                    "GOOD" -> Color(0xFF1565C0)
                                    else -> Color(0xFFC62828)
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Text(
                    text = session.itemTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (session.isCompleted) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(top = 2.dp)
                )
                
                if (session.itemDescription.isNotEmpty()) {
                    Text(
                        text = session.itemDescription,
                        fontSize = 12.sp,
                        color = if (session.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Tags
                if (session.itemTags.isNotEmpty()) {
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        session.itemTags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(2).forEach { tag ->
                            Text(
                                text = "#$tag ",
                                fontSize = 10.sp,
                                color = if (session.isCompleted) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// COLUMN 3: Analytics Tab (數據化複習進度圖表)
// ==========================================
@Composable
fun AnalyticsTab(
    viewModel: RevisionViewModel
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val completionStats by viewModel.completionStats.collectAsStateWithLifecycle()
    val categoryDistribution by viewModel.categoryDistribution.collectAsStateWithLifecycle()
    val weeklyTrendData by viewModel.weeklyTrendData.collectAsStateWithLifecycle()
    
    val totalSessions = completionStats.second
    val completedSessions = completionStats.first
    val completionRate = completionStats.third
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "複習大腦統計中心",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        
        // 1. Completion rate display Dial Card (Custom Frosted Glass Gradient card)
        item {
            val isDark = isSystemInDarkTheme()
            val glassBrush = remember(isDark) {
                if (isDark) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1B4332),
                            Color(0xFF2D6A4F)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFD1E8D5),
                            Color(0xFFB7E4C7)
                        )
                    )
                }
            }
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = glassBrush, shape = RoundedCornerShape(32.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "整體複釋完成率",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Circular Donut/Arc Diagram
                    val strokeColor = MaterialTheme.colorScheme.primary
                    val trackColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color.White.copy(alpha = 0.4f)
                    Box(
                        modifier = Modifier.size(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(130.dp)) {
                            // Track circular
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 15.dp.toPx(), cap = StrokeCap.Round)
                            )
                            // Live level sweep
                            drawArc(
                                color = strokeColor,
                                startAngle = -90f,
                                sweepAngle = completionRate * 360f,
                                useCenter = false,
                                style = Stroke(width = 15.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(completionRate * 100).toInt()}%",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "遺忘對抗率",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("已排定複習總數", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$totalSessions 次", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("已打卡完成", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$completedSessions 次", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("待複習剩餘", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${totalSessions - completedSessions} 次", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        
        // 2. Bar Chart: Category items distribution
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "各科複習項目分布 (個/種類)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (categories.isEmpty() || categoryDistribution.isEmpty()) {
                        Text(
                            text = "暫無學科項目分布資料，添加項目後自動統計 🌱",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        // Custom distribution bar draw list
                        categories.forEach { cat ->
                            val count = categoryDistribution[cat.id] ?: 0
                            val maxCount = categoryDistribution.values.maxOrNull() ?: 1
                            val fraction = count.toFloat() / maxCount.toFloat()
                            val parsedHex = try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cat.name,
                                    fontSize = 12.sp,
                                    modifier = Modifier.width(64.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // Draw horizontal progress bar with corresponding category colors
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction = if (fraction > 0f) fraction else 0.01f)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(parsedHex)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$count 個",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(40.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // 3. Stacked bar chart: Past 7 days Trend
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "近 7 日記憶抗忘打卡趨勢",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "（深綠：已打卡，淺灰綠：未打卡）",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Simple custom canvas stack columns charts
                    val gridColor = MaterialTheme.colorScheme.surfaceVariant
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val nonCompletedColor = MaterialTheme.colorScheme.surfaceVariant
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val barWidth = 24.dp.toPx()
                            val spacing = (size.width - (barWidth * 7)) / 8
                            val internalHeight = size.height - 30.dp.toPx()
                            
                            // 1. Draw horizontal grid baselines
                            for (row in 0..4) {
                                val y = row * (internalHeight / 4)
                                drawLine(
                                    color = gridColor,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1f
                                )
                            }
                            
                            // 2. Draw bars
                            weeklyTrendData.forEachIndexed { index, point ->
                                val x = spacing + index * (barWidth + spacing)
                                
                                if (point.total > 0) {
                                    val totalHeightFraction = 1f // Standardize height to 100% capacity
                                    val completedFraction = point.completed.toFloat() / point.total.toFloat()
                                    
                                    val barMaxY = internalHeight
                                    
                                    // Base gray track height represents total items scaling
                                    val trackHeight = internalHeight * 0.9f 
                                    
                                    // Gray bar (uncompleted)
                                    drawRoundRect(
                                        color = nonCompletedColor,
                                        topLeft = Offset(x, barMaxY - trackHeight),
                                        size = Size(barWidth, trackHeight),
                                        cornerRadius = CornerRadius(12f, 12f)
                                    )
                                    
                                    // Green bar bottom overlaps (representing completion ratio)
                                    val completedHeight = trackHeight * completedFraction
                                    if (completedHeight > 0) {
                                        drawRoundRect(
                                            color = primaryColor,
                                            topLeft = Offset(x, barMaxY - completedHeight),
                                            size = Size(barWidth, completedHeight),
                                            cornerRadius = CornerRadius(12f, 12f)
                                        )
                                    }
                                } else {
                                    // No tasks on that day, optional small circle indicator
                                    drawCircle(
                                        color = gridColor,
                                        radius = 4f,
                                        center = Offset(x + barWidth / 2, internalHeight - 10f)
                                    )
                                }
                            }
                        }
                        
                        // Overlaid texts for date labels on canvas
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 155.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            weeklyTrendData.forEach { point ->
                                Text(
                                    text = point.label,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// COLUMN 4: Settings / Config Tab (設定頁面)
// ==========================================
@Composable
fun SettingsTab(
    viewModel: RevisionViewModel
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    
    var showCategoryCreateDialog by remember { mutableStateOf(false) }
    var alertHour by remember { mutableStateOf(9) }
    var showTimeSelectorDialog by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "複習類別與提醒管理",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        
        // 1. Notification trigger simulation layout
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "推播提醒功能",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "每日指定時間提供排程打卡提醒，維持記憶曲線。",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("每日定時提醒", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "設定於每天上午 ${String.format("%02d:00", alertHour)} 派發",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Button(
                            onClick = { showTimeSelectorDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text("修改時間", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Instant Test physical notification trigger
                    Button(
                        onClick = {
                            // Find uncompleted count for today
                            val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            val countToday = allSessions.count { it.scheduledDate == todayStr && !it.isCompleted }
                            
                            // Call AlarmReceiver simulation trigger standard notification
                            AlarmReceiver.triggerInstantNotification(context, reviewCount = countToday)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("btn_test_notification"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = "測試", modifier = Modifier.padding(end = 6.dp))
                        Text("測試推播提醒 (Live模擬效果)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        // 2. Categories manager Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "科目與分類管理",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = { showCategoryCreateDialog = true },
                            modifier = Modifier.testTag("btn_add_category")
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = "新增分類", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    categories.forEach { cat ->
                        val catColor = try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (e: Exception) { Color.Gray }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(catColor)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(cat.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            
                            // Do not delete last surviving category 
                            if (categories.size > 1) {
                                IconButton(
                                    onClick = { viewModel.removeCategory(cat.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete, 
                                        contentDescription = "刪除", 
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Category create Dialog
    if (showCategoryCreateDialog) {
        AddCategoryDialog(
            onDismiss = { showCategoryCreateDialog = false },
            onCreate = { name, color ->
                viewModel.addNewCategory(name, color)
                showCategoryCreateDialog = false
            }
        )
    }
    
    // Time selector dialog
    if (showTimeSelectorDialog) {
        TimeSelectionDialog(
            currentHour = alertHour,
            onDismiss = { showTimeSelectorDialog = false },
            onSelected = { hr ->
                alertHour = hr
                showTimeSelectorDialog = false
            }
        )
    }
}

// ==========================================
// SUB-DIALOGS AND POPUPS
// ==========================================

// Evaluation card selector after tagging items completed
@Composable
fun RatingEvaluationDialog(
    session: ReviewSessionWithItem,
    onDismiss: () -> Unit,
    onRatingSelected: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.72f)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.SelfImprovement, 
                    contentDescription = "自我評估",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "複習成果打卡",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "這次複習 「${session.itemTitle}」 感覺難度如何？點擊評估會調整您的記憶權重唷 🌱",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onRatingSelected("EASY") },
                        modifier = Modifier.fillMaxWidth().testTag("rating_easy"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F5E9), contentColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("很簡單 🌱 (感覺很熟練，下次可延長間隔)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    
                    Button(
                        onClick = { onRatingSelected("GOOD") },
                        modifier = Modifier.fillMaxWidth().testTag("rating_good"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3F2FD), contentColor = Color(0xFF1565C0)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("適中 🌿 (有在動腦，記憶剛剛好)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    
                    Button(
                        onClick = { onRatingSelected("HARD") },
                        modifier = Modifier.fillMaxWidth().testTag("rating_hard"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = Color(0xFFC62828)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("困難 🪵 (很多不記得，需要頻繁複習)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("先不記難度，直接完成", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// Add Item detail modal
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(
    viewModel: RevisionViewModel,
    onDismiss: () -> Unit
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var tagsInput by remember { mutableStateOf("") }
    var selectedStartDate by remember { mutableStateOf(LocalDate.now()) }
    
    // Default Ebbinghaus curve parameters Preset selection: 0=Ebbinghaus, 1=Golden, 2=Sprint, 3=Custom
    var selectedPresetIndex by remember { mutableStateOf(0) }
    var customIntervalsInput by remember { mutableStateOf("1,3,7,12,30") }
    
    val presets = listOf(
        listOf(1, 2, 4, 7, 15),       // 艾賓浩斯古典曲線
        listOf(1, 3, 7, 14, 30),      // 黃金精省曲線
        listOf(1, 2, 3),              // 考前速記衝刺
    )
    
    // Auto populate category index
    LaunchedEffect(categories) {
        if (categories.isNotEmpty() && selectedCategoryId == null) {
            selectedCategoryId = categories.first().id
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.72f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("新增安排複習項目", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "關閉")
                    }
                }
                
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("複習標題 (例如：英文文法 Chapter 3)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_item_title"),
                    singleLine = true
                )
                
                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("詳細備註或核心重點說明 (選填)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_item_description"),
                    maxLines = 3
                )
                
                // Category Picker selection
                Text("選擇科目與分類：", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategoryId == cat.id
                        val catColor = try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                        
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text(cat.name) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(catColor)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor.copy(alpha = 0.25f),
                                selectedLabelColor = catColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("chip_category_${cat.id}")
                        )
                    }
                }
                
                // Tag labels
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("分類標籤 (多個請用逗號隔開，例如: 錯題, 公式)") },
                    placeholder = { Text("例如: 重點, 難題") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_item_tags"),
                    singleLine = true
                )
                
                // Start date (today base)
                var showDatePickerPopup by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = selectedStartDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    onValueChange = {},
                    label = { Text("複習起點日期") },
                    shape = RoundedCornerShape(12.dp),
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "起點") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePickerPopup = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "選擇起點")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (showDatePickerPopup) {
                    DatePickerInlinePopup(
                        currentDate = selectedStartDate,
                        onDismiss = { showDatePickerPopup = false },
                        onDateSelected = { 
                            selectedStartDate = it
                            showDatePickerPopup = false
                        }
                    )
                }
                
                // Presets Memory curve
                Text("複習規律與記憶週期：", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val presetsLabel = listOf(
                        "艾賓浩斯記憶法 (第 1,2,4,7,15 天)",
                        "黃金複習規律 (第 1,3,7,14,30 天)",
                        "考前重點速記衝刺 (第 1,2,3 天)",
                        "自訂週期規律時間"
                    )
                    
                    presetsLabel.forEachIndexed { idx, label ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedPresetIndex = idx }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedPresetIndex == idx,
                                onClick = { selectedPresetIndex = idx }
                            )
                            Text(label, fontSize = 13.sp)
                        }
                    }
                }
                
                if (selectedPresetIndex == 3) {
                    OutlinedTextField(
                        value = customIntervalsInput,
                        onValueChange = { customIntervalsInput = it },
                        label = { Text("自訂幾天後自動添加複習 (例如：1,5,10,20)") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_custom_intervals"),
                        singleLine = true
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Complete button
                Button(
                    onClick = {
                        if (title.trim().isEmpty()) return@Button
                        
                        val selectedIntervals = if (selectedPresetIndex == 3) {
                            customIntervalsInput.split(",")
                                .mapNotNull { it.trim().toIntOrNull() }
                                .filter { it > 0 }
                        } else {
                            presets[selectedPresetIndex]
                        }
                        
                        viewModel.addNewRevisionItem(
                            title = title,
                            description = description,
                            categoryId = selectedCategoryId ?: 1L,
                            tags = tagsInput,
                            startDate = selectedStartDate,
                            intervals = selectedIntervals
                        )
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_save_item"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("自動化派入日曆日程", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// Simple Inline date picker helper inside dialogue card
@Composable
fun DatePickerInlinePopup(
    currentDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.72f)),
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("選擇複習建檔起點", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Allow selecting offset days starting from today: today, tomorrow, dayAfter
                listOf(0, 1, 2, 3, 5, 7).forEach { dayOffset ->
                    val calculatedDate = LocalDate.now().plusDays(dayOffset.toLong())
                    val description = when(dayOffset) {
                        0 -> "今天 (${calculatedDate.format(DateTimeFormatter.ofPattern("M/d"))})"
                        1 -> "明天 (${calculatedDate.format(DateTimeFormatter.ofPattern("M/d"))})"
                        2 -> "後天 (${calculatedDate.format(DateTimeFormatter.ofPattern("M/d"))})"
                        else -> "${dayOffset}天後 (${calculatedDate.format(DateTimeFormatter.ofPattern("M/d"))})"
                    }
                    Button(
                        onClick = { onDateSelected(calculatedDate) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentDate == calculatedDate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (currentDate == calculatedDate) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(description)
                    }
                }
                
                TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) {
                    Text("取消")
                }
            }
        }
    }
}

// Add Category modal
@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    // Preschool colors choice mapping
    val colors = listOf(
        "#FFA726", // Orange
        "#29B6F6", // Blue
        "#66BB6A", // Green
        "#AB47BC", // Purple
        "#8D6E63", // Brown
        "#EC407A", // Pink
        "#26A69A", // Teal
        "#EF5350"  // Red
    )
    var selectedColor by remember { mutableStateOf(colors[0]) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("新增學科或複習類別", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("細分類別名稱 (例如: 計算機科學)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_category_name"),
                    singleLine = true
                )
                
                Text("選擇標籖色彩顏色：", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                // Color Presets grid selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    colors.forEach { col ->
                        val isSelected = selectedColor == col
                        val parsedColor = Color(android.graphics.Color.parseColor(col))
                        
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = col },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check, 
                                    contentDescription = "選中",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.trim().isNotEmpty()) {
                                onCreate(name, selectedColor)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_save_category")
                    ) {
                        Text("確認新增", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Time Selection modal
@Composable
fun TimeSelectionDialog(
    currentHour: Int,
    onDismiss: () -> Unit,
    onSelected: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.72f)),
            modifier = Modifier.padding(16.dp).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("調整每日打卡推播時間", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Common review trigger hour options
                listOf(7, 8, 9, 12, 18, 20, 21).forEach { hour ->
                    val label = when(hour) {
                        in 6..11 -> "早上 ${hour}:00 (元氣讀書時間)"
                        12 -> "中午 12:00 (午休醒腦)"
                        in 13..18 -> "下午 ${hour - 12}:00 (課後複習時光)"
                        else -> "晚間 ${hour - 12}:00 (睡前深層鞏固)"
                    }
                    Button(
                        onClick = { onSelected(hour) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentHour == hour) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (currentHour == hour) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(label, fontWeight = FontWeight.Bold)
                    }
                }
                
                TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 12.dp)) {
                    Text("取消")
                }
            }
        }
    }
}
