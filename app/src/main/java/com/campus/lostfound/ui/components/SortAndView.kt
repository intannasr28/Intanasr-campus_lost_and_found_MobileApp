package com.campus.lostfound.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class SortOption(val displayName: String) {
    NEWEST("Terbaru"),
    OLDEST("Terlama"),
    MOST_RELEVANT("Paling Relevan")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortOptionsButton(
    currentSort: SortOption,
    onSortChange: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                Icons.Default.Sort,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(currentSort.displayName, style = MaterialTheme.typography.labelLarge)
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SortOption.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        onSortChange(option)
                        expanded = false
                    },
                    leadingIcon = {
                        if (option == currentSort) {
                            Icon(Icons.Default.Check, null)
                        }
                    }
                )
            }
        }
    }
}

enum class ViewMode {
    LIST, GRID
}

@Composable
fun ViewModeToggle(
    currentMode: ViewMode,
    onModeChange: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            IconButton(
                onClick = { onModeChange(ViewMode.LIST) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.ViewList,
                    "List view",
                    tint = if (currentMode == ViewMode.LIST) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { onModeChange(ViewMode.GRID) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.GridView,
                    "Grid view",
                    tint = if (currentMode == ViewMode.GRID) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
