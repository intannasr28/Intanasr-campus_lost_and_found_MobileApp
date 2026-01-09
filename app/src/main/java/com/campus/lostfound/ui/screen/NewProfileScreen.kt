package com.campus.lostfound.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.lostfound.data.repository.UserRepository
import com.campus.lostfound.ui.components.LargeUserAvatar
import com.campus.lostfound.ui.theme.LostRed
import com.campus.lostfound.ui.theme.FoundGreen
import com.campus.lostfound.ui.theme.ThemeColor
import com.campus.lostfound.ui.viewmodel.SettingsViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

/**
 * ProfileScreen
 * Menggantikan SettingsScreen dengan design modern
 * Menampilkan foto profile, nama, statistik kontribusi user
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToChangePassword: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val auth = FirebaseAuth.getInstance()
    val userRepository = remember { UserRepository() }
    val settingsViewModel: SettingsViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Check guest mode
    val settingsRepository = remember { com.campus.lostfound.data.SettingsRepository(context) }
    val isGuestMode by settingsRepository.isGuestModeFlow.collectAsState(initial = false)
    
    var userProfile by remember { mutableStateOf<com.campus.lostfound.data.model.User?>(null) }
    var stats by remember { mutableStateOf(Triple(0, 0, 0)) } // Reported, Found, Helped
    var isLoading by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    
    // Load user profile only if not guest
    LaunchedEffect(isGuestMode) {
        if (!isGuestMode) {
            scope.launch {
                isLoading = true
                val result = userRepository.getCurrentUserProfile()
                result.onSuccess { user ->
                    userProfile = user
                    // Load stats
                    userRepository.getUserStats(user.id).onSuccess {
                        stats = it
                    }
                }
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }
    
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Surface(
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = "Manage your account",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Guest Mode Banner
            if (isGuestMode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Mode Tamu",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Anda sedang menjelajah sebagai tamu. Login untuk mengakses fitur lengkap!",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    settingsRepository.setGuestMode(false)
                                    onLogout() // Navigate to login
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Login Sekarang")
                        }
                    }
                }
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (!isGuestMode) {
                // Profile Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Profile Photo
                        LargeUserAvatar(
                            photoUrl = userProfile?.photoUrl ?: "",
                            name = userProfile?.name ?: "User"
                        )
                        
                        // User Info
                        Text(
                            text = userProfile?.name ?: "User",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = userProfile?.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // NIM & Faculty (if available)
                        if (!userProfile?.nim.isNullOrEmpty()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "NIM: ${userProfile?.nim}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!userProfile?.faculty.isNullOrEmpty()) {
                                    Text(
                                        text = userProfile?.faculty ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Statistics Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                value = stats.first,
                                label = "Reported",
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                value = stats.second,
                                label = "Found",
                                color = FoundGreen,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                value = stats.third,
                                label = "Helped",
                                color = LostRed,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Account Settings Section
                SettingsSection(title = "Pengaturan Akun") {
                    SettingsItem(
                        icon = Icons.Outlined.Edit,
                        title = "Edit Profil",
                        subtitle = "Ubah informasi profil Anda",
                        onClick = onNavigateToEditProfile
                    )
                    
                    // Show change password only for email/password users
                    if (auth.currentUser?.providerData?.any { it.providerId == "password" } == true) {
                        SettingsItem(
                            icon = Icons.Outlined.Lock,
                            title = "Ganti Password",
                            subtitle = "Perbarui kata sandi akun Anda",
                            onClick = onNavigateToChangePassword
                        )
                    }
                    
                    SettingsItem(
                        icon = Icons.Outlined.Palette,
                        title = "Tema Aplikasi",
                        subtitle = "Terang / Gelap / Sistem",
                        onClick = { showThemeDialog = true }
                    )
                    
                    SettingsItem(
                        icon = Icons.Outlined.ColorLens,
                        title = "Warna Tema",
                        subtitle = "Pilih skema warna favorit",
                        onClick = { showColorDialog = true }
                    )
                }
                
                // Notification Section
                SettingsSection(title = "Notifikasi") {
                    val notificationsEnabled = settingsViewModel.uiState.collectAsState().value.notificationsEnabled
                    val soundEnabled = settingsViewModel.uiState.collectAsState().value.soundEnabled
                    
                    SettingsSwitchItem(
                        icon = Icons.Outlined.NotificationsActive,
                        title = "Notifikasi Push",
                        subtitle = "Terima notifikasi laporan baru",
                        checked = notificationsEnabled,
                        onCheckedChange = { settingsViewModel.setNotificationsEnabled(it) }
                    )
                    
                    SettingsSwitchItem(
                        icon = Icons.Outlined.VolumeUp,
                        title = "Suara Notifikasi",
                        subtitle = "Bunyi saat ada notifikasi",
                        checked = soundEnabled,
                        onCheckedChange = { settingsViewModel.setSoundEnabled(it) }
                    )
                }
                
                // About Section
                SettingsSection(title = "Tentang") {
                    SettingsItem(
                        icon = Icons.Outlined.HelpOutline,
                        title = "Bantuan & Dukungan",
                        subtitle = "Dapatkan bantuan dan dukungan",
                        onClick = onNavigateToHelp
                    )
                    
                    SettingsItem(
                        icon = Icons.Outlined.Description,
                        title = "Syarat & Ketentuan",
                        subtitle = "Baca syarat dan kebijakan kami",
                        onClick = onNavigateToTerms
                    )
                }
                
                // Logout Button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable { showLogoutDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = LostRed.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Logout,
                            contentDescription = null,
                            tint = LostRed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Keluar",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = LostRed
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
    
    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    Icons.Filled.Logout,
                    contentDescription = null,
                    tint = LostRed
                )
            },
            title = {
                Text("Keluar dari Akun?")
            },
            text = {
                Text("Anda yakin ingin keluar dari akun Anda?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Keluar", color = LostRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
    
    // Theme Dialog
    if (showThemeDialog) {
        val currentTheme = settingsViewModel.uiState.collectAsState().value.themeMode
        
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            icon = {
                Icon(Icons.Outlined.Palette, contentDescription = null)
            },
            title = {
                Text("Pilih Tema")
            },
            text = {
                Column {
                    ThemeOption(
                        title = "Sistem Default",
                        subtitle = "Mengikuti tema sistem",
                        selected = currentTheme == "system",
                        onClick = {
                            settingsViewModel.setThemeMode("system")
                            showThemeDialog = false
                        }
                    )
                    ThemeOption(
                        title = "Mode Terang",
                        subtitle = "Selalu gunakan tema terang",
                        selected = currentTheme == "light",
                        onClick = {
                            settingsViewModel.setThemeMode("light")
                            showThemeDialog = false
                        }
                    )
                    ThemeOption(
                        title = "Mode Gelap",
                        subtitle = "Selalu gunakan tema gelap",
                        selected = currentTheme == "dark",
                        onClick = {
                            settingsViewModel.setThemeMode("dark")
                            showThemeDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
    
    // Color Theme Selection Dialog
    if (showColorDialog) {
        val currentColor = settingsViewModel.uiState.collectAsState().value.themeColor
        
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            icon = {
                Icon(Icons.Outlined.ColorLens, contentDescription = null)
            },
            title = {
                Text("Pilih Warna Tema")
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    ThemeColor.values().forEach { color ->
                        ColorOption(
                            themeColor = color,
                            selected = currentColor == color,
                            onClick = {
                                settingsViewModel.setThemeColor(color)
                                showColorDialog = false
                            }
                        )
                        if (color != ThemeColor.values().last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
private fun ThemeOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer 
                else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ColorOption(
    themeColor: ThemeColor,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer 
                else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Color preview circles
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Primary color
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(themeColor.primaryLight)
                )
                // Secondary color
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(themeColor.secondaryLight)
                )
            }
            
            // Text info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = themeColor.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
            
            // Radio button
            RadioButton(
                selected = selected,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun StatCard(
    value: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
        
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.padding(8.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.padding(8.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}