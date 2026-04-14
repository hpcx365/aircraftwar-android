package com.example.aircraftwar.ui

import android.content.Context
import android.graphics.*
import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.graphics.withClip
import com.example.aircraftwar.engine.GameDifficulty
import com.example.aircraftwar.engine.GameEngine
import com.example.aircraftwar.engine.PlayerJoinRedCommand
import com.example.aircraftwar.entity.EntityType
import kotlin.math.max
import kotlin.random.Random

class GameView(
    context: Context,
    difficulty: GameDifficulty = GameDifficulty.NORMAL,
    private val onDeathContinue: () -> Unit = {},
) : SurfaceView(context), SurfaceHolder.Callback {
    
    private val session = GameEngine(difficulty.config)
    private val inputController = TouchPlayerInputController("local")
    private val sprites = SpriteRepository(context)
    private val audio = GameAudio(context)
    private val viewport = WorldViewport(difficulty.config.worldWidth, difficulty.config.worldHeight)
    private val backgroundBitmap = when (difficulty) {
        GameDifficulty.EASY -> sprites.bg1
        GameDifficulty.NORMAL -> if (Random.nextBoolean()) sprites.bg2 else sprites.bg3
        GameDifficulty.HARD -> if (Random.nextBoolean()) sprites.bg4 else sprites.bg5
    }
    
    private var running = false
    
    private var latestSnapshot: FrameSnapshot? = null
    private val choreographer = Choreographer.getInstance()
    private var frameCallback: Choreographer.FrameCallback? = null
    private var lastFrameTimeNs: Long = 0
    
    private var backgroundOffsetPx = 0f
    private val backgroundScrollSpeedPxPerSec = 120f
    private var deathPromptHandled = false
    
    init {
        holder.addCallback(this)
        isFocusable = true
        isClickable = true
    }
    
    private fun startGameLoop() {
        lastFrameTimeNs = System.nanoTime()
        frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!running) return
                
                val dt = ((frameTimeNanos - lastFrameTimeNs) / 1_000_000_000f).coerceAtMost(0.033f)
                lastFrameTimeNs = frameTimeNanos
                
                session.update(dt)
                val snapshot = session.snapshot()
                latestSnapshot = snapshot
                audio.sync(snapshot, snapshot.events)
                
                if (!snapshot.gameOver) {
                    updateBackground(dt)
                }
                drawFrame(snapshot)
                
                if (snapshot.gameOver) {
                    running = false
                    return
                }
                
                choreographer.postFrameCallback(this)
            }
        }
        choreographer.postFrameCallback(frameCallback!!)
    }
    
    private fun updateBackground(dt: Float) {
        viewport.update(width, height)
        val contentHeight = viewport.contentRect().height()
        if (contentHeight <= 0f) return
        
        backgroundOffsetPx += backgroundScrollSpeedPxPerSec * dt
        backgroundOffsetPx %= contentHeight
    }
    
    private fun drawFrame(snapshot: FrameSnapshot) {
        val canvas: Canvas = holder.lockCanvas() ?: return
        try {
            canvas.drawColor(Color.BLACK)
            
            viewport.update(width, height)
            val contentRect = viewport.contentRect()
            
            drawScrollingBackground(canvas, contentRect)
            
            canvas.withClip(contentRect) {
                snapshot.drawables.forEach { state ->
                    val bitmap = state.type.getBitmap()
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
                
                snapshot.drawables.forEach { state ->
                    drawHealthBar(this, state)
                }
            }
            
            drawHud(canvas, contentRect, snapshot)
            if (snapshot.gameOver) {
                drawDeathOverlay(canvas, contentRect)
            }
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }
    
    private fun drawScrollingBackground(canvas: Canvas, contentRect: RectF) {
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
    
    private fun EntityType.isHero() = this == EntityType.RED_HERO || this == EntityType.BLUE_HERO
    
    private fun EntityType.getBitmap() = when (this) {
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
    
    private fun EntityType.getHasHealthBar(): Boolean = when (this) {
        EntityType.RED_HERO,
        EntityType.BLUE_HERO,
        EntityType.MOB_ENEMY,
        EntityType.ELITE_ENEMY,
        EntityType.SUPER_ENEMY,
        EntityType.BOSS_ENEMY -> true
        else -> false
    }
    
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
    
    private fun drawHealthBar(canvas: Canvas, state: Drawable) {
        val hp = state.hp ?: return
        val maxHp = state.maxHp ?: return
        if (!state.type.getHasHealthBar() || maxHp <= 0) return
        
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
        val fillPaint = if (state.type.isHero()) heroHpBarPaint else enemyHpBarPaint
        
        canvas.drawRoundRect(backgroundRect, barHeight * 0.35f, barHeight * 0.35f, hpBarBackgroundPaint)
        if (fillRatio > 0f) {
            canvas.drawRoundRect(fillRect, barHeight * 0.35f, barHeight * 0.35f, fillPaint)
        }
        canvas.drawRoundRect(backgroundRect, barHeight * 0.35f, barHeight * 0.35f, hpBarBorderPaint)
    }
    
    private fun drawHud(canvas: Canvas, contentRect: RectF, snapshot: FrameSnapshot) {
        val padding = max(16f, viewport.scale * 0.22f)
        hudTextPaint.textSize = max(26f, viewport.scale * 0.34f)
        val lineHeight = hudTextPaint.fontSpacing
        val left = contentRect.left + padding
        val topBaseline = contentRect.top + padding - hudTextPaint.fontMetrics.ascent
        
        canvas.drawText("红方分数: ${snapshot.scoreRed}", left, topBaseline, hudTextPaint)
        canvas.drawText("蓝方分数: ${snapshot.scoreBlue}", left, topBaseline + lineHeight, hudTextPaint)
        canvas.drawText(formatElapsedTime(snapshot.elapsedTimeSec), left, topBaseline + lineHeight * 2, hudTextPaint)
    }
    
    private fun drawDeathOverlay(canvas: Canvas, contentRect: RectF) {
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
        val snapshot = latestSnapshot
        if (snapshot != null && snapshot.gameOver) {
            if (event.action == MotionEvent.ACTION_DOWN && !deathPromptHandled) {
                deathPromptHandled = true
                post { onDeathContinue() }
            }
            return true
        }
        
        viewport.update(width, height)
        inputController.onMotionEvent(event, viewport).forEach(session::submitCommand)
        return true
    }
    
    override fun surfaceCreated(holder: SurfaceHolder) {
        viewport.update(width, height)
        backgroundOffsetPx = 0f
        deathPromptHandled = false
        audio.onHostResume()
        session.submitCommand(PlayerJoinRedCommand("local", 0))
        latestSnapshot = session.snapshot()
        running = true
        startGameLoop()
    }
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        viewport.update(width, height)
    }
    
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        frameCallback?.let { choreographer.removeFrameCallback(it) }
        frameCallback = null
        audio.onHostPause()
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        audio.release()
    }
    
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        isFilterBitmap = true
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
