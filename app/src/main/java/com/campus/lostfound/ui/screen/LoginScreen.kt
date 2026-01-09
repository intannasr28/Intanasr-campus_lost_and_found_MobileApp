package com.campus.lostfound.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.campus.lostfound.R
import com.campus.lostfound.ui.viewmodel.AuthViewModel
import com.campus.lostfound.util.GoogleSignInHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlin.math.sin

/**
 * 🎨 PREMIUM LOGIN SCREEN - Material 3 Design
 * 
 * Features:
 * - Modern gradient background with animated blobs
 * - Glassmorphism card effect
 * - Smooth micro-interactions & animations
 * - Real-time validation with visual feedback
 * - Haptic feedback on interactions
 * - Skeleton loading states
 * - Premium color palette (Soft Teal & Warm Gray)
 * - Responsive layout for all screen sizes
 * 
 * Color Palette:
 * - Primary: Soft Teal (#00897B → #26A69A)
 * - Secondary: Warm Gray (#F5F5F5 → #E0E0E0)
 * - Accent: Vibrant Orange (#FF6B35)
 * - Success: Fresh Green (#4CAF50)
 * - Error: Soft Red (#EF5350)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToHome: () -> Unit,
    onForgotPassword: () -> Unit
) {
    val viewModel: AuthViewModel = viewModel()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val loginForm by viewModel.loginForm.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    
    var passwordVisible by remember { mutableStateOf(false) }
    var emailFocused by remember { mutableStateOf(false) }
    var passwordFocused by remember { mutableStateOf(false) }
    
    // Animated background blobs
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val blob1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob1"
    )
    
    val blob2Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob2"
    )
    
    // Google Sign-In
    val googleSignInClient = remember {
        GoogleSignInHelper.getGoogleSignInClient(
            context,
            context.getString(R.string.default_web_client_id)
        )
    }
    
    val googleSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            GoogleSignInHelper.handleSignInResult(task)
                .onSuccess { idToken -> viewModel.loginWithGoogle(idToken) }
                .onFailure { /* Handled by viewModel */ }
        }
    }
    
    // Navigate on success
    LaunchedEffect(authState.isSuccess) {
        if (authState.isSuccess) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(300) // Allow animation to complete
            onNavigateToHome()
            viewModel.clearSuccess()
        }
    }
    
    // Error handling
    LaunchedEffect(authState.error) {
        authState.error?.let {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(3000)
            viewModel.clearError()
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 🎨 PREMIUM AMBIENT BACKGROUND with 3 moving blobs
        AmbientBackground(
            blob1Offset = blob1Offset,
            blob2Offset = blob2Offset
        )
        
        // 📱 MAIN CONTENT with responsive width
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // 🎭 HERO SECTION with new education icon
            AnimatedHeroSection()
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 💳 GLASSMORPHISM LOGIN CARD with responsive width
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 450.dp) // Tablet constraint
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Email Field with premium styling
                    PremiumTextField(
                        value = loginForm.email,
                        onValueChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setLoginEmail(it)
                        },
                        label = "Alamat Email",
                        placeholder = "email.anda@kampus.edu",
                        leadingIcon = Icons.Outlined.Email,
                        isError = loginForm.emailError != null,
                        errorMessage = loginForm.emailError,
                        isFocused = emailFocused,
                        onFocusChange = { emailFocused = it },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                    
                    // Password Field with premium styling
                    PremiumTextField(
                        value = loginForm.password,
                        onValueChange = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setLoginPassword(it)
                        },
                        label = "Kata Sandi",
                        placeholder = "Masukkan kata sandi Anda",
                        leadingIcon = Icons.Outlined.Lock,
                        trailingIcon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        onTrailingIconClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            passwordVisible = !passwordVisible
                        },
                        isError = loginForm.passwordError != null,
                        errorMessage = loginForm.passwordError,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isFocused = passwordFocused,
                        onFocusChange = { passwordFocused = it },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.login()
                            }
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Forgot Password Link (grouped with password field)
                    Text(
                        text = "Lupa kata sandi?",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onForgotPassword()
                            }
                            .padding(4.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    // Error Alert
                    AnimatedVisibility(
                        visible = authState.error != null,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        authState.error?.let { error ->
                            PremiumErrorCard(error)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Premium Gradient Sign In Button (Primary Action)
                    PremiumButton(
                        text = "Masuk",
                        isLoading = authState.isLoading,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.login()
                        }
                    )
                    
                    // Elegant Divider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0xFFBDBDBD)
                                        )
                                    )
                                )
                        )
                        Text(
                            text = "ATAU",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF757575),
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFBDBDBD),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                    
                    // Google Sign-In Button (Premium Flat Design)
                    PremiumSocialButton(
                        text = "Lanjutkan dengan Google",
                        icon = R.drawable.ic_google,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            googleSignInClient.signOut().addOnCompleteListener {
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            }
                        },
                        enabled = !authState.isLoading
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Guest Mode Link
                    PremiumTextButton(
                        text = "Lanjutkan sebagai Tamu",
                        icon = Icons.Outlined.Person,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch {
                                val settingsRepository = com.campus.lostfound.data.SettingsRepository(context)
                                settingsRepository.setGuestMode(true)
                                onNavigateToHome()
                            }
                        },
                        enabled = !authState.isLoading
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Register Link with animation
            AnimatedRegisterLink(
                onNavigateToRegister = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigateToRegister()
                }
            )
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

