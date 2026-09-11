package com.ntando.ivu.ui.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntando.ivu.R
import com.ntando.ivu.data.entity.Badge
import com.ntando.ivu.viewmodel.AchievementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementScreen(
    viewModel: AchievementViewModel,
    onNavigate: (String) -> Unit
) {
    val stats by viewModel.userStats.collectAsState()
    var selectedBadge by remember { mutableStateOf<Badge?>(null) }

    Scaffold(
        bottomBar = {
            com.ntando.ivu.ui.components.BottomNavigationBar(
                currentScreen = "profile",
                onNavigate = onNavigate
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(20.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.title_progress),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            item {
                // Large Streak Icon
                Surface(
                    modifier = Modifier.size(100.dp),
                    color = Color.Transparent
                ) {
                    Text(
                        text = "🔥",
                        fontSize = 80.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Text(
                    text = stringResource(R.string.streak_format, stats?.currentStreak ?: 0),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.label_level_wordsmith, stats?.level ?: 1),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                // XP Bar
                val xpProgress = (stats?.xp ?: 0) % 100
                LinearProgressIndicator(
                    progress = { xpProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(CircleShape),
                    color = Color(0xFFE8C07C),
                    trackColor = Color(0xFFEEEEEE)
                )
                Text(
                    text = "${stats?.xp ?: 0} / ${((stats?.level ?: 1) * 100)} XP total",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                // This Week Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.label_this_week_section),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val mockHeights = listOf(0.4f, 0.7f, 0.3f, 0.8f, 0.5f, 0.2f, 0.6f)
                        val days = listOf("M", "T", "W", "T", "F", "S", "S")
                        mockHeights.forEachIndexed { index, height ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .width(12.dp)
                                        .fillMaxHeight(height)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF7FB6A7))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(days[index], fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                // Badges Section
                Text(
                    text = stringResource(R.string.label_badges_section),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(Badge.ALL.chunked(2)) { rowBadges ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowBadges.forEach { badge ->
                        val isUnlocked = stats?.badges?.contains(badge.id) == true
                        Box(modifier = Modifier.weight(1f)) {
                            BadgeItem(
                                badge = badge,
                                isUnlocked = isUnlocked,
                                onClick = { selectedBadge = badge }
                            )
                        }
                    }
                    if (rowBadges.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    selectedBadge?.let { badge ->
        val isUnlocked = stats?.badges?.contains(badge.id) == true
        AlertDialog(
            onDismissRequest = { selectedBadge = null },
            title = { Text(badge.displayName) },
            text = { 
                Column {
                    Text(if (isUnlocked) badge.description else stringResource(R.string.label_condition, badge.description))
                    if (isUnlocked) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.label_earned), fontWeight = FontWeight.Bold, color = Color(0xFFE88A68))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedBadge = null }) {
                    Text(stringResource(R.string.btn_close))
                }
            }
        )
    }
}

@Composable
fun BadgeItem(badge: Badge, isUnlocked: Boolean, onClick: () -> Unit) {
    val badgeColors = mapOf(
        "FIRST_REVIEW" to Color(0xFFE8C07C),
        "CARDS_50" to Color(0xFF7FB6A7),
        "FIRST_JOURNAL" to Color(0xFFE88A68),
        "STREAK_7" to Color(0xFF7FB6A7),
        "STREAK_30" to Color(0xFFB8C07C)
    )
    val color = badgeColors[badge.id] ?: Color.LightGray

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (isUnlocked) color else Color.LightGray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isUnlocked) "⭐" else "🔒",
                    fontSize = 28.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = badge.displayName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isUnlocked) stringResource(R.string.label_unlocked) else stringResource(R.string.label_locked),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = if (isUnlocked) Color(0xFFE88A68) else Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
