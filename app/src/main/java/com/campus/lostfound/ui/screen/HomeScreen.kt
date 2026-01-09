package com.campus.lostfound.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
// FASE 2 imports (not used in old design)
// import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
// import androidx.compose.foundation.lazy.grid.GridCells
// import androidx.compose.foundation.lazy.grid.items
// import androidx.compose.foundation.lazy.grid.rememberLazyGridState
// import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
// import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
// import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
// FASE 2 component imports (not used in old design, but preserved for future)
// import com.campus.lostfound.ui.components.CategoryFilterChips
// import com.campus.lostfound.ui.components.LocationFilterChips
// import com.campus.lostfound.ui.components.TimeFilterChips
// import com.campus.lostfound.ui.components.ActiveFiltersBadge
// import com.campus.lostfound.ui.components.SortOptionsButton
// import com.campus.lostfound.ui.components.ViewModeToggle
// import com.campus.lostfound.ui.components.ItemGridCard
// import com.campus.lostfound.ui.components.ShimmerGridItemCard
// import com.campus.lostfound.ui.components.ViewMode
// import com.campus.lostfound.ui.components.SortOption
// import com.campus.lostfound.ui.components.TimeFilter
import com.campus.lostfound.ui.components.ShimmerItemCard
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campus.lostfound.data.model.ItemType
import com.campus.lostfound.ui.components.ItemCard
import com.campus.lostfound.ui.viewmodel.HomeViewModel
import com.campus.lostfound.ui.viewmodel.NotificationViewModel
import com.campus.lostfound.ui.theme.LostRed
import com.campus.lostfound.ui.theme.FoundGreen
import com.campus.lostfound.util.WhatsAppUtil
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically

// Quick Stats Card Component
@Composable
fun QuickStatCard(
    icon: ImageVector,
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = color
                )
            }
            
            // Content
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Premium Header Illustration - Custom Canvas Drawing
@Composable
fun HeaderIllustration(
    modifier: Modifier = Modifier,
    tint: Color
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 3.5.dp.toPx()
        
        // Magnifying glass - primary element
        // Glass circle
        drawCircle(
            color = tint,
            radius = size.minDimension * 0.28f,
            center = Offset(size.width * 0.42f, size.height * 0.38f),
            style = Stroke(width = strokeWidth)
        )
        
        // Handle line
        drawLine(
            color = tint,
            start = Offset(size.width * 0.56f, size.height * 0.52f),
            end = Offset(size.width * 0.72f, size.height * 0.68f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        
        // Document/ID card outline (rounded rectangle)
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.12f, size.height * 0.58f),
            size = Size(size.width * 0.28f, size.height * 0.32f),
            cornerRadius = CornerRadius(6.dp.toPx()),
            style = Stroke(width = strokeWidth * 0.9f)
        )
        
        // Document detail lines (inside the card)
        val docLineWidth = strokeWidth * 0.6f
        // Top line (title)
        drawLine(
            color = tint.copy(alpha = tint.alpha * 0.7f),
            start = Offset(size.width * 0.16f, size.height * 0.66f),
            end = Offset(size.width * 0.32f, size.height * 0.66f),
            strokeWidth = docLineWidth,
            cap = StrokeCap.Round
        )
        // Bottom lines (content)
        drawLine(
            color = tint.copy(alpha = tint.alpha * 0.5f),
            start = Offset(size.width * 0.16f, size.height * 0.73f),
            end = Offset(size.width * 0.36f, size.height * 0.73f),
            strokeWidth = docLineWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint.copy(alpha = tint.alpha * 0.5f),
            start = Offset(size.width * 0.16f, size.height * 0.78f),
            end = Offset(size.width * 0.30f, size.height * 0.78f),
            strokeWidth = docLineWidth,
            cap = StrokeCap.Round
        )
        
        // Location pin (simplified teardrop shape)
        val pinCenter = Offset(size.width * 0.68f, size.height * 0.22f)
        val pinRadius = size.minDimension * 0.09f
        
        // Pin circle
        drawCircle(
            color = tint,
            radius = pinRadius,
            center = pinCenter,
            style = Stroke(width = strokeWidth * 0.85f)
        )
        
        // Pin inner dot
        drawCircle(
            color = tint.copy(alpha = tint.alpha * 0.8f),
            radius = pinRadius * 0.35f,
            center = pinCenter
        )
        
        // Floating decorative dots (particles)
        val dots = listOf(
            Offset(size.width * 0.08f, size.height * 0.18f) to 2.5.dp.toPx(),
            Offset(size.width * 0.88f, size.height * 0.48f) to 3.dp.toPx(),
            Offset(size.width * 0.22f, size.height * 0.88f) to 2.dp.toPx(),
            Offset(size.width * 0.75f, size.height * 0.85f) to 2.2.dp.toPx(),
            Offset(size.width * 0.92f, size.height * 0.25f) to 1.8.dp.toPx()
        )
        
        dots.forEach { (offset, radius) ->
            drawCircle(
                color = tint.copy(alpha = tint.alpha * 0.4f),
                radius = radius,
                center = offset
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAdd: () -> Unit,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToDetail: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(context) as T
            }
        }
    )
    
    HomeScreenContent(
        context = context,
        viewModel = viewModel,
        onNavigateToAdd = onNavigateToAdd,
        onNavigateToNotifications = onNavigateToNotifications,
        onNavigateToDetail = onNavigateToDetail
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    context: android.content.Context,
    viewModel: HomeViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToDetail: ((String) -> Unit)? = null
) {
    val items by viewModel.filteredItems.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    // FASE 2 states (preserved in backend, not used in UI)
    // val selectedCategories by viewModel.selectedCategories.collectAsStateWithLifecycle()
    // val selectedLocations by viewModel.selectedLocations.collectAsStateWithLifecycle()
    // val selectedTimeFilter by viewModel.selectedTimeFilter.collectAsStateWithLifecycle()
    // val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    // val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    // val activeFiltersCount by viewModel.activeFiltersCount.collectAsStateWithLifecycle()
    // val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    
    // Notification ViewModel untuk unread count (local storage)
    val notificationViewModel: NotificationViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NotificationViewModel(context) as T
            }
        }
    )
    val notifications by notificationViewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount = notifications.count { !it.read }
    val hasUnreadNotifications = unreadCount > 0
    
    // States for UI (Old Design - Simple)
    val listState = rememberLazyListState()
    // val gridState = rememberLazyGridState() // FASE 2 - Not used
    // val pullToRefreshState = rememberPullToRefreshState() // FASE 2 - Not used
    
    val headerCollapsed by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 50 }
    }
    
    // Scroll offset for parallax effect
    val scrollOffset by remember {
        derivedStateOf { listState.firstVisibleItemScrollOffset.toFloat() }
    }
    
    // Floating animation for illustration
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
        // Premium Header with Custom Illustration
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.0f)
                        ),
                        startY = 0f,
                        endY = 280f
                    )
                )
        ) {
            // Custom Illustration Layer (behind text)
            HeaderIllustration(
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 30.dp, y = (-5).dp)
                    .graphicsLayer {
                        rotationZ = 12f
                        translationY = floatOffset + (scrollOffset * 0.2f)
                        alpha = (1f - (scrollOffset / 300f)).coerceIn(0.3f, 1f)
                    },
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
            )
            
            // Content Layer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Lost & Found Kampus",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        letterSpacing = (-0.3).sp
                    )
                    // Dynamic subtitle based on item count
                    val subtitle = when {
                        items.isEmpty() -> "Belum ada laporan"
                        items.size == 1 -> "1 laporan terbaru"
                        else -> "${items.size} laporan terbaru"
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                
                // Notification Icon - theme-aware
                Box {
                    val bellInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    val bellPressed by bellInteraction.collectIsPressedAsState()
                    val bellScale by animateFloatAsState(
                        targetValue = if (bellPressed) 0.92f else 1f,
                        animationSpec = tween(100),
                        label = "bellScale"
                    )

                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .scale(bellScale),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                        onClick = { onNavigateToNotifications() },
                        interactionSource = bellInteraction
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Notifications,
                                contentDescription = "Notifikasi",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Notification badge
                    if (hasUnreadNotifications) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp),
                            shape = CircleShape,
                            color = LostRed,
                            shadowElevation = 2.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Simple Filter (Old Design - Tanpa FASE 2 Components)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { viewModel.setFilter(null) },
                label = { Text("Semua") }
            )
            FilterChip(
                selected = selectedFilter == ItemType.LOST,
                onClick = { viewModel.setFilter(ItemType.LOST) },
                label = { Text("Hilang") }
            )
            FilterChip(
                selected = selectedFilter == ItemType.FOUND,
                onClick = { viewModel.setFilter(ItemType.FOUND) },
                label = { Text("Ditemukan") }
            )
        }
        
        // Items List (Old Design - Simple)
        if (isLoading && items.isEmpty()) {
            // Loading shimmer (simple)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(5) {
                    ShimmerItemCard()
                }
            }
        } else if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                com.campus.lostfound.ui.components.EmptyStateIllustration.EmptyStateHomeIllustration()
            }
        } else {
            // Simple List View
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp)
            ) {
                itemsIndexed(items) { index, item ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(300, delayMillis = index * 50)) +
                                slideInVertically(
                                    initialOffsetY = { it / 2 },
                                    animationSpec = tween(300, delayMillis = index * 50)
                                )
                    ) {
                        ItemCard(
                            item = item,
                            onContactClick = {
                                WhatsAppUtil.openWhatsApp(
                                    context = context,
                                    phoneNumber = item.whatsappNumber,
                                    itemName = item.itemName,
                                    type = if (item.type == ItemType.LOST) "barang hilang" else "barang ditemukan"
                                )
                            },
                            onCardClick = {
                                onNavigateToDetail?.invoke(item.id)
                            }
                        )
                    }
                }
            }
        }
        } // Closing brace untuk Column
    } // Closing brace untuk Box
} // Closing brace untuk HomeScreenContent