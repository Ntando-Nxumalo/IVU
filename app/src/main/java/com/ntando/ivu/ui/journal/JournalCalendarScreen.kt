package com.ntando.ivu.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalCalendarScreen(
    viewModel: JournalViewModel,
    onBack: () -> Unit,
    onAddEntry: (Calendar) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.journal_calendar), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddEntry(selectedDate) },
                containerColor = Color(0xFFE88A68),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_entry))
            }
        },
        containerColor = Color(0xFFFFF8F0)
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            CalendarView(
                selectedDate = selectedDate,
                entries = (uiState as? JournalUiState.Success)?.entries ?: emptyList(),
                onDateSelected = { selectedDate = it }
            )
            
            HorizontalDivider(thickness = 1.dp, color = Color.LightGray.copy(alpha = 0.3f))
            
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

@Composable
fun CalendarView(
    selectedDate: Calendar,
    entries: List<JournalEntry>,
    onDateSelected: (Calendar) -> Unit
) {
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
    
    val calendar = remember(selectedDate) {
        val cal = selectedDate.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal
    }
    
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
    val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = monthFormat.format(calendar.time),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3D2B1F),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
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
        
        var currentDay = 1
        for (i in 0 until 6) {
            if (currentDay > maxDays) break
            Row(modifier = Modifier.fillMaxWidth()) {
                for (j in 0 until 7) {
                    val isDayInMonth = (i > 0 || j >= firstDayOfWeek) && currentDay <= maxDays
                    if (isDayInMonth) {
                        val dayDate = (calendar.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, currentDay) }
                        val isSelected = isSameDay(dayDate, selectedDate)
                        val hasEntry = entries.any { isSameDay(it.date, dayDate) }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFFE88A68) else Color.Transparent)
                                .clickable { onDateSelected(dayDate) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = currentDay.toString(),
                                    color = if (isSelected) Color.White else Color(0xFF3D2B1F),
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

@Composable
fun JournalEntryItem(entry: JournalEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (moodEmoji, moodColor) = when (entry.mood.lowercase()) {
                    "great" -> "😊" to Color(0xFF4CAF50)
                    "okay" -> "😐" to Color(0xFFFFC107)
                    "tough" -> "😔" to Color(0xFFF44336)
                    else -> "😶" to Color.Gray
                }
                
                Surface(
                    color = moodColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$moodEmoji ${entry.mood.uppercase()}",
                        color = moodColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Text(
                text = entry.text,
                modifier = Modifier.padding(top = 12.dp),
                fontSize = 15.sp,
                color = Color(0xFF3D2B1F)
            )
            
            if (entry.linkedDeckId != null) {
                Text(
                    text = "Linked to a deck",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

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

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
