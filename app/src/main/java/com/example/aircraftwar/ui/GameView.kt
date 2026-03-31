package com.example.aircraftwar.ui

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.graphics.withClip
import com.example.aircraftwar.engine.GameEngine
import com.example.aircraftwar.entity.EntityType
import kotlin.math.min

data class Renderable(
    val id: Int,
    val type: EntityType,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
)

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {
    
    private val engine = GameEngine()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        isFilterBitmap = true
    }
    private val viewport = WorldViewport()
    private val sprites = SpriteRepository(context)
    
    @Volatile
    private var running = false
    private var renderThread: Thread? = null
    
    private var backgroundOffsetPx = 0f
    private val backgroundScrollSpeedPxPerSec = 120f
    
    init {
        holder.addCallback(this)
        isFocusable = true
    }
    
    override fun run() {
        var lastNs = System.nanoTime()
        while (running) {
            val now = System.nanoTime()
            val dt = ((now - lastNs) / 1_000_000_000f).coerceAtMost(0.033f)
            lastNs = now
            
            engine.tick(dt)
            updateBackground(dt)
            drawFrame(engine.capture())
        }
    }
    
    private fun updateBackground(dt: Float) {
        viewport.update(width, height)
        val contentHeight = viewport.contentRect().height()
        if (contentHeight <= 0f) return
        
        backgroundOffsetPx += backgroundScrollSpeedPxPerSec * dt
        backgroundOffsetPx %= contentHeight
    }
    
    private fun drawFrame(states: List<Renderable>) {
        val canvas: Canvas = holder.lockCanvas() ?: return
        try {
            canvas.drawColor(Color.BLACK)
            
            viewport.update(width, height)
            val contentRect = viewport.contentRect()
            
            drawScrollingBackground(canvas, contentRect)
            
            canvas.withClip(contentRect) {
                states.forEach { state ->
                    val bitmap = state.type.bitmap
                    val renderScale = state.type.renderScale
                    
                    val drawWidth = state.width * renderScale
                    val drawHeight = state.height * renderScale
                    
                    val targetRect = viewport.worldRectToScreen(
                        centerX = state.x,
                        centerY = state.y,
                        width = drawWidth,
                        height = drawHeight
                    )
                    
                    drawBitmapAspectFit(
                        canvas = canvas,
                        bitmap = bitmap,
                        targetRect = targetRect,
                        paint = paint
                    )
                }
                
            }
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }
    
    private fun drawScrollingBackground(canvas: Canvas, contentRect: RectF) {
        val bg = sprites.background
        val contentHeight = contentRect.height()
        if (contentHeight <= 0f) return
        
        canvas.withClip(contentRect) {
            val offset = backgroundOffsetPx % contentHeight
            
            val firstRect = RectF(
                contentRect.left,
                contentRect.top + offset - contentHeight,
                contentRect.right,
                contentRect.top + offset
            )
            val secondRect = RectF(
                contentRect.left,
                contentRect.top + offset,
                contentRect.right,
                contentRect.top + offset + contentHeight
            )
            
            canvas.drawBitmap(bg, null, firstRect, paint)
            canvas.drawBitmap(bg, null, secondRect, paint)
            
        }
    }
    
    val EntityType.bitmap
        get() = when (this) {
            EntityType.HERO         -> sprites.hero
            EntityType.MOB_ENEMY    -> sprites.mob
            EntityType.ELITE_ENEMY  -> sprites.elite
            EntityType.SUPER_ENEMY  -> sprites.superElite
            EntityType.BOSS_ENEMY   -> sprites.boss
            EntityType.HERO_BULLET  -> sprites.heroBullet
            EntityType.ENEMY_BULLET -> sprites.enemyBullet
            EntityType.HEALTH_PROP  -> sprites.healthProp
            EntityType.BOMB_PROP    -> sprites.bombProp
            EntityType.BULLET_PROP  -> sprites.bulletProp
            EntityType.SUPER_BULLET_PROP -> sprites.superBulletProp
        }
    
    val EntityType.renderScale
        get() = when (this) {
            EntityType.HERO         -> 1.8f
            EntityType.MOB_ENEMY    -> 1.7f
            EntityType.ELITE_ENEMY  -> 1.9f
            EntityType.SUPER_ENEMY  -> 2.0f
            EntityType.BOSS_ENEMY   -> 2.8f
            EntityType.HERO_BULLET  -> 1.2f
            EntityType.ENEMY_BULLET -> 1.2f
            EntityType.HEALTH_PROP  -> 2.2f
            EntityType.BOMB_PROP    -> 2.2f
            EntityType.BULLET_PROP  -> 2.2f
            EntityType.SUPER_BULLET_PROP -> 2.2f
        }
    
    private fun drawBitmapAspectFit(
        canvas: Canvas,
        bitmap: Bitmap,
        targetRect: RectF,
        paint: Paint?
    ) {
        val srcW = bitmap.width.toFloat()
        val srcH = bitmap.height.toFloat()
        if (srcW <= 0f || srcH <= 0f) return
        
        val dstW = targetRect.width()
        val dstH = targetRect.height()
        if (dstW <= 0f || dstH <= 0f) return
        
        val scale = min(dstW / srcW, dstH / srcH)
        val drawW = srcW * scale
        val drawH = srcH * scale
        
        val left = targetRect.centerX() - drawW / 2f
        val top = targetRect.centerY() - drawH / 2f
        val drawRect = RectF(left, top, left + drawW, top + drawH)
        
        canvas.drawBitmap(bitmap, null, drawRect, paint)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_DOWN) {
            viewport.update(width, height)
            
            if (!viewport.containsScreenPoint(event.x, event.y)) {
                return true
            }
            
            val worldX = viewport.screenToWorldX(event.x)
            val worldY = viewport.screenToWorldY(event.y)
            engine.heroTarget(worldX, worldY)
            return true
        }
        return super.onTouchEvent(event)
    }
    
    override fun surfaceCreated(holder: SurfaceHolder) {
        viewport.update(width, height)
        backgroundOffsetPx = 0f
        running = true
        renderThread = Thread(this, "game-loop").also { it.start() }
    }
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        viewport.update(width, height)
    }
    
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        renderThread?.join(500)
    }
}