/**
 * 🎭 Animated Hero Section with Logo and Welcome Text
 */
@Composable
private fun AnimatedHeroSection() {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Animated Logo with scale + fade
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn()
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00897B).copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // New education/search icon with gradient
                Icon(
                    painter = painterResource(R.drawable.ic_education_search),
                    contentDescription = "Campus Lost & Found",
                    modifier = Modifier.size(90.dp),
                    tint = Color.Unspecified // Keep gradient colors
                )
            }
        }
        
        // Animated Welcome Text
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { 50 },
                animationSpec = tween(600)
            ) + fadeIn()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Welcome Back! 👋",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF212121),
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp
                )
                
                Text(
                    text = "Sign in to continue your journey",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF757575),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

/**
 * 🌈 Ambient Background with 3 Moving Blobs
 * Premium animated background with high blur effect
 */
@Composable
private fun AmbientBackground(
    blob1Offset: Float,
    blob2Offset: Float
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAF9)) // Very light base color
    ) {
        // Blob 1: Green Accent (Top Left to Center)
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(
                    x = (-30 + blob1Offset * 120).dp,
                    y = (50 + blob1Offset * 180).dp
                )
                .background(
                    color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                    shape = CircleShape
                )
                .blur(80.dp)
        )
        
        // Blob 2: Teal Soft (Bottom Right moving opposite)
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(
                    x = (220 - blob2Offset * 80).dp,
                    y = (500 + blob2Offset * 100).dp
                )
                .background(
                    color = Color(0xFF009688).copy(alpha = 0.12f),
                    shape = CircleShape
                )
                .blur(70.dp)
        )
        
        // Blob 3: Light Green Neutral (Center Left moving vertically)
        Box(
            modifier = Modifier
                .size(240.dp)
                .offset(
                    x = (40.dp),
                    y = (300 - blob1Offset * 150).dp
                )
                .background(
                    color = Color(0xFF81C784).copy(alpha = 0.10f),
                    shape = CircleShape
                )
                .blur(65.dp)
        )
    }
}

/**
 * 💳 Glassmorphic Card Component with enhanced transparency
 */
@Composable
private fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.90f), // Glassmorphism effect
        shadowElevation = 12.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = Color.White.copy(alpha = 0.6f)
        )
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            content()
        }
    }
}

/**
 * ✨ Premium Text Field with Enhanced Styling
 */
@Composable
private fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isFocused: Boolean = false,
    onFocusChange: (Boolean) -> Unit = {},
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> Color(0xFFEF5350)
            isFocused -> Color(0xFF00897B)
            else -> Color(0xFFE0E0E0)
        },
        animationSpec = tween(300),
        label = "border"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isFocused) Color(0xFFF5F5F5) else Color(0xFFFAFAFA),
        animationSpec = tween(300),
        label = "background"
    )
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal
                )
            },
            placeholder = {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFBDBDBD)
                )
            },
            leadingIcon = {
                Icon(
                    leadingIcon,
                    contentDescription = null,
                    tint = if (isFocused) Color(0xFF00897B) else Color(0xFF9E9E9E)
                )
            },
            trailingIcon = trailingIcon?.let {
                {
                    IconButton(onClick = { onTrailingIconClick?.invoke() }) {
                        Icon(
                            it,
                            contentDescription = null,
                            tint = Color(0xFF9E9E9E)
                        )
                    }
                }
            },
            visualTransformation = visualTransformation,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { onFocusChange(it.isFocused) }
                .background(backgroundColor, RoundedCornerShape(24.dp)),
            singleLine = true,
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = borderColor,
                unfocusedBorderColor = borderColor,
                errorBorderColor = Color(0xFFEF5350),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                errorContainerColor = Color(0xFFFFEBEE).copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(24.dp), // More rounded corners
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions
        )
        
        // Error message with animation
        AnimatedVisibility(
            visible = isError && errorMessage != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            errorMessage?.let {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = null,
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFEF5350),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 🎯 Premium Gradient Button
 */
@Composable
private fun PremiumButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 2.dp else 8.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = Color(0xFF4CAF50).copy(alpha = 0.4f)
            ),
        enabled = !isLoading,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF4CAF50), // Fresh green
                            Color(0xFF66BB6A), // Light green
                            Color(0xFF81C784)  // Soft green
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                    Text(
                        "Sedang masuk...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text(
                    text,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

/**
 * 🌐 Premium Social Login Button (Flat with Border)
 */
@Composable
private fun PremiumSocialButton(
    text: String,
    icon: Int,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )
    
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF424242)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color(0xFFE0E0E0)
        ),        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),        interactionSource = interactionSource
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = Color.Unspecified
            )
            Text(
                text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF424242)
            )
        }
    }
}

/**
 * 💬 Premium Text Button
 */
@Composable
private fun PremiumTextButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = Color(0xFF757575)
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * ⚠️ Premium Error Card
 */
@Composable
private fun PremiumErrorCard(errorMessage: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFEBEE),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color(0xFFEF5350).copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Error,
                contentDescription = null,
                tint = Color(0xFFEF5350),
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFC62828),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 📝 Animated Register Link
 */
@Composable
private fun AnimatedRegisterLink(onNavigateToRegister: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(8.dp)
    ) {
        Text(
            text = "Belum punya akun? ",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF757575)
        )
        Text(
            text = "Daftar",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF00897B),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable(onClick = onNavigateToRegister)
                .padding(4.dp)
        )
    }
}
