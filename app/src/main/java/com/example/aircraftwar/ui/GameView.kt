package com.example.aircraftwar.ui

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.graphics.withClip
import com.example.aircraftwar.engine.GameDifficulty
import com.example.aircraftwar.entity.EntityType
import kotlin.math.max
import kotlin.random.Random

class GameView(
    context: Context,
    difficulty: GameDifficulty = GameDifficulty.NORMAL,
    private val onDeathContinue: () -> Unit = {},
) : SurfaceView(context), SurfaceHolder.Callback, Runnable {
    
    private val session = GameSession(difficulty)
    private val inputController = TouchPlayerInputController()
    private val sprites = SpriteRepository(context)
    private val audio = GameAudio(context)
    private val viewport = WorldViewport(difficulty.config.worldWidth, difficulty.config.worldHeight)
    private val backgroundBitmap = when (difficulty) {
        GameDifficulty.EASY -> sprites.bg1
        GameDifficulty.NORMAL -> if (Random.nextBoolean()) sprites.bg2 else sprites.bg3
        GameDifficulty.HARD -> if (Random.nextBoolean()) sprites.bg4 else sprites.bg5
    }
    
    @Volatile
    private var running = false
    
    @Volatile
    private var latestSnapshot = FrameSnapshot(
        drawables = emptyList(),
        score = 0,
        elapsedTimeSec = 0f,
        hasBoss = false,
        gameOver = false,
    )
    private var renderThread: Thread? = null
    
    private var backgroundOffsetPx = 0f
    private val backgroundScrollSpeedPxPerSec = 120f
    private var deathPromptHandled = false
    
    init {
        holder.addCallback(this)
        isFocusable = true
        isClickable = true
    }
    
    override fun run() {
        var lastNs = System.nanoTime()
        while (running) {
            val now = System.nanoTime()
            val dt = ((now - lastNs) / 1_000_000_000f).coerceAtMost(0.033f)
            lastNs = now
            
            val frame = session.tick(dt)
            latestSnapshot = frame.renderFrame
            audio.sync(frame.renderFrame, frame.audioEvents)
            
            if (!frame.renderFrame.gameOver) {
                updateBackground(dt)
            }
            drawFrame(frame.renderFrame)
        }
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
    
    private val EntityType.bitmap
        get() = when (this) {
            EntityType.HERO         -> sprites.hero
            EntityType.MOB_ENEMY    -> sprites.enemyMob
            EntityType.ELITE_ENEMY  -> sprites.enemyElite
            EntityType.SUPER_ENEMY  -> sprites.enemySuper
            EntityType.BOSS_ENEMY   -> sprites.enemyBoss
            EntityType.HERO_BULLET  -> sprites.bulletHero
            EntityType.ENEMY_BULLET -> sprites.bulletEnemy
            EntityType.HEALTH_PROP  -> sprites.propHealth
            EntityType.BOMB_PROP    -> sprites.propBomb
            EntityType.ENHANCE_PROP -> sprites.propEnhance
            EntityType.RAMPAGE_PROP -> sprites.propRampage
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
        val fillPaint = if (state.type == EntityType.HERO) heroHpBarPaint else enemyHpBarPaint
        
        canvas.drawRoundRect(backgroundRect, barHeight * 0.35f, barHeight * 0.35f, hpBarBackgroundPaint)
        if (fillRatio > 0f) {
            canvas.drawRoundRect(fillRect, barHeight * 0.35f, barHeight * 0.35f, fillPaint)
        }
        canvas.drawRoundRect(backgroundRect, barHeight * 0.35f, barHeight * 0.35f, hpBarBorderPaint)
    }
    
    private fun drawHud(canvas: Canvas, contentRect: RectF, snapshot: FrameSnapshot) {
        val padding = max(16f, viewport.scale * 0.22f)
        hudTextPaint.textSize = max(26f, viewport.scale * 0.34f)
        val lineHeight = hudTextPaint.fontSpacing * 0.92f
        val left = contentRect.left + padding
        val topBaseline = contentRect.top + padding - hudTextPaint.fontMetrics.ascent
        
        canvas.drawText("Score: ${snapshot.score}", left, topBaseline, hudTextPaint)
        canvas.drawText("Time: ${formatElapsedTime(snapshot.elapsedTimeSec)}", left, topBaseline + lineHeight, hudTextPaint)
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
    
    private val EntityType.hasHealthBar: Boolean
        get() = when (this) {
            EntityType.HERO,
            EntityType.MOB_ENEMY,
            EntityType.ELITE_ENEMY,
            EntityType.SUPER_ENEMY,
            EntityType.BOSS_ENEMY -> true
            
            else -> false
        }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val snapshot = latestSnapshot
        if (snapshot.gameOver) {
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
        latestSnapshot = session.currentFrame().renderFrame
        running = true
        renderThread = Thread(this, "game-loop").also { it.start() }
    }
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        viewport.update(width, height)
    }
    
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        renderThread?.join(500)
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
