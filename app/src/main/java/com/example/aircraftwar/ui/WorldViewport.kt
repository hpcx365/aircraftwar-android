package com.example.aircraftwar.ui

import android.graphics.RectF
import com.example.aircraftwar.engine.GameConstants
import kotlin.math.min

data class WorldViewport(
    val worldWidth: Float = GameConstants.WORLD_WIDTH,
    val worldHeight: Float = GameConstants.WORLD_HEIGHT
) {
    
    var scale: Float = 1f
        private set
    
    var offsetX: Float = 0f
        private set
    
    var offsetY: Float = 0f
        private set
    
    var screenWidth: Int = 1
        private set
    
    var screenHeight: Int = 1
        private set
    
    fun update(screenWidth: Int, screenHeight: Int) {
        this.screenWidth = screenWidth.coerceAtLeast(1)
        this.screenHeight = screenHeight.coerceAtLeast(1)
        
        scale = min(
            this.screenWidth / worldWidth,
            this.screenHeight / worldHeight
        )
        
        val contentWidth = worldWidth * scale
        val contentHeight = worldHeight * scale
        
        offsetX = (this.screenWidth - contentWidth) / 2f
        offsetY = (this.screenHeight - contentHeight) / 2f
    }
    
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
    
    fun containsScreenPoint(screenX: Float, screenY: Float): Boolean {
        val left = offsetX
        val right = offsetX + worldWidth * scale
        val top = offsetY
        val bottom = offsetY + worldHeight * scale
        return screenX in left..right && screenY in top..bottom
    }
    
    fun contentRect(): RectF {
        return RectF(
            offsetX,
            offsetY,
            offsetX + worldWidth * scale,
            offsetY + worldHeight * scale
        )
    }
}
