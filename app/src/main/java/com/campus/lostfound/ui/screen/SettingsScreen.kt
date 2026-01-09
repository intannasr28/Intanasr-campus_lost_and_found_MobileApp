package com.campus.lostfound.ui.screen

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.lostfound.ui.viewmodel.SettingsViewModel
import com.campus.lostfound.ui.theme.ThemeColor
import com.campus.lostfound.util.WhatsAppUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateToProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    
    // Dialog states
    var showThemeDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showFaqDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Premium Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Pengaturan",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Kelola preferensi dan informasi aplikasi",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            
            // ===== SECTION: ACCOUNT =====
            SettingsSectionHeader(title = "Akun", icon = Icons.Outlined.Person)
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                SettingsClickableItem(
                    icon = Icons.Outlined.AccountCircle,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Profile & Akun",
                    subtitle = "Kelola informasi profile Anda",
                    onClick = onNavigateToProfile
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // ===== SECTION: PREFERENSI =====
            SettingsSectionHeader(title = "Tampilan", icon = Icons.Outlined.Tune)
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                // Tema Mode (Light/Dark/System)
                SettingsClickableItem(
                    icon = Icons.Outlined.DarkMode,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = "Mode Tema",
                    subtitle = when(uiState.themeMode) {
                        "light" -> "Mode Terang"
                        "dark" -> "Mode Gelap"
                        else -> "Ikuti Sistem"
                    },
                    onClick = { showThemeDialog = true }
                )
                
                SettingsDivider()
                
                // Warna Tema (Color Palette)
                SettingsColorItem(
                    icon = Icons.Outlined.Palette,
                    title = "Warna Tema",
                    subtitle = uiState.themeColor.displayName,
                    currentColor = uiState.themeColor,
                    onClick = { showColorDialog = true }
                )
                
                SettingsDivider()
                
                // Bahasa
                SettingsInfoItem(
                    icon = Icons.Outlined.Language,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    title = "Bahasa",
                    subtitle = "Indonesia"
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // ===== SECTION: NOTIFIKASI =====
            SettingsSectionHeader(title = "Notifikasi", icon = Icons.Outlined.Notifications)
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                SettingsSwitchItem(
                    icon = Icons.Outlined.NotificationsActive,
                    iconTint = Color(0xFF00C853),
                    title = "Notifikasi Push",
                    subtitle = "Laporan baru, update status",
                    checked = uiState.notificationsEnabled,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                )
                
                SettingsDivider()
                
                SettingsSwitchItem(
                    icon = Icons.Outlined.VolumeUp,
                    iconTint = Color(0xFF2196F3),
                    title = "Suara Notifikasi",
                    subtitle = "Bunyi saat ada notifikasi",
                    checked = uiState.soundEnabled,
                    onCheckedChange = { viewModel.setSoundEnabled(it) }
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // ===== SECTION: BANTUAN =====
            SettingsSectionHeader(title = "Bantuan & Dukungan", icon = Icons.Outlined.HelpOutline)
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                SettingsClickableItem(
                    icon = Icons.Outlined.QuestionAnswer,
                    iconTint = Color(0xFF9C27B0),
                    title = "Pertanyaan Umum (FAQ)",
                    subtitle = "Jawaban pertanyaan sering ditanyakan",
                    onClick = { showFaqDialog = true }
                )
                
                SettingsDivider()
                
                SettingsClickableItem(
                    icon = Icons.Outlined.Feedback,
                    iconTint = Color(0xFFFF9800),
                    title = "Kirim Masukan",
                    subtitle = "Saran dan kritik pengembangan",
                    onClick = { showFeedbackDialog = true }
                )
                
                SettingsDivider()
                
                SettingsClickableItem(
                    icon = Icons.Outlined.BugReport,
                    iconTint = Color(0xFFF44336),
                    title = "Laporkan Masalah",
                    subtitle = "Temukan bug? Beritahu kami via WhatsApp",
                    onClick = {
                        WhatsAppUtil.openBugReportWhatsApp(context)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // ===== SECTION: PRIVASI =====
            SettingsSectionHeader(title = "Privasi & Keamanan", icon = Icons.Outlined.Security)
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                SettingsInfoCard(
                    icon = Icons.Outlined.PrivacyTip,
                    iconTint = Color(0xFF4CAF50),
                    title = "Data Anda Aman",
                    description = "Data bersifat anonim. Nomor WhatsApp hanya untuk komunikasi terkait laporan."
                )
                
                SettingsDivider()
                
                SettingsClickableItem(
                    icon = Icons.Outlined.Policy,
                    iconTint = Color(0xFF607D8B),
                    title = "Kebijakan Privasi",
                    subtitle = "Baca kebijakan privasi lengkap",
                    onClick = { }
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // ===== SECTION: TENTANG =====
            SettingsSectionHeader(title = "Tentang Aplikasi", icon = Icons.Outlined.Info)
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                // App Info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Campus Lost & Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Versi 1.0.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Aplikasi untuk membantu mahasiswa\nmelaporkan dan menemukan barang hilang",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                
                SettingsDivider()
                
                SettingsClickableItem(
                    icon = Icons.Outlined.Star,
                    iconTint = Color(0xFFFFB300),
                    title = "Beri Rating",
                    subtitle = "Bantu kami dengan review Anda",
                    onClick = { }
                )
                
                SettingsDivider()
                
                SettingsClickableItem(
                    icon = Icons.Outlined.Share,
                    iconTint = Color(0xFF03A9F4),
                    title = "Bagikan Aplikasi",
                    subtitle = "Rekomendasikan ke teman",
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Campus Lost & Found")
                            putExtra(Intent.EXTRA_TEXT, "Coba aplikasi Campus Lost & Found untuk barang hilang di kampus!")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Bagikan via"))
                    }
                )
            }
            
            // Footer
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Dibuat dengan ❤️ untuk mahasiswa kampus",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "© 2025 Campus Lost & Found",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
    
    // ===== DIALOGS =====
    
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            icon = { Icon(Icons.Outlined.DarkMode, contentDescription = null) },
            title = { Text("Mode Tema", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    ThemeOption("Ikuti Sistem", "Otomatis sesuai perangkat", uiState.themeMode == "system") {
                        viewModel.setThemeMode("system")
                        showThemeDialog = false
                    }
                    ThemeOption("Mode Terang", "Tampilan terang sepanjang waktu", uiState.themeMode == "light") {
                        viewModel.setThemeMode("light")
                        showThemeDialog = false
                    }
                    ThemeOption("Mode Gelap", "Tampilan gelap untuk mata nyaman", uiState.themeMode == "dark") {
                        viewModel.setThemeMode("dark")
                        showThemeDialog = false
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text("Tutup") } }
        )
    }
    
    // Color Palette Dialog
    if (showColorDialog) {
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            title = { 
                Text(
                    "Pilih Warna Tema", 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                ) 
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Pilih warna aksen untuk aplikasi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Color Grid - 2 columns
                    val colors = ThemeColor.entries.toList()
                    colors.chunked(2).forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowColors.forEach { color ->
                                ColorPaletteItem(
                                    themeColor = color,
                                    isSelected = uiState.themeColor == color,
                                    isDarkMode = uiState.themeMode == "dark",
                                    onClick = {
                                        viewModel.setThemeColor(color)
                                        showColorDialog = false
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Fill empty space if odd number
                            if (rowColors.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
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
    
    if (showFaqDialog) {
        AlertDialog(
            onDismissRequest = { showFaqDialog = false },
            icon = { Icon(Icons.Outlined.QuestionAnswer, contentDescription = null) },
            title = { Text("Pertanyaan Umum", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FaqItem("Bagaimana cara melaporkan barang hilang?", "Tekan tombol + di navigasi bawah, pilih 'Barang Hilang', isi detail dan kirim.")
                    FaqItem("Bagaimana jika menemukan barang orang lain?", "Tekan tombol + dan pilih 'Barang Ditemukan'. Isi detail agar pemilik bisa mengenali.")
                    FaqItem("Apakah data saya aman?", "Ya! Data bersifat anonim. WhatsApp hanya untuk komunikasi langsung.")
                    FaqItem("Bagaimana menandai barang sudah ditemukan?", "Buka Aktivitas, temukan laporan Anda, tekan 'Selesai'.")
                }
            },
            confirmButton = { TextButton(onClick = { showFaqDialog = false }) { Text("Mengerti") } }
        )
    }
    
    if (showFeedbackDialog) {
        var feedbackText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            icon = { Icon(Icons.Outlined.Feedback, contentDescription = null) },
            title = { Text("Kirim Masukan", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Kami menghargai masukan Anda untuk pengembangan aplikasi.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        placeholder = { Text("Tulis masukan di sini...") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:feedback@campuslostfound.id")
                            putExtra(Intent.EXTRA_SUBJECT, "Masukan - Campus Lost & Found")
                            putExtra(Intent.EXTRA_TEXT, feedbackText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Kirim Masukan"))
                        showFeedbackDialog = false
                    },
                    enabled = feedbackText.isNotBlank()
                ) { Text("Kirim") }
            },
            dismissButton = { TextButton(onClick = { showFeedbackDialog = false }) { Text("Batal") } }
        )
    }
}

// ===== HELPER COMPOSABLES =====

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun SettingsClickableItem(icon: ImageVector, iconTint: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(iconTint.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SettingsSwitchItem(icon: ImageVector, iconTint: Color, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(iconTint.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsInfoItem(icon: ImageVector, iconTint: Color, title: String, subtitle: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(iconTint.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsInfoCard(icon: ImageVector, iconTint: Color, title: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(iconTint.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun ThemeOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    Column {
        Text(question, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(4.dp))
        Text(answer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
    }
}

// Color Palette Item for Theme Settings
@Composable
private fun SettingsColorItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    currentColor: ThemeColor,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color preview circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(currentColor.primaryLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, 
                null, 
                tint = Color.White, 
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title, 
                style = MaterialTheme.typography.bodyLarge, 
                fontWeight = FontWeight.SemiBold, 
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle, 
                style = MaterialTheme.typography.bodySmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Color preview dots
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(currentColor.primaryLight)
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(currentColor.secondaryLight)
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(currentColor.tertiaryLight)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Icon(
            Icons.Filled.ChevronRight, 
            null, 
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
            modifier = Modifier.size(20.dp)
        )
    }
}

// Color Palette Item in Dialog
@Composable
private fun ColorPaletteItem(
    themeColor: ThemeColor,
    isSelected: Boolean,
    isDarkMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = if (isDarkMode) themeColor.primaryDark else themeColor.primaryLight
    val secondaryColor = if (isDarkMode) themeColor.secondaryDark else themeColor.secondaryLight
    val tertiaryColor = if (isDarkMode) themeColor.tertiaryDark else themeColor.tertiaryLight
    
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                primaryColor.copy(alpha = 0.12f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (isSelected) 
            androidx.compose.foundation.BorderStroke(2.dp, primaryColor)
        else 
            null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Color preview circles
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary color - larger
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                )
                // Secondary & Tertiary - smaller
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(secondaryColor)
                    )
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(tertiaryColor)
                    )
                }
            }
            
            // Color name
            Text(
                text = themeColor.displayName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            
            // Selected indicator
            if (isSelected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = primaryColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}


