package com.example.ui.util

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/**
 * Common responsiveness logic for all modals in the application.
 */
object ResponsiveModal {

    /**
     * Standard DialogProperties to allow custom widths across different screen sizes.
     */
    val dialogProperties = DialogProperties(
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = true
    )

    /**
     * Applies standard width and height constraints to modal content.
     * - Mobile: ~95% width
     * - Desktop: Max 600dp
     * - Max Height: 90% of screen height
     */
    @Composable
    fun modalModifier(isSmall: Boolean = false): Modifier {
        val configuration = LocalConfiguration.current
        val screenHeight = configuration.screenHeightDp.dp
        
        val maxWidth = if (isSmall) 400.dp else 600.dp
        
        return Modifier
            .fillMaxWidth(0.95f) // 95% on mobile
            .widthIn(max = maxWidth) // Max width for tablet/desktop
            .heightIn(max = screenHeight * 0.9f) // Max height 90dvh
    }
}
