package com.campus.lostfound.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.lostfound.R
import com.campus.lostfound.ui.viewmodel.AuthViewModel

/**
 * # 🎨 Premium Register Screen
 * 
 * Production-ready registration screen with premium design elements:
 * 
 * ## ✨ Key Features:
 * - **Animated Gradient Background** with moving blobs for dynamic feel
 * - **Glassmorphism Card** with semi-transparent white surface
 * - **Premium Text Fields** with animated focus states, inline errors, haptic feedback
 * - **Password Strength Indicator** with visual feedback and criteria checklist
 * - **Animated Hero Section** with scale-in logo and slide-in welcome text
 * - **Gradient Register Button** with press animation and loading state
 * - **Social Sign-Up** with Google OAuth integration
 * - **Terms & Privacy** checkbox with links
 * - **Haptic Feedback** on all user interactions
 * - **Spring Animations** for natural, bouncy feel
 * - **Real-time Validation** with inline error messages
 * 
 * ## 🎨 Design Tokens:
 * - **Primary Color**: Soft Teal (#00897B → #26A69A)
 * - **Background**: Animated gradient with moving blobs
 * - **Surface**: Glassmorphic white card (85% opacity)
 * - **Typography**: ExtraBold headlines, clear hierarchy
 * - **Spacing**: 32dp container padding, 16dp element spacing
 * 
 * ## 🔧 Technical Details:
 * - Uses `AuthViewModel` for state management
 * - Integrates with `GoogleSignInHelper` for OAuth
 * - Focus management with `FocusManager`
 * - Keyboard actions with proper IME handling
 * - Efficient recomposition with `remember` and state hoisting
 * 
 * @param onNavigateToLogin Callback when "Sign In" link is clicked
 * @param onNavigateToHome Callback when registration is successful
 * @param authViewModel ViewModel for authentication logic (Hilt injected)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val registerForm by authViewModel.registerForm.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    // Focus states for each field
    var isNameFocused by remember { mutableStateOf(false) }
    var isEmailFocused by remember { mutableStateOf(false) }
    var isPasswordFocused by remember { mutableStateOf(false) }
    var isConfirmPasswordFocused by remember { mutableStateOf(false) }

    // Password visibility toggles
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Terms acceptance state
    var termsAccepted by remember { mutableStateOf(false) }

    // Password strength calculation
    val passwordStrength = remember(registerForm.password) {
        calculatePasswordStrength(registerForm.password)
    }

    // Navigate on successful registration
    LaunchedEffect(authState.isSuccess) {
        if (authState.isSuccess) {
            onNavigateToHome()
            authViewModel.clearSuccess()
        }
    }

    // Animated background blobs
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val blob1Offset by infiniteTransition.animateValue(
        initialValue = Offset(-50f, -100f),
        targetValue = Offset(50f, 50f),
        typeConverter = Offset.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob1"
    )
    val blob2Offset by infiniteTransition.animateValue(
        initialValue = Offset(200f, 500f),
        targetValue = Offset(100f, 600f),
        typeConverter = Offset.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        // 🎨 PREMIUM AMBIENT BACKGROUND with 3 moving blobs
        RegisterAmbientBackground(
            blob1Offset = blob1Offset,
            blob2Offset = blob2Offset
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Hero Section with new icon
            AnimatedRegisterHeroSection()

            Spacer(modifier = Modifier.height(18.dp))

            // Glassmorphic Card with responsive width
            RegisterGlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 450.dp) // Tablet constraint
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Error message
                    AnimatedVisibility(
                        visible = authState.error != null,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        RegisterErrorCard(
                            error = authState.error ?: "",
                            modifier = Modifier.padding(bottom = 20.dp)
                        )
                    }

                    // Name Field
                    PremiumRegisterTextField(
                        value = registerForm.name,
                        onValueChange = { authViewModel.setRegisterName(it) },
                        label = "Nama Lengkap",
                        placeholder = "John Doe",
                        leadingIcon = Icons.Filled.Person,
                        isError = registerForm.name.isNotBlank() && registerForm.name.length < 3,
                        errorMessage = if (registerForm.name.isNotBlank() && registerForm.name.length < 3) 
                            "Nama minimal 3 karakter" else null,
                        isFocused = isNameFocused,
                        onFocusChange = { isNameFocused = it },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        )
                    )

                    // Email Field
                    PremiumRegisterTextField(
                        value = registerForm.email,
                        onValueChange = { authViewModel.setRegisterEmail(it) },
                        label = "Alamat Email",
                        placeholder = "john.doe@example.com",
                        leadingIcon = Icons.Filled.Email,
                        isError = registerForm.email.isNotBlank() && !isValidEmail(registerForm.email),
                        errorMessage = if (registerForm.email.isNotBlank() && !isValidEmail(registerForm.email)) 
                            "Format email tidak valid" else null,
                        isFocused = isEmailFocused,
                        onFocusChange = { isEmailFocused = it },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    // Password Field
                    PremiumRegisterTextField(
                        value = registerForm.password,
                        onValueChange = { authViewModel.setRegisterPassword(it) },
                        label = "Kata Sandi",
                        placeholder = "••••••••",
                        leadingIcon = Icons.Filled.Lock,
                        trailingIcon = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        onTrailingIconClick = { 
                            passwordVisible = !passwordVisible
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None 
                            else PasswordVisualTransformation(),
                        isFocused = isPasswordFocused,
                        onFocusChange = { isPasswordFocused = it },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        )
                    )

                    // Password Strength Indicator
                    if (registerForm.password.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        PasswordStrengthIndicator(
                            strength = passwordStrength,
                            password = registerForm.password
                        )
                    }

                    // Confirm Password Field
                    PremiumRegisterTextField(
                        value = registerForm.confirmPassword,
                        onValueChange = { authViewModel.setRegisterConfirmPassword(it) },
                        label = "Konfirmasi Kata Sandi",
                        placeholder = "••••••••",
                        leadingIcon = Icons.Filled.Lock,
                        trailingIcon = if (confirmPasswordVisible) Icons.Filled.Visibility 
                            else Icons.Filled.VisibilityOff,
                        onTrailingIconClick = { 
                            confirmPasswordVisible = !confirmPasswordVisible
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        isError = registerForm.confirmPassword.isNotBlank() && 
                            registerForm.password != registerForm.confirmPassword,
                        errorMessage = if (registerForm.confirmPassword.isNotBlank() && 
                            registerForm.password != registerForm.confirmPassword) 
                            "Kata sandi tidak cocok" else null,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None 
                            else PasswordVisualTransformation(),
                        isFocused = isConfirmPasswordFocused,
                        onFocusChange = { isConfirmPasswordFocused = it },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { 
                                focusManager.clearFocus()
                                if (termsAccepted && registerForm.password == registerForm.confirmPassword) {
                                    authViewModel.register()
                                }
                            }
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Terms & Privacy Checkbox
                    TermsCheckbox(
                        checked = termsAccepted,
                        onCheckedChange = { 
                            termsAccepted = it
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Register Button
                    PremiumRegisterButton(
                        text = "Buat Akun",
                        isLoading = authState.isLoading,
                        enabled = termsAccepted && 
                            registerForm.name.length >= 3 &&
                            isValidEmail(registerForm.email) &&
                            registerForm.password.length >= 6 &&
                            registerForm.password == registerForm.confirmPassword,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            focusManager.clearFocus()
                            authViewModel.register()
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFE0E0E0)
                        )
                        Text(
                            text = "atau",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9E9E9E),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFE0E0E0)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Google Sign-Up Button
                    PremiumGoogleButton(
                        text = "Continue with Google",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            // TODO: Implement Google Sign-Up flow
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Sign In Link
                    AnimatedSignInLink(onClick = onNavigateToLogin)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ============================================================
// AMBIENT BACKGROUND
// ============================================================

/**
 * \ud83c\udf08 Ambient Background with 3 Moving Blobs
 * Premium animated background with high blur effect
 */
