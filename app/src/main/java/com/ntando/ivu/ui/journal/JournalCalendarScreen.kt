package com.ntando.ivu.ui.journal

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntando.ivu.R
import com.ntando.ivu.network.JournalEntry
import com.ntando.ivu.viewmodel.JournalUiState
import com.ntando.ivu.viewmodel.JournalViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "JournalCalendarScreen"

/**
 * Interactive calendar and journal overview screen.
 *
 * Layout Structure:
 * - [Scaffold] with [TopAppBar], [com.ntando.ivu.ui.components.BottomNavigationBar], and a [FloatingActionButton] to create new entries.
 * - Custom [CalendarView] containing month navigation controls and an infinite [HorizontalPager] for monthly date grids.
 * - [LazyColumn] rendering [JournalEntryItem] entries filtered specifically for the currently selected date.
 * - [AlertDialog] displaying celebratory feedback when a new badge is unlocked upon saving an entry.
 *
 * @param viewModel ViewModel handling journal entry retrieval and badge unlock flows.
 * @param onBack Callback triggered when tapping the back navigation arrow.
 * @param onAddEntry Callback navigating to the entry creation screen for a given date: `(Calendar) -> Unit`.
 * @param onNavigate Navigation callback triggered when switching bottom bar screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalCalendarScreen(
    viewModel: JournalViewModel,
    onBack: () -> Unit,
    onAddEntry: (Calendar) -> Unit,
    onNavigate: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var badgeToShow by remember { mutableStateOf<com.ntando.ivu.data.entity.Badge?>(null) }

    // Handle badge unlock feedback
    LaunchedEffect(uiState) {
        Log.d(TAG, "Observed JournalUiState update: $uiState")
        val state = uiState
        if (state is JournalUiState.Success && state.newlyUnlockedBadges.isNotEmpty()) {
            val newlyUnlocked = state.newlyUnlockedBadges.first()
            Log.i(TAG, "Newly unlocked badge detected: ${newlyUnlocked.displayName}")
            badgeToShow = newlyUnlocked
        }
    }

    if (badgeToShow != null) {
        AlertDialog(
            onDismissRequest = { 
                Log.d(TAG, "Dismissing badge unlock dialog")
                badgeToShow = null 
                viewModel.clearUnlockedBadges()
            },
            title = { Text("🎉 Badge Unlocked!") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("🏅", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(badgeToShow?.displayName ?: "", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(badgeToShow?.description ?: "", textAlign = TextAlign.Center)
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        Log.d(TAG, "Badge unlock confirmation button clicked")
                        badgeToShow = null 
                        viewModel.clearUnlockedBadges()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE88A68))
                ) {
                    Text("Awesome!")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_my_calendar), fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d(TAG, "Top bar back button clicked")
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE88A68)
                )
            )
        },
        bottomBar = {
            com.ntando.ivu.ui.components.BottomNavigationBar(
                currentScreen = "journal",
                onNavigate = { route ->
                    Log.d(TAG, "Navigating from JournalCalendarScreen to route: $route")
                    onNavigate(route)
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    Log.d(TAG, "FAB clicked to add journal entry for selected date: ${selectedDate.time}")
                    onAddEntry(selectedDate)
                },
                containerColor = Color(0xFFE88A68),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_entry))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            CalendarView(
                selectedDate = selectedDate,
                entries = (uiState as? JournalUiState.Success)?.entries ?: emptyList(),
                onDateSelected = { newDate ->
                    Log.d(TAG, "Date selected in CalendarView: ${newDate.time}")
                    selectedDate = newDate
                }
            )
            
            Text(
                text = stringResource(R.string.label_entries_this_week),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(16.dp)
            )
            
            Box(modifier = Modifier.weight(1f)) {
                when (val state = uiState) {
                    is JournalUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color(0xFFE88A68))
                    is JournalUiState.Error -> Text(state.message, color = Color.Red, modifier = Modifier.align(Alignment.Center))
                    is JournalUiState.Success -> {
                        val filteredEntries = state.entries.filter { 
                            isSameDay(it.date, selectedDate)
                        }
                        
                        if (filteredEntries.isEmpty()) {
                            Text(
                                stringResource(R.string.no_entries_day),
                                modifier = Modifier.align(Alignment.Center),
                                color = Color.Gray
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredEntries) { entry ->
                                    JournalEntryItem(entry)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Calendar grid component rendering month navigation controls, day of week labels, and a month page view.
 * Highlights dates that contain recorded journal entries with small dot indicators.
 *
 * @param selectedDate Currently active [Calendar] date selection.
 * @param entries Complete list of [JournalEntry] items used to mark days with recorded entries.
 * @param onDateSelected Callback invoked when a user taps a date cell in the month grid.
 */
