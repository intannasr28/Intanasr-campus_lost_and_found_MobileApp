package com.campus.lostfound.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.campus.lostfound.data.model.Category

object CategoryIcons {
    fun getCategoryIcon(category: Category): ImageVector {
        return when (category) {
            Category.ELECTRONICS -> Icons.Outlined.Smartphone
            Category.DOCUMENTS -> Icons.Outlined.Description
            Category.KEYS_ACCESSORIES -> Icons.Outlined.VpnKey
            Category.BAGS_WALLETS -> Icons.Outlined.WorkOutline
            Category.BOOKS_STATIONERY -> Icons.Outlined.MenuBook
            Category.OTHER -> Icons.Outlined.Category
        }
    }
    
    fun getLocationIcon(): ImageVector {
        return Icons.Outlined.LocationOn
    }
}
