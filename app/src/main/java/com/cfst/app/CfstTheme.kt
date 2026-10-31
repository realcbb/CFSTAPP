package com.cfst.app

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CfstTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF087F8C), onPrimary = Color.White,
            secondary = Color(0xFF356871),
            primaryContainer = Color(0xFFD5F3EE), onPrimaryContainer = Color(0xFF073E43),
            secondaryContainer = Color(0xFFDFF0F1), onSecondaryContainer = Color(0xFF123D46),
            background = Color(0xFFF2F6F8), surface = Color.White,
            surfaceVariant = Color(0xFFEAF2F5), onSurface = Color(0xFF172F38),
            onSurfaceVariant = Color(0xFF536971), outline = Color(0xFF91A7AE)
        ),
        shapes = Shapes(
            small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp)
        ),
        content = content
    )
}