@Composable
fun CalendarView(
    selectedDate: Calendar,
    entries: List<JournalEntry>,
    onDateSelected: (Calendar) -> Unit
) {
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
    
    // Use a large initial page count for the pager
    val initialPage = 500
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 1000 })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val calendarForMonth = Calendar.getInstance().apply {
                add(Calendar.MONTH, pagerState.currentPage - initialPage)
            }
            
            IconButton(onClick = { 
                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev Month", tint = Color(0xFFE88A68))
            }
            
            Text(
                text = monthFormat.format(calendarForMonth.time),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            IconButton(onClick = { 
                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month", tint = Color(0xFFE88A68))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) { page ->
            val calendar = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, page - initialPage)
            }
            
            val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
            val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            
            Column {
                var currentDay = 1
                for (i in 0 until 6) {
                    if (currentDay > maxDays) break
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (j in 0 until 7) {
                            val isDayInMonth = (i > 0 || j >= firstDayOfWeek) && currentDay <= maxDays
                            if (isDayInMonth) {
                                val dayNum = currentDay
                                val dayDateFinal = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, dayNum) }
                                
                                val isSelected = isSameDay(dayDateFinal, selectedDate)
                                val hasEntry = entries.any { isSameDay(it.date, dayDateFinal) }
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFFE88A68) else Color.Transparent)
                                        .clickable { onDateSelected(dayDateFinal) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayNum.toString(),
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (hasEntry) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) Color.White else Color(0xFFE88A68))
                                            )
                                        }
                                    }
                                }
                                currentDay++
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card component displaying summary details of a single [JournalEntry] (mood indicator, formatted date, deck link indicator).
 *
 * @param entry The [JournalEntry] model object to display.
 */
@Composable
fun JournalEntryItem(entry: JournalEntry) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (_, moodColor) = when (entry.mood.lowercase()) {
                "great" -> "😊" to Color(0xFF7FB6A7)
                "okay" -> "😐" to Color(0xFFE8C07C)
                "tough" -> "😔" to Color(0xFFE88A68)
                else -> "😶" to Color.Gray
            }
            
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = moodColor.copy(alpha = 0.6f)
            ) {}
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                val inputSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val displaySdf = SimpleDateFormat("EEE d MMM", Locale.getDefault())
                val dateText = try {
                    val date = inputSdf.parse(entry.date)
                    if (date != null) displaySdf.format(date) else entry.date
                } catch (e: Exception) {
                    entry.date
                }
                Text(
                    text = "$dateText — Feeling ${entry.mood.lowercase()}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (entry.linkedDeckId != null) stringResource(R.string.label_linked_to_deck) else stringResource(R.string.label_no_deck_linked),
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
            }
        }
    }
}

/**
 * Helper function checking if a formatted date string ("yyyy-MM-dd") represents the same calendar day as a [Calendar] instance.
 */
private fun isSameDay(dateStr: String, calendar: Calendar): Boolean {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateStr)
        val cal = Calendar.getInstance().apply { time = date!! }
        cal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
    } catch (e: Exception) {
        false
    }
}

/**
 * Helper function checking if two [Calendar] instances represent the exact same calendar day.
 */
private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
