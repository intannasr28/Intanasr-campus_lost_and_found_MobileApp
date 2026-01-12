package com.campus.lostfound

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.campus.lostfound.data.SettingsRepository
import com.campus.lostfound.navigation.NavigationGraph
import com.campus.lostfound.navigation.Screen
import com.campus.lostfound.ui.components.BottomNavigationBar
import com.campus.lostfound.ui.theme.CampusLostFoundTheme
import com.campus.lostfound.ui.theme.ThemeColor
import com.campus.lostfound.service.LocalNotificationService
import com.campus.lostfound.service.RealtimeNotificationListener
import com.campus.lostfound.service.OneSignalNotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var localNotificationService: LocalNotificationService
    private lateinit var realtimeListener: RealtimeNotificationListener
    private lateinit var oneSignalService: OneSignalNotificationService
    
    // Permission launcher for Android 13+ notification permission
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.w("MainActivity", "Notification permission denied")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ✅ Enable Firestore Offline Persistence for better performance
        try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().apply {
                firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true) // Enable offline cache
                    .setCacheSizeBytes(10 * 1024 * 1024) // 10 MB cache
                    .build()
            }
            Log.d("MainActivity", "Firestore offline persistence enabled")
        } catch (e: Exception) {
            Log.w("MainActivity", "Firestore already initialized", e)
        }
        
        // Request notification permission for Android 13+
        requestNotificationPermission()

        // Handle navigation from push notification
        handleNotificationNavigation(intent)
        
        // Initialize notification services
        localNotificationService = LocalNotificationService(this)
        realtimeListener = RealtimeNotificationListener(this, localNotificationService)
        
        // Initialize OneSignal for real-time push notifications
        initializeOneSignal()

        // Initialize per-device lastSeen on first run so new installs don't see old notifications
        try {
            val prefs = getSharedPreferences("notif_prefs", Context.MODE_PRIVATE)
            if (!prefs.contains("lastSeen")) {
                prefs.edit().putLong("lastSeen", System.currentTimeMillis()).apply()
            }
        } catch (e: Exception) {
            Log.w("MainActivity", "Failed to init lastSeen", e)
        }

        // Subscribe to global topic for broadcast notifications
        FirebaseMessaging.getInstance().subscribeToTopic("all")
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) Log.w("MainActivity", "Topic subscribe failed", task.exception)
            }
        
        // Subscribe to campus reports topic for push notifications
        FirebaseMessaging.getInstance().subscribeToTopic("campus_reports")
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) Log.w("MainActivity", "Campus reports subscribe failed", task.exception)
            }
        
        // Start real-time notification listener
        realtimeListener.startListening()
        
        // Schedule cleanup of old completed items (runs once on app start)
        scheduleCompletedItemsCleanup()
        
        setContent {
            // Collect theme settings
            val context = LocalContext.current
            val settingsRepository = SettingsRepository(context)
            val themeMode by settingsRepository.themeModeFlow.collectAsState(initial = "system")
            val themeColorName by settingsRepository.themeColorFlow.collectAsState(initial = "DEFAULT")
            
            val themeColor = try {
                ThemeColor.valueOf(themeColorName)
            } catch (e: Exception) {
                ThemeColor.DEFAULT
            }
            
            CampusLostFoundTheme(
                themeMode = themeMode,
                themeColor = themeColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleNotificationNavigation(intent)
    }

    private fun handleNotificationNavigation(intent: Intent?) {
        intent?.let {
            val navigateTo = it.getStringExtra("navigate_to")
            val itemId = it.getStringExtra("item_id")
            
            // Store navigation info to be handled by Compose
            val prefs = getSharedPreferences("navigation", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("navigate_to", navigateTo)
                putString("item_id", itemId)
                putBoolean("has_pending_navigation", true)
                apply()
            }
            
            Log.d("MainActivity", "Stored navigation: $navigateTo, itemId: $itemId")
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", "Notification permission already granted")
                }
                else -> {
                    Log.d("MainActivity", "Requesting notification permission...")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::realtimeListener.isInitialized) {
            realtimeListener.cleanup()
        }
        if (::oneSignalService.isInitialized) {
            oneSignalService.cleanup()
        }
    }
    
    private fun initializeOneSignal() {
        try {
            oneSignalService = OneSignalNotificationService(this)
            oneSignalService.initialize()
            Log.d("MainActivity", "✅ OneSignal service initialized")
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Failed to initialize OneSignal", e)
        }
    }
    
    private fun scheduleCompletedItemsCleanup() {
        // Run cleanup in background coroutine
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                // Check if we've run cleanup recently (once per day)
                val prefs = getSharedPreferences("cleanup_prefs", Context.MODE_PRIVATE)
                val lastCleanup = prefs.getLong("last_cleanup", 0)
                val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
                
                if (lastCleanup < oneDayAgo) {
                    val repository = com.campus.lostfound.data.repository.LostFoundRepository(this@MainActivity)
                    val result = repository.cleanupOldCompletedItems()
                    
                    result.onSuccess { deletedCount ->
                        if (deletedCount > 0) {
                            Log.d("MainActivity", "✅ Cleanup: Deleted $deletedCount old completed items")
                        }
                    }.onFailure { error ->
                        Log.w("MainActivity", "Cleanup failed: ${error.message}")
                    }
                    
                    // Update last cleanup time
                    prefs.edit().putLong("last_cleanup", System.currentTimeMillis()).apply()
                }
            } catch (e: Exception) {
                Log.w("MainActivity", "Cleanup task failed", e)
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: Screen.Home.route
    
    // Check authentication and guest mode
    val settingsRepository = remember { SettingsRepository(context) }
    val isGuestMode by settingsRepository.isGuestModeFlow.collectAsState(initial = false)
    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    val isAuthenticated = auth.currentUser != null
    
    // Redirect to login if not authenticated and not in guest mode
    LaunchedEffect(isAuthenticated, isGuestMode) {
        if (!isAuthenticated && !isGuestMode && currentRoute != Screen.Login.route && currentRoute != Screen.Register.route) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
    
    // Handle pending navigation from notification intent
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("navigation", Context.MODE_PRIVATE)
        val hasPending = prefs.getBoolean("has_pending_navigation", false)
        
        if (hasPending) {
            val navigateTo = prefs.getString("navigate_to", null)
            val itemId = prefs.getString("item_id", null)
            
            when (navigateTo) {
                "detail" -> {
                    if (!itemId.isNullOrEmpty()) {
                        navController.navigate(Screen.Detail.createRoute(itemId))
                    }
                }
                "notifications" -> {
                    navController.navigate(Screen.Notifications.route)
                }
                "home" -> {
                    navController.navigate(Screen.Home.route)
                }
            }
            
            // Clear pending navigation
            prefs.edit().clear().apply()
        }
    }
    
    // Only show bottom navigation on main app sections
    val showBottomBar = when {
        currentRoute == Screen.Home.route -> true
        currentRoute == Screen.Add.route -> true
        currentRoute == Screen.Activity.route -> true
        currentRoute == Screen.Settings.route -> true
        currentRoute == Screen.Explore.route -> true
        // detail has parameter like "detail/{itemId}", match prefix
        currentRoute?.startsWith("detail") == true -> false
        currentRoute == Screen.Notifications.route -> false
        else -> false
    }

    androidx.compose.material3.Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavigationGraph(navController = navController)
        }
    }
}
