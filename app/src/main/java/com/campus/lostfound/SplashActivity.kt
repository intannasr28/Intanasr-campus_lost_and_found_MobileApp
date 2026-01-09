package com.campus.lostfound

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.colorResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.campus.lostfound.ui.theme.CampusLostFoundTheme
import com.campus.lostfound.ui.theme.PrimaryBlue
import com.campus.lostfound.ui.theme.PrimaryBlueDark
import kotlinx.coroutines.delay
import com.campus.lostfound.R

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install SplashScreen API for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val splashScreen = installSplashScreen()
            splashScreen.setKeepOnScreenCondition { false }
        }
        
        super.onCreate(savedInstanceState)
        // set status/navigation bar colors to match splash for a seamless look
        try {
            window.statusBarColor = ContextCompat.getColor(this, R.color.splash_opening_top)
            window.navigationBarColor = ContextCompat.getColor(this, R.color.splash_opening_bottom)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        } catch (_: Exception) {
            // ignore if setting system bars fails on older devices
        }
        setContent {
            CampusLostFoundTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SplashScreenContent {
                        // Navigate to MainActivity after animation
                        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreenContent(onTimeout: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    
    // Logo scale animation (0.85 → 1.0, spring)
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )
    
    // Logo alpha animation (fade in)
    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 420,
            easing = LinearOutSlowInEasing
        ),
        label = "logoAlpha"
    )
    
    // Text fade up animation (320ms delay, 360ms duration)
    val textAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 360,
            delayMillis = 320,
            easing = LinearOutSlowInEasing
        ),
        label = "textAlpha"
    )
    
    val textOffsetY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 18f,
        animationSpec = tween(
            durationMillis = 360,
            delayMillis = 320,
            easing = LinearOutSlowInEasing
        ),
        label = "textOffsetY"
    )
    
    // Screen fade out animation
    var fadeOut by remember { mutableStateOf(false) }
    val screenAlpha by animateFloatAsState(
        targetValue = if (fadeOut) 0f else 1f,
        animationSpec = tween(
            durationMillis = 300,
            easing = LinearEasing
        ),
        label = "screenAlpha"
    )
    
    LaunchedEffect(Unit) {
        startAnimation = true
        // show for ~1.0s then fade (faster)
        delay(1000)
        fadeOut = true
        delay(280)
        onTimeout()
    }
    
    val bgTop = colorResource(id = R.color.splash_opening_top)
    val bgBottom = colorResource(id = R.color.splash_opening_bottom)

    // subtle bobbing animation - lighter movement
    val infinite = rememberInfiniteTransition()
    val bobbing by infinite.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobbing"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        bgTop,
                        bgBottom
                    )
                )
            )
            .alpha(screenAlpha),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
                    // Logo with subtle glow, shadow, fade + scale animation (use splash_opening.png)
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .graphicsLayer { translationY = bobbing },
                        contentAlignment = Alignment.Center
                    ) {
                        // Subtle glow - more minimal
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .background(
                                    color = colorResource(id = R.color.splash_opening_accent).copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(24.dp)
                                )
                        )

                        // actual icon - cleaner presentation
                        Image(
                            painter = painterResource(id = R.drawable.splash_opening),
                            contentDescription = "App Icon",
                            modifier = Modifier
                                .size(110.dp)
                                .scale(logoScale)
                                .alpha(logoAlpha)
                                .graphicsLayer { translationY = bobbing }
                        )
                    }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // App Name - refined typography
            Text(
                text = "Campus Lost & Found",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = (-0.5).sp,
                modifier = Modifier
                    .alpha(textAlpha)
                    .graphicsLayer {
                        translationY = textOffsetY
                    }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tagline - subtle
            Text(
                text = "Temukan & Laporkan Barang Hilang",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .alpha(textAlpha)
                    .graphicsLayer {
                        translationY = textOffsetY
                    }
            )
            Spacer(modifier = Modifier.height(18.dp))
                ThreeDotLoader(
                    modifier = Modifier.alpha(if (startAnimation) 1f else 0f),
                    dotColor = Color.White
                )
            }

            // subtle vignette overlays to focus center
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0x22000000), Color.Transparent)
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0x22000000))
                        )
                    )
            )
    }
}

@Composable
fun ThreeDotLoader(modifier: Modifier = Modifier, dotColor: Color = Color.White) {
    val infinite = rememberInfiniteTransition()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0..2) {
            val scale by infinite.animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, easing = LinearEasing, delayMillis = i * 150),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dotScale$i"
            )

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .scale(scale)
                    .background(color = dotColor, shape = RoundedCornerShape(50))
            )
        }
    }
}