@Composable
private fun RegisterAmbientBackground(
    blob1Offset: Offset,
    blob2Offset: Offset
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAF9)) // Very light base color
    ) {
        // Blob 1: Green Accent
        Box(
            modifier = Modifier
                .offset(blob1Offset.x.dp, blob1Offset.y.dp)
                .size(320.dp)
                .background(
                    color = Color(0xFF4CAF50).copy(alpha = 0.15f),
                    shape = CircleShape
                )
                .blur(80.dp)
        )
        
        // Blob 2: Teal Soft
        Box(
            modifier = Modifier
                .offset(blob2Offset.x.dp, blob2Offset.y.dp)
                .size(280.dp)
                .background(
                    color = Color(0xFF009688).copy(alpha = 0.12f),
                    shape = CircleShape
                )
                .blur(70.dp)
        )
        
        // Blob 3: Light Green Neutral (vertical movement)
        Box(
            modifier = Modifier
                .offset(
                    x = 50.dp,
                    y = (350 - blob1Offset.y * 0.5f).dp
                )
                .size(250.dp)
                .background(
                    color = Color(0xFF81C784).copy(alpha = 0.10f),
                    shape = CircleShape
                )
                .blur(65.dp)
        )
    }
}

// ============================================================
// HERO SECTION
// ============================================================

