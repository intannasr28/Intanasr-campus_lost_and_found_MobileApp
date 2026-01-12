package com.campus.lostfound.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campus.lostfound.navigation.Screen

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem(
            screen = Screen.Home,
            label = "Beranda",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home
        ),
        BottomNavItem(
            screen = Screen.Explore,
            label = "Jelajah",
            selectedIcon = Icons.Filled.Explore,
            unselectedIcon = Icons.Outlined.Explore
        ),
        BottomNavItem(
            screen = Screen.Add,
            label = "Lapor",
            selectedIcon = Icons.Filled.Add,
            unselectedIcon = Icons.Outlined.Add
        ),
        BottomNavItem(
            screen = Screen.Activity,
            label = "Aktivitas",
            selectedIcon = Icons.Filled.Assignment,
            unselectedIcon = Icons.Outlined.Assignment
        ),
        BottomNavItem(
            screen = Screen.Settings,
            label = "Profil",
            selectedIcon = Icons.Filled.AccountCircle,
            unselectedIcon = Icons.Outlined.AccountCircle
        )
    )
    
    // Premium surface with elevated shadow
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.screen.route
                val animatedScale by animateFloatAsState(
                    targetValue = if (selected) 1.1f else 1f,
                    animationSpec = tween(durationMillis = 200),
                    label = "scale"
                )
                val animatedColor by animateColorAsState(
                    targetValue = if (selected) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 200),
                    label = "color"
                )
                
                NavigationBarItem(
                    icon = {
                        Box(
                            modifier = Modifier.scale(animatedScale),
                            contentAlignment = Alignment.Center
                        ) {
                            // Special handling for Add button - make it stand out
                            if (item.screen == Screen.Add) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = CircleShape,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                    shadowElevation = 0.dp
                                ) {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label,
                                        tint = if (selected) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(8.dp)
                                    )
                                }
                            } else {
                                // Regular icon untuk semua tab lain
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                    tint = animatedColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    },
                    label = {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = animatedColor,
                            maxLines = 1
                        )
                    },
                    selected = selected,
                    onClick = { onNavigate(item.screen.route) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Transparent, // Handled by animatedColor
                        unselectedIconColor = Color.Transparent,
                        selectedTextColor = Color.Transparent,
                        unselectedTextColor = Color.Transparent,
                        indicatorColor = Color.Transparent // No default indicator
                    )
                )
            }
        }
    }
}

