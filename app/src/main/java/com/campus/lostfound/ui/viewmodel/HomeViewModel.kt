package com.campus.lostfound.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campus.lostfound.data.model.Category
import com.campus.lostfound.data.model.ItemType
import com.campus.lostfound.data.model.LostFoundItem
import com.campus.lostfound.data.repository.LostFoundRepository
import com.campus.lostfound.ui.components.SortOption
import com.campus.lostfound.ui.components.TimeFilter
import com.campus.lostfound.ui.components.ViewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class HomeViewModel(
    private val context: Context,
    private val repository: LostFoundRepository = LostFoundRepository(context)
) : ViewModel() {
    
    private val _selectedFilter = MutableStateFlow<ItemType?>(null)
    val selectedFilter: StateFlow<ItemType?> = _selectedFilter.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // New filter states
    private val _selectedCategories = MutableStateFlow<Set<Category>>(emptySet())
    val selectedCategories: StateFlow<Set<Category>> = _selectedCategories.asStateFlow()
    
    private val _selectedLocations = MutableStateFlow<Set<String>>(emptySet())
    val selectedLocations: StateFlow<Set<String>> = _selectedLocations.asStateFlow()
    
    private val _selectedTimeFilter = MutableStateFlow(TimeFilter.ALL)
    val selectedTimeFilter: StateFlow<TimeFilter> = _selectedTimeFilter.asStateFlow()
    
    private val _sortOption = MutableStateFlow(SortOption.NEWEST)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()
    
    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()
    
    private val _items = MutableStateFlow<List<LostFoundItem>>(emptyList())
    val items: StateFlow<List<LostFoundItem>> = _items.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private var currentJob: kotlinx.coroutines.Job? = null
    
    // Active filters count
    val activeFiltersCount: StateFlow<Int> = combine(
        _selectedCategories,
        _selectedLocations,
        _selectedTimeFilter
    ) { categories, locations, timeFilter ->
        categories.size + locations.size + if (timeFilter != TimeFilter.ALL) 1 else 0
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )
    
    val filteredItems: StateFlow<List<LostFoundItem>> = combine(
        _items,
        _selectedFilter,
        _searchQuery.debounce(300),
        _selectedCategories,
        _selectedLocations,
        _selectedTimeFilter,
        _sortOption
    ) { flows ->
        val items = flows[0] as List<LostFoundItem>
        val filter = flows[1] as ItemType?
        val query = flows[2] as String
        val categories = flows[3] as Set<Category>
        val locations = flows[4] as Set<String>
        val timeFilter = flows[5] as TimeFilter
        val sortOption = flows[6] as SortOption
        var filtered = items
        
        // Filter by type (Lost/Found)
        if (filter != null) {
            filtered = filtered.filter { it.type == filter }
        }
        
        // Filter by categories
        if (categories.isNotEmpty()) {
            filtered = filtered.filter { it.category in categories }
        }
        
        // Filter by locations
        if (locations.isNotEmpty()) {
            filtered = filtered.filter { it.location in locations }
        }
        
        // Filter by time
        if (timeFilter != TimeFilter.ALL) {
            val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(timeFilter.days.toLong())
            filtered = filtered.filter { 
                it.createdAt.seconds * 1000 >= cutoffTime
            }
        }
        
        // Search query
        if (query.isNotBlank()) {
            val lowerQuery = query.lowercase()
            filtered = filtered.filter {
                it.itemName.lowercase().contains(lowerQuery) ||
                it.location.lowercase().contains(lowerQuery) ||
                it.description.lowercase().contains(lowerQuery) ||
                it.category.displayName.lowercase().contains(lowerQuery)
            }
        }
        
        // Sort
        filtered = when (sortOption) {
            SortOption.NEWEST -> filtered.sortedByDescending { it.createdAt }
            SortOption.OLDEST -> filtered.sortedBy { it.createdAt }
            SortOption.MOST_RELEVANT -> {
                // Sort by search relevance if query exists, otherwise by date
                if (query.isNotBlank()) {
                    val lowerQuery = query.lowercase()
                    filtered.sortedByDescending { item ->
                        when {
                            item.itemName.lowercase().startsWith(lowerQuery) -> 3
                            item.itemName.lowercase().contains(lowerQuery) -> 2
                            item.category.displayName.lowercase().contains(lowerQuery) -> 1
                            else -> 0
                        }
                    }
                } else {
                    filtered.sortedByDescending { it.createdAt }
                }
            }
        }
        
        filtered
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    init {
        loadItems()
    }
    
    fun loadItems() {
        // Cancel previous job jika ada
        currentJob?.cancel()
        
        currentJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getAllItems(_selectedFilter.value).collect { itemsList ->
                    _items.value = itemsList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
                e.printStackTrace()
            }
        }
    }
    
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.getAllItems(_selectedFilter.value).collect { itemsList ->
                    _items.value = itemsList
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
    
    fun setFilter(type: ItemType?) {
        _selectedFilter.value = type
        loadItems()
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    // New filter methods
    fun toggleCategory(category: Category) {
        _selectedCategories.value = if (category in _selectedCategories.value) {
            _selectedCategories.value - category
        } else {
            _selectedCategories.value + category
        }
    }
    
    fun toggleLocation(location: String) {
        _selectedLocations.value = if (location in _selectedLocations.value) {
            _selectedLocations.value - location
        } else {
            _selectedLocations.value + location
        }
    }
    
    fun setTimeFilter(timeFilter: TimeFilter) {
        _selectedTimeFilter.value = timeFilter
    }
    
    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }
    
    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }
    
    fun clearAllFilters() {
        _selectedCategories.value = emptySet()
        _selectedLocations.value = emptySet()
        _selectedTimeFilter.value = TimeFilter.ALL
    }
}