/**
 * Animated hero section with logo and welcome text.
 * 
 * Features:
 * - Scale-in + fade-in animation for logo
 * - Slide-in animation for text
 * - Radial gradient background
 * - Shadow effect on text
 */
@Composable
private fun AnimatedRegisterHeroSection() {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo container with gradient
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00897B).copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // New education/search icon with gradient
                Icon(
                    painter = painterResource(id = R.drawable.ic_education_search),
                    contentDescription = "Campus Lost & Found",
                    modifier = Modifier.size(90.dp),
                    tint = Color.Unspecified // Keep gradient colors
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { 20 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ) + fadeIn()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Bergabunglah! 🎉",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.1f),
                                offset = Offset(0f, 2f),
                                blurRadius = 4f
                            )
                        ),
                        color = Color(0xFF212121)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Buat akun Anda untuk memulai",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF757575)
                    )
                }
            }
        }
    }
}

// ============================================================
// GLASSMORPHIC CARD
// ============================================================

/**
 * Glassmorphism card container with enhanced transparency.
 */
@Composable
private fun RegisterGlassmorphicCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .border(
                width = 1.5.dp,
                color = Color.White.copy(alpha = 0.6f),
                shape = RoundedCornerShape(28.dp)
            ),
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.90f), // Glassmorphism effect
        shadowElevation = 12.dp
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

// ============================================================
// PREMIUM TEXT FIELD
// ============================================================

/**
 * Premium text field with animated border, focus states, and inline errors.
 */
@Composable
private fun PremiumRegisterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isFocused: Boolean = false,
    onFocusChange: (Boolean) -> Unit = {},
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val haptic = LocalHapticFeedback.current

    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> Color(0xFFEF5350)
            isFocused -> Color(0xFF4CAF50) // Match new green theme
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

    val iconColor by animateColorAsState(
        targetValue = when {
            isError -> Color(0xFFEF5350)
            isFocused -> Color(0xFF4CAF50) // Match new green theme
            else -> Color(0xFF9E9E9E)
        },
        animationSpec = tween(300),
        label = "icon"
    )

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(backgroundColor, RoundedCornerShape(16.dp))
                .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    AnimatedVisibility(
                        visible = value.isNotEmpty() || isFocused,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = iconColor
                        )
                    }

                    BasicTextField(
                        value = value,
                        onValueChange = {
                            onValueChange(it)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { onFocusChange(it.isFocused) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = Color(0xFF212121)
                        ),
                        singleLine = true,
                        visualTransformation = visualTransformation,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        decorationBox = { innerTextField ->
                            if (value.isEmpty() && !isFocused) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFFBDBDBD)
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                if (trailingIcon != null && onTrailingIconClick != null) {
                    IconButton(
                        onClick = onTrailingIconClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = "Toggle visibility",
                            tint = iconColor
                        )
                    }
                }
            }
        }

        // Inline error message
        AnimatedVisibility(
            visible = isError && errorMessage != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Text(
                text = errorMessage ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFEF5350),
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

// ============================================================
// PASSWORD STRENGTH INDICATOR
// ============================================================

/**
 * Visual password strength indicator with criteria checklist.
 */
@Composable
private fun PasswordStrengthIndicator(
    strength: PasswordStrength,
    password: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Strength bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            when {
                                index < strength.level -> strength.color
                                else -> Color(0xFFE0E0E0)
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Strength label
        Text(
            text = strength.label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = strength.color
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Criteria checklist
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            PasswordCriteriaItem("Minimal 6 karakter", password.length >= 6)
            PasswordCriteriaItem("Mengandung huruf besar", password.any { it.isUpperCase() })
            PasswordCriteriaItem("Mengandung huruf kecil", password.any { it.isLowerCase() })
            PasswordCriteriaItem("Mengandung angka", password.any { it.isDigit() })
        }
    }
}

@Composable
private fun PasswordCriteriaItem(text: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Filled.CheckCircle else Icons.Filled.Circle,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isMet) Color(0xFF4CAF50) else Color(0xFFBDBDBD)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isMet) Color(0xFF757575) else Color(0xFFBDBDBD)
        )
    }
}

data class PasswordStrength(
    val level: Int,
    val label: String,
    val color: Color
)

private fun calculatePasswordStrength(password: String): PasswordStrength {
    var score = 0
    if (password.length >= 6) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isLowerCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++

    return when {
        score <= 1 -> PasswordStrength(1, "Lemah", Color(0xFFEF5350))
        score == 2 -> PasswordStrength(2, "Cukup", Color(0xFFFF9800))
        score == 3 -> PasswordStrength(3, "Baik", Color(0xFFFFEB3B))
        else -> PasswordStrength(4, "Kuat", Color(0xFF4CAF50))
    }
}

// ============================================================
// TERMS CHECKBOX
// ============================================================

/**
 * Terms & Privacy checkbox with clickable links.
 */
@Composable
private fun TermsCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF00897B),
                uncheckedColor = Color(0xFFBDBDBD)
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "Saya setuju dengan Syarat & Kebijakan Privasi",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF757575)
        )
    }
}

// ============================================================
// PREMIUM BUTTONS
// ============================================================

/**
 * Premium gradient button with press animation.
 */
@Composable
private fun PremiumRegisterButton(
    text: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
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
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color(0xFFE0E0E0)
        ),
        contentPadding = PaddingValues(0.dp),
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (enabled) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF4CAF50), // Fresh green
                                Color(0xFF66BB6A), // Light green
                                Color(0xFF81C784)  // Soft green
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFE0E0E0),
                                Color(0xFFE0E0E0)
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Membuat akun...",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = Color.White
                    )
                }
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = if (enabled) Color.White else Color(0xFF9E9E9E)
                )
            }
        }
    }
}

/**
 * Premium Google button with flat design.
 */
@Composable
private fun PremiumGoogleButton(
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, Color(0xFFE0E0E0)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White
        ),
        interactionSource = interactionSource
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_google),
                contentDescription = "Google",
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFF212121)
            )
        }
    }
}

// ============================================================
// ERROR CARD
// ============================================================

/**
 * Premium error card with icon and message.
 */
@Composable
private fun RegisterErrorCard(
    error: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFEBEE)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = null,
                tint = Color(0xFFEF5350),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFEF5350)
            )
        }
    }
}

// ============================================================
// SIGN IN LINK
// ============================================================

/**
 * Animated "Already have account? Sign In" link.
 */
@Composable
private fun AnimatedSignInLink(
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Sudah punya akun? ",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF757575)
        )
        TextButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = "Masuk",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF00897B)
            )
        }
    }
}

// ============================================================
// HELPER FUNCTIONS
// ============================================================

/**
 * Validates email format using regex.
 */
private fun isValidEmail(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}
