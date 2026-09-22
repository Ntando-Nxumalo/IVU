package com.ntando.ivu.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntando.ivu.R

private const val TAG = "BottomNav"

/**
 * Reusable primary bottom navigation bar component supporting route switching across primary app sections.
 *
 * Layout Structure:
 * - [Surface] container with 8.dp tonal/shadow elevation.
 * - Evenly spaced [Row] displaying four [BottomNavItem] instances: Home, Decks, Journal, and Profile.
 *
 * @param currentScreen The string route identifier of the currently active screen (e.g. "home", "decks", "journal", "profile").
 * @param onNavigate Navigation callback triggered with the target route identifier string when an item is selected.
 */
@Composable
fun BottomNavigationBar(
    currentScreen: String,
    onNavigate: (String) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().height(80.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                label = stringResource(R.string.nav_home),
                icon = Icons.Default.Home,
                isSelected = currentScreen == "home",
            ) {
                Log.d(TAG, "Navigating to home screen")
                onNavigate("home")
            }
            BottomNavItem(
                label = stringResource(R.string.nav_decks),
                icon = Icons.AutoMirrored.Filled.List,
                isSelected = currentScreen == "decks",
            ) {
                Log.d(TAG, "Navigating to decks screen")
                onNavigate("decks")
            }
            BottomNavItem(
                label = stringResource(R.string.nav_journal),
                icon = Icons.Default.DateRange,
                isSelected = currentScreen == "journal",
            ) {
                Log.d(TAG, "Navigating to journal screen")
                onNavigate("journal")
            }
            BottomNavItem(
                label = stringResource(R.string.nav_profile),
                icon = Icons.Default.Person,
                isSelected = currentScreen == "profile",
            ) {
                Log.d(TAG, "Navigating to profile screen")
                onNavigate("profile")
            }
        }
    }
}

/**
 * Individual navigation tab displaying an icon and descriptive text label.
 * Applies highlighted color background alpha and bold typography when [isSelected] is true.
 *
 * @param label Localization string label describing the target destination.
 * @param icon Vector graphic icon associated with the tab destination.
 * @param isSelected Selection flag indicating if this tab represents the current route.
 * @param onClick Click event callback triggered when the user taps this navigation item.
 */
@Composable
fun BottomNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = if (isSelected) Color(0xFFE88A68) else Color.Gray
    Column(
        modifier = Modifier
            .clip(CircleShape)
            .clickable {
                Log.d(TAG, "BottomNavItem clicked: $label (isSelected=$isSelected)")
                onClick()
            }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isSelected) color.copy(alpha = 0.1f) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
    }
}
