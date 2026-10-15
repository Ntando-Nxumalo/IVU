package com.ntando.ivu.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntando.ivu.sync.SyncManager
import com.ntando.ivu.sync.SyncStatusRepository

/**
 * Animated banner displaying the number of offline changes waiting to sync with a manual "Sync now" action button.
 *
 * @param ownerUserId Firebase UID string of current user.
 * @param modifier Optional modifier.
 */
@Composable
fun PendingSyncBanner(
    ownerUserId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val syncStatusRepository = remember(context) { SyncStatusRepository(context) }
    val pendingCount by syncStatusRepository.getPendingCountFlow(ownerUserId).collectAsState(initial = 0)

    AnimatedVisibility(
        visible = pendingCount > 0,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFF3E0))
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (pendingCount == 1) "1 change waiting to sync" else "$pendingCount changes waiting to sync",
                color = Color(0xFFE65100),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            TextButton(
                onClick = {
                    SyncManager.enqueueSync(context)
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sync Now",
                    tint = Color(0xFFE65100),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Sync now",
                    color = Color(0xFFE65100),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
