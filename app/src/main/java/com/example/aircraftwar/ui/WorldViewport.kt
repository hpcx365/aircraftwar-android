package com.example.aircraftwar.ui

import android.graphics.RectF
import kotlin.math.min

data class WorldViewport(
    val worldWidth: Float,
    val worldHeight: Float,
    val screenWidth: Int,
    val screenHeight: Int,
) {
    
    val contentWidth: Float get() = worldWidth * scale
    val contentHeight: Float get() = worldHeight * scale
    val offsetX: Float get() = 0.5f * (screenWidth - contentWidth)
    val offsetY: Float get() = 0.5f * (screenHeight - contentHeight)
    val scale: Float get() = min(screenWidth / worldWidth, screenHeight / worldHeight)
    val contentRect: RectF
        get() = RectF(
            offsetX,
            offsetY,
            offsetX + worldWidth * scale,
            offsetY + worldHeight * scale
        )
    
    fun worldToScreenX(worldX: Float): Float {
        return offsetX + worldX * scale
    }
    
    fun worldToScreenY(worldY: Float): Float {
        return screenHeight - (offsetY + worldY * scale)
    }
    
    fun screenToWorldX(screenX: Float): Float {
        return ((screenX - offsetX) / scale).coerceIn(0f, worldWidth)
    }
    
    fun screenToWorldY(screenY: Float): Float {
        return ((screenHeight - screenY - offsetY) / scale).coerceIn(0f, worldHeight)
    }
    
    fun worldRectToScreen(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float
    ): RectF {
        val left = worldToScreenX(centerX - width / 2f)
        val right = worldToScreenX(centerX + width / 2f)
        val top = worldToScreenY(centerY + height / 2f)
        val bottom = worldToScreenY(centerY - height / 2f)
        return RectF(left, top, right, bottom)
    }
}
