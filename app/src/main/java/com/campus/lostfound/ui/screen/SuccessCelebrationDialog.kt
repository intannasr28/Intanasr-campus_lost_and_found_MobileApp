package com.campus.lostfound.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay

@Composable
fun SuccessCelebrationDialog(
    onDismiss: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(600)  // Delay awal diperlambat untuk entrance lebih dramatis
        showContent = true
        delay(800)  // Beri waktu lebih lama untuk user lihat animasi
        showConfetti = true
        // User kontrol penuh kapan ingin menutup dialog
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    AnimatedVisibility(
                        visible = showContent,
                        enter = scaleIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            initialScale = 0.3f  // Mulai dari lebih kecil untuk efek dramatis
                        ) + fadeIn(
                            animationSpec = tween(600)  // Fade in lebih smooth
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Animated Success Icon
                            SuccessIcon(showConfetti)
                        
                        // Title
                        Text(
                            text = "🎉 Berhasil!",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        
                        // Description
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Laporan berhasil dibuat!",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Kami akan segera menampilkan laporanmu di halaman utama.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Success Features
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val primaryColor = MaterialTheme.colorScheme.primary
                            SuccessFeatureRow(
                                icon = Icons.Default.Visibility,
                                text = "Laporan dapat dilihat semua pengguna",
                                iconColor = primaryColor
                            )
                            SuccessFeatureRow(
                                icon = Icons.Default.Notifications,
                                text = "Notifikasi akan dikirim ke pelapor",
                                iconColor = primaryColor
                            )
                            SuccessFeatureRow(
                                icon = Icons.Default.Phone,
                                text = "Kontak dapat dihubungi via WhatsApp",
                                iconColor = primaryColor
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Action Button
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Kembali ke Beranda",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuccessIcon(animate: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (animate) 1.3f else 1.0f,
        animationSpec = repeatable(
            iterations = 6,  // Lebih banyak bouncing
            animation = tween(600, easing = FastOutSlowInEasing),  // Durasi lebih lama
            repeatMode = RepeatMode.Reverse
        ),
        label = "Icon Scale"
    )
    
    val rotation by animateFloatAsState(
        targetValue = if (animate) 360f else 0f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),  // Rotasi lebih smooth dan lambat
        label = "Icon Rotation"
    )
    
    Box(
        modifier = Modifier
            .size(100.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = Color.White
        )
    }
    
    // Confetti Effect (simplified)
    if (animate) {
        Row(
            modifier = Modifier.offset(y = (-20).dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(5) { index ->
                ConfettiPiece(delay = index * 100L)
            }
        }
    }
}

@Composable
private fun ConfettiPiece(delay: Long) {
    var startAnimation by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(delay)
        startAnimation = true
    }
    
    val offsetY by animateDpAsState(
        targetValue = if (startAnimation) 100.dp else 0.dp,  // Jatuh lebih jauh
        animationSpec = tween(2000, easing = FastOutSlowInEasing),  // Durasi lebih lama
        label = "Confetti Fall"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 1f,
        animationSpec = tween(2000),  // Fade out lebih smooth
        label = "Confetti Fade"
    )
    
    Box(
        modifier = Modifier
            .size(8.dp)
            .offset(y = offsetY)
            .clip(CircleShape)
            .background(
                listOf(
                    Color(0xFFFFEB3B),
                    Color(0xFFFF9800),
                    Color(0xFFE91E63),
                    Color(0xFF9C27B0),
                    Color(0xFF2196F3)
                ).random().copy(alpha = alpha)
            )
    )
}

@Composable
private fun SuccessFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    iconColor: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = iconColor
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
