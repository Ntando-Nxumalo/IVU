package com.ntando.ivu.ui.achievements

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntando.ivu.data.entity.Achievement
import com.ntando.ivu.viewmodel.AchievementViewModel

/**
 * AchievementScreen for IVU.
 * Displays study streaks, card mastery, and other academic milestones.
 */
@Composable
fun AchievementScreen(viewModel: AchievementViewModel) {
    val achievements by viewModel.achievements.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Study Badges",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val unlockedCount = achievements.count { it.isUnlocked }
        Text(
            text = "$unlockedCount / ${achievements.size} Badges Earned",
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(achievements) { achievement ->
                AchievementItem(achievement)
            }
        }
    }
}

@Composable
fun AchievementItem(achievement: Achievement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon placeholder
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = if (achievement.isUnlocked) Color(0xFFFFD700) else Color.Gray
            ) {
                // In a real app, we'd use icons like bolt, school, edit
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = achievement.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (achievement.isUnlocked) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        Color.Gray
                )
                Text(
                    text = achievement.description,
                    fontSize = 14.sp,
                    color = if (achievement.isUnlocked) 
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) 
                    else 
                        Color.Gray
                )
            }
        }
    }
}
