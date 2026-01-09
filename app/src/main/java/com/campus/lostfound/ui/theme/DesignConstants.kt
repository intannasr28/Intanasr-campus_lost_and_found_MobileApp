package com.campus.lostfound.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object DesignConstants {
    // Border Radius - Konsistensi (12-20dp range)
    val CardRadius = 16.dp
    val ButtonRadius = 12.dp
    val ChipRadius = 20.dp
    val InputRadius = 12.dp
    val SmallRadius = 8.dp
    val LargeRadius = 20.dp
    val ExtraLargeRadius = 24.dp
    
    // Elevation - Halus (2-6dp)
    val CardElevation = 2.dp
    val CardElevationPressed = 4.dp
    val ButtonElevation = 4.dp
    val SearchBarElevation = 2.dp
    val NavigationElevation = 6.dp
    
    // Spacing - Breathing room
    val SpacingXSmall = 4.dp
    val SpacingSmall = 8.dp
    val SpacingMedium = 12.dp
    val SpacingLarge = 16.dp
    val SpacingXLarge = 24.dp
    val SpacingXXLarge = 32.dp
    
    // Icon Sizes
    val IconSizeSmall = 16.dp
    val IconSizeMedium = 24.dp
    val IconSizeLarge = 48.dp
    val IconSizeXLarge = 64.dp
    
    // Shapes - Material 3 compatible
    val CardShape = RoundedCornerShape(CardRadius)
    val ButtonShape = RoundedCornerShape(ButtonRadius)
    val ChipShape = RoundedCornerShape(ChipRadius)
    val InputShape = RoundedCornerShape(InputRadius)
    val SmallShape = RoundedCornerShape(SmallRadius)
    val LargeShape = RoundedCornerShape(LargeRadius)
    val ExtraLargeShape = RoundedCornerShape(ExtraLargeRadius)
    
    // Material 3 Shapes
    val Shapes = Shapes(
        extraSmall = SmallShape,
        small = InputShape,
        medium = ButtonShape,
        large = CardShape,
        extraLarge = ExtraLargeShape
    )
}

