package com.ntando.ivu.ui.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.ntando.ivu.data.entity.Achievement
import com.ntando.ivu.viewmodel.AchievementViewModel

@Composable
fun AchievementScreen(viewModel: AchievementViewModel) {
    val achievements by viewModel.achievements.collectAsState()
    val stats by viewModel.userStats.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = stringResource(R.string.your_progress),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3D2B1F)
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // Streak and Level Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProgressCard(
                title = stringResource(R.string.current_streak),
                value = stringResource(R.string.days_format, stats?.currentStreak ?: 0),
                icon = "🔥",
                modifier = Modifier.weight(1f)
            )
            ProgressCard(
                title = stringResource(R.string.current_level),
                value = stringResource(R.string.level_format, stats?.level ?: 1),
                icon = "⭐",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // XP Bar
        Text(
            text = stringResource(R.string.experience_xp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        val xpProgress = (stats?.xp ?: 0) % 100
        LinearProgressIndicator(
            progress = { xpProgress / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape),
            color = Color(0xFFE88A68),
            trackColor = Color(0xFFEEEEEE)
        )
        Text(
            text = stringResource(R.string.xp_to_next_level, xpProgress),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp),
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Badges Section
        Text(
            text = stringResource(R.string.study_badges),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3D2B1F)
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(achievements) { achievement ->
                BadgeItem(achievement)
            }
        }
    }
}

@Composable
fun ProgressCard(title: String, value: String, icon: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontSize = 12.sp, color = Color.Gray)
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3D2B1F))
        }
    }
}

@Composable
fun BadgeItem(achievement: Achievement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) Color.White else Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (achievement.isUnlocked) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (achievement.isUnlocked) Color(0xFFFFD700).copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (achievement.isUnlocked) "🏅" else "🔒",
                    fontSize = 24.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = achievement.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = if (achievement.isUnlocked) Color(0xFF3D2B1F) else Color.Gray
            )
            Text(
                text = achievement.description,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
