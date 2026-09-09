package com.ntando.ivu.ui.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

@Composable
fun AchievementScreen(viewModel: AchievementViewModel) {
    val stats by viewModel.userStats.collectAsState()
    var selectedBadge by remember { mutableStateOf<Badge?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8F0))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.title_progress),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3D2B1F),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Large Streak Icon
        Surface(
            modifier = Modifier.size(100.dp),
            color = Color.Transparent
        ) {
            Text(
                text = "🔥", // Image shows a leaf/flame
                fontSize = 80.sp,
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = pluralStringResource(R.plurals.days_format, stats?.currentStreak ?: 0, stats?.currentStreak ?: 0),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3D2B1F)
        )
        Text(
            text = "Level ${stats?.level ?: 1} · Wordsmith",
            fontSize = 14.sp,
            color = Color.LightGray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // XP Bar
        val xpProgress = (stats?.xp ?: 0) % 1000 // Assuming 1000 for level 5 in image
        LinearProgressIndicator(
            progress = { xpProgress / 1000f },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape),
            color = Color(0xFFE8C07C),
            trackColor = Color(0xFFEEEEEE)
        )
        Text(
            text = "${stats?.xp ?: 0} / 1000 XP to Level ${ (stats?.level ?: 0) + 1 }",
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp),
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        // This Week Section
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "This week",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3D2B1F)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(100.dp),
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

        // Badges Section
        Text(
            text = "Badges",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3D2B1F),
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(Badge.ALL) { badge ->
                val isUnlocked = stats?.badges?.contains(badge.id) == true
                BadgeItem(
                    badge = badge,
                    isUnlocked = isUnlocked,
                    onClick = { selectedBadge = badge }
                )
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
                    Text(if (isUnlocked) badge.description else "Condition: ${badge.description}")
                    if (isUnlocked) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Earned! 🎉", fontWeight = FontWeight.Bold, color = Color(0xFFE88A68))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedBadge = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun ProgressCard(title: String, value: String, icon: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontSize = 12.sp, color = Color.Gray)
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }
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
            containerColor = Color.White
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
                color = Color(0xFF3D2B1F)
            )
            Text(
                text = if (isUnlocked) "Unlocked" else "Locked",
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = if (isUnlocked) Color(0xFFE88A68) else Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
