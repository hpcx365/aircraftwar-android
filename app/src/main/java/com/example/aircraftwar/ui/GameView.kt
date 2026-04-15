package com.example.aircraftwar.ui

import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.Choreographer
import android.view.Choreographer.FrameCallback
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.graphics.withClip
import com.example.aircraftwar.engine.*
import com.example.aircraftwar.transfer.ObjectTransferService
import kotlin.math.max

class GameView(
    context: Context,
    private val isHost: Boolean,
    private val transferService: ObjectTransferService,
    private val onGameOver: (FrameSnapshot) -> Unit = {},
) : SurfaceView(context), SurfaceHolder.Callback {
    
    private val controller = PlayerController("local")
    private val sprites = SpriteRepository(context)
    private val audio = GameAudio(context)
    
    @Volatile
    var running = false
        private set
    
    @Volatile
    private var latestSnapshot: FrameSnapshot? = null
    
    private val choreographer = Choreographer.getInstance()
    
    private var backgroundOffsetPx = 0f
    private var deathPromptHandled = false
    private var startButtonVisible = isHost
    
    init {
        holder.addCallback(this)
        isFocusable = true
        isClickable = true
    }
    
    override fun surfaceCreated(holder: SurfaceHolder) {
        backgroundOffsetPx = 0f
        deathPromptHandled = false
        audio.onHostResume()
        transferService.sendCommand(PlayerJoinRedCommand("local", 0))
        running = true
        
        Thread {
            while (running) {
                val snapshot = transferService.receiveSnapshot() ?: continue
                latestSnapshot = snapshot
                Log.d("GameView", "Received snapshot: ${snapshot.elapsedTimeSec}")
                audio.sync(snapshot, snapshot.events)
            }
        }.start()
        
        choreographer.postFrameCallback(object : FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!running) return
                val snapshot = latestSnapshot ?: return
                
                if (!snapshot.gameOver) {
                    updateBackground(snapshot)
                }
                drawFrame(snapshot)
                
                choreographer.postFrameCallback(this)
            }
        })
    }
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        audio.onHostPause()
    }
    
    private fun updateBackground(frame: FrameSnapshot) {
        backgroundOffsetPx++
        backgroundOffsetPx %= frame.viewport.contentRect.height()
    }
    
    private fun drawFrame(frame: FrameSnapshot) {
        val canvas: Canvas = holder.lockCanvas() ?: return
        try {
            val viewport = frame.viewport
            val contentRect = viewport.contentRect
            
            canvas.drawColor(Color.BLACK)
            
            drawScrollingBackground(canvas, frame.background, contentRect)
            
            canvas.withClip(contentRect) {
                frame.entities.forEach { state ->
                    val bitmap = state.type.bitmap
                    val targetRect = viewport.worldRectToScreen(
                        centerX = state.x,
                        centerY = state.y,
                        width = state.width,
                        height = state.height
                    )
                    drawBitmapAspectCover(
                        canvas = this,
                        bitmap = bitmap,
                        targetRect = targetRect,
                        paint = paint
                    )
                }
                
                frame.entities.forEach { state ->
                    drawHealthBar(this, state, viewport)
                }
            }
            
            drawHud(canvas, contentRect, frame, viewport)
            if (frame.gameOver) {
                drawDeathOverlay(canvas, contentRect, viewport)
            }
            if (startButtonVisible) {
                drawStartButton(canvas, contentRect, viewport)
            }
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }
    
    private fun drawScrollingBackground(canvas: Canvas, backgroundBitmap: Bitmap, contentRect: RectF) {
        if (contentRect.height() <= 0f) return
        
        val offset = backgroundOffsetPx % contentRect.height()
        val firstRect = RectF(
            contentRect.left,
            contentRect.top + offset - contentRect.height(),
            contentRect.right,
            contentRect.top + offset
        )
        val secondRect = RectF(
            contentRect.left,
            contentRect.top + offset,
            contentRect.right,
            contentRect.top + offset + contentRect.height()
        )
        
        canvas.withClip(contentRect) {
            drawBitmap(backgroundBitmap, null, firstRect, paint)
            drawBitmap(backgroundBitmap, null, secondRect, paint)
        }
    }
    
    private val EntityType.isHero
        get() = when (this) {
            EntityType.RED_HERO,
            EntityType.BLUE_HERO -> true
            else -> false
        }
    
    private val EntityType.bitmap
        get() = when (this) {
            EntityType.RED_HERO -> sprites.heroRed
            EntityType.BLUE_HERO -> sprites.heroBlue
            EntityType.MOB_ENEMY -> sprites.enemyMob
            EntityType.ELITE_ENEMY -> sprites.enemyElite
            EntityType.SUPER_ENEMY -> sprites.enemySuper
            EntityType.BOSS_ENEMY -> sprites.enemyBoss
            EntityType.RED_HERO_BULLET -> sprites.bulletHeroRed
            EntityType.BLUE_HERO_BULLET -> sprites.bulletHeroBlue
            EntityType.ENEMY_BULLET -> sprites.bulletEnemy
            EntityType.HEALTH_PROP -> sprites.propHealth
            EntityType.BOMB_PROP -> sprites.propBomb
            EntityType.ENHANCE_PROP -> sprites.propEnhance
            EntityType.RAMPAGE_PROP -> sprites.propRampage
        }
    
    private val EntityType.hasHealthBar: Boolean
        get() = when (this) {
            EntityType.RED_HERO,
            EntityType.BLUE_HERO,
            EntityType.MOB_ENEMY,
            EntityType.ELITE_ENEMY,
            EntityType.SUPER_ENEMY,
            EntityType.BOSS_ENEMY -> true
            else -> false
        }
    
    private val FrameSnapshot.background: Bitmap
        get() = when (difficulty) {
            GameDifficulty.EASY -> sprites.bg1
            GameDifficulty.NORMAL -> sprites.bg3
            GameDifficulty.HARD -> sprites.bg5
        }
    
    private val FrameSnapshot.viewport: WorldViewport
        get() = WorldViewport(
            worldWidth = worldWidth,
            worldHeight = worldHeight,
            screenWidth = this@GameView.width,
            screenHeight = this@GameView.height,
        )
    
    private fun drawBitmapAspectCover(
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
        
        val scale = max(dstW / srcW, dstH / srcH)
        val drawW = srcW * scale
        val drawH = srcH * scale
        
        val left = targetRect.centerX() - drawW / 2f
        val top = targetRect.centerY() - drawH / 2f
        val drawRect = RectF(left, top, left + drawW, top + drawH)
        
        canvas.withClip(targetRect) {
            drawBitmap(bitmap, null, drawRect, paint)
        }
    }
    
    private fun drawHealthBar(canvas: Canvas, state: EntitySnapshot, viewport: WorldViewport) {
        val hp = state.hp ?: return
        val maxHp = state.maxHp ?: return
        if (!state.type.hasHealthBar || maxHp <= 0) return
        
        val targetRect = viewport.worldRectToScreen(
            centerX = state.x,
            centerY = state.y,
            width = state.width,
            height = state.height
        )
        
        val barWidth = targetRect.width() * 0.88f
        val barHeight = viewport.scale * 0.10f
        val gap = viewport.scale * 0.10f
        val left = targetRect.centerX() - barWidth / 2f
        val top = targetRect.bottom + gap
        val bottom = top + barHeight
        val backgroundRect = RectF(left, top, left + barWidth, bottom)
        val fillRatio = (hp.toFloat() / maxHp.toFloat()).coerceIn(0f, 1f)
        val fillRect = RectF(
            backgroundRect.left,
            backgroundRect.top,
            backgroundRect.left + backgroundRect.width() * fillRatio,
            backgroundRect.bottom
        )
        val fillPaint = if (state.type.isHero) heroHpBarPaint else enemyHpBarPaint
        
        canvas.drawRoundRect(backgroundRect, barHeight * 0.35f, barHeight * 0.35f, hpBarBackgroundPaint)
        if (fillRatio > 0f) {
            canvas.drawRoundRect(fillRect, barHeight * 0.35f, barHeight * 0.35f, fillPaint)
        }
        canvas.drawRoundRect(backgroundRect, barHeight * 0.35f, barHeight * 0.35f, hpBarBorderPaint)
    }
    
    private fun drawHud(canvas: Canvas, contentRect: RectF, frame: FrameSnapshot, viewport: WorldViewport) {
        val padding = max(16f, viewport.scale * 0.22f)
        hudTextPaint.textSize = max(26f, viewport.scale * 0.34f)
        val lineHeight = hudTextPaint.fontSpacing
        val left = contentRect.left + padding
        val topBaseline = contentRect.top + padding - hudTextPaint.fontMetrics.ascent
        
        canvas.drawText("红方分数: ${frame.scoreRed}", left, topBaseline, hudTextPaint)
        canvas.drawText("蓝方分数: ${frame.scoreBlue}", left, topBaseline + lineHeight, hudTextPaint)
        canvas.drawText(formatElapsedTime(frame.elapsedTimeSec), left, topBaseline + lineHeight * 2, hudTextPaint)
    }
    
    private fun drawDeathOverlay(canvas: Canvas, contentRect: RectF, viewport: WorldViewport) {
        val bannerHeight = contentRect.height() * 0.22f
        val bannerTop = contentRect.centerY() - bannerHeight * 0.5f
        val bannerRect = RectF(contentRect.left, bannerTop, contentRect.right, bannerTop + bannerHeight)
        canvas.drawRect(bannerRect, deathBannerPaint)
        
        val titleSize = max(48f, viewport.scale * 0.72f)
        val subtitleSize = max(20f, viewport.scale * 0.28f)
        deathTitlePaint.textSize = titleSize
        deathSubtitlePaint.textSize = subtitleSize
        
        val titleBaseline = bannerRect.centerY() - (deathTitlePaint.fontMetrics.ascent + deathTitlePaint.fontMetrics.descent) * 0.5f - subtitleSize * 0.32f
        val subtitleBaseline = titleBaseline + subtitleSize * 2.0f
        canvas.drawText("YOU DIED", bannerRect.centerX(), titleBaseline, deathTitlePaint)
        canvas.drawText("点击任意位置继续...", bannerRect.centerX(), subtitleBaseline, deathSubtitlePaint)
    }
    
    private fun formatElapsedTime(seconds: Float): String {
        val totalSeconds = seconds.coerceAtLeast(0f).toInt()
        val minutes = totalSeconds / 60
        val remainSeconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, remainSeconds)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val snapshot = latestSnapshot ?: return true
        
        if (startButtonVisible && event.action == MotionEvent.ACTION_DOWN) {
            val viewport = snapshot.viewport
            val buttonRect = getStartButtonRect(viewport.contentRect, viewport)
            if (buttonRect.contains(event.x, event.y)) {
                startButtonVisible = false
                transferService.sendCommand(StartGameCommand)
                return true
            }
        }
        
        if (snapshot.gameOver) {
            if (event.action == MotionEvent.ACTION_DOWN && !deathPromptHandled) {
                deathPromptHandled = true
                post { onGameOver(snapshot) }
            }
            return true
        }
        
        val command = controller.onMotionEvent(event, snapshot.viewport)
        if (command != null) transferService.sendCommand(command)
        return true
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        audio.release()
    }
    
    private fun drawStartButton(canvas: Canvas, contentRect: RectF, viewport: WorldViewport) {
        val buttonWidth = contentRect.width() * 0.5f
        val buttonHeight = viewport.scale * 0.8f
        val buttonLeft = contentRect.centerX() - buttonWidth / 2f
        val buttonTop = contentRect.bottom - buttonHeight - viewport.scale * 0.3f
        val buttonRect = RectF(buttonLeft, buttonTop, buttonLeft + buttonWidth, buttonTop + buttonHeight)
        
        canvas.drawRoundRect(buttonRect, buttonHeight * 0.2f, buttonHeight * 0.2f, startButtonBgPaint)
        canvas.drawRoundRect(buttonRect, buttonHeight * 0.2f, buttonHeight * 0.2f, startButtonBorderPaint)
        
        startButtonTextPaint.textSize = max(28f, viewport.scale * 0.42f)
        val textX = buttonRect.centerX()
        val textY = buttonRect.centerY() - (startButtonTextPaint.fontMetrics.ascent + startButtonTextPaint.fontMetrics.descent) * 0.5f
        canvas.drawText("开始游戏", textX, textY, startButtonTextPaint)
    }
    
    private fun getStartButtonRect(contentRect: RectF, viewport: WorldViewport): RectF {
        val buttonWidth = contentRect.width() * 0.5f
        val buttonHeight = viewport.scale * 0.8f
        val buttonLeft = contentRect.centerX() - buttonWidth / 2f
        val buttonTop = contentRect.bottom - buttonHeight - viewport.scale * 0.3f
        return RectF(buttonLeft, buttonTop, buttonLeft + buttonWidth, buttonTop + buttonHeight)
    }
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        isFilterBitmap = true
    }
    private val startButtonBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(220, 76, 175, 80)
    }
    private val startButtonBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.argb(255, 255, 255, 255)
    }
    private val startButtonTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        setShadowLayer(6f, 2f, 2f, Color.argb(180, 0, 0, 0))
    }
    private val hpBarBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(180, 24, 24, 24)
    }
    private val hpBarBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.argb(220, 235, 235, 235)
    }
    private val heroHpBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.rgb(72, 208, 96)
    }
    private val enemyHpBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.rgb(220, 68, 68)
    }
    private val hudTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 165, 0)
        style = Paint.Style.FILL
        textSize = 32f
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        setShadowLayer(8f, 3f, 3f, Color.argb(200, 0, 0, 0))
    }
    private val deathBannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(168, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val deathTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 165, 0)
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
        setShadowLayer(10f, 4f, 4f, Color.argb(200, 0, 0, 0))
    }
    private val deathSubtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 165, 0)
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        setShadowLayer(8f, 3f, 3f, Color.argb(200, 0, 0, 0))
    }
}
