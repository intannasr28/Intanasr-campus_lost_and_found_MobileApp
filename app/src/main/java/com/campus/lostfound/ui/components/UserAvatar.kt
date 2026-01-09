package com.campus.lostfound.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * UserAvatar Component
 * 
 * Hybrid strategy for Firebase Free Tier:
 * - Google Sign-In users: Display Google photo
 * - Email/Password users: Display initial avatar (letter + color)
 */
@Composable
fun UserAvatar(
    photoUrl: String,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp
) {
    if (photoUrl.isNotEmpty() && photoUrl.startsWith("http")) {
        // Google photo available - use AsyncImage
        AsyncImage(
            model = photoUrl,
            contentDescription = "Profile photo of $name",
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        // No photo - use initial avatar
        InitialAvatar(
            name = name,
            modifier = modifier,
            size = size
        )
    }
}

@Composable
fun InitialAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp
) {
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val backgroundColor = getColorFromName(name)
    
    Box(
        modifier = modifier
            .size(size)
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            fontSize = (size.value / 2.5f).sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

// Generate consistent color from name (same name = same color)
private fun getColorFromName(name: String): Color {
    val colors = listOf(
        Color(0xFF2196F3), // Blue
        Color(0xFF4CAF50), // Green
        Color(0xFFFF9800), // Orange
        Color(0xFFE91E63), // Pink
        Color(0xFF9C27B0), // Purple
        Color(0xFF00BCD4), // Cyan
        Color(0xFFFF5722), // Deep Orange
        Color(0xFF3F51B5), // Indigo
        Color(0xFF009688), // Teal
        Color(0xFFCDDC39)  // Lime
    )
    
    val hash = name.hashCode()
    val index = (hash % colors.size).let { if (it < 0) it + colors.size else it }
    return colors[index]
}

// Small avatar for list items
@Composable
fun SmallUserAvatar(
    photoUrl: String,
    name: String,
    modifier: Modifier = Modifier
) {
    UserAvatar(
        photoUrl = photoUrl,
        name = name,
        modifier = modifier,
        size = 40.dp
    )
}

// Large avatar for profile screen
@Composable
fun LargeUserAvatar(
    photoUrl: String,
    name: String,
    modifier: Modifier = Modifier
) {
    UserAvatar(
        photoUrl = photoUrl,
        name = name,
        modifier = modifier,
        size = 120.dp
    )
}
