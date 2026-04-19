package pers.hpcx.aircraftwar.client

import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.Choreographer
import android.view.Choreographer.FrameCallback
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.graphics.withClip
import pers.hpcx.aircraftwar.kernal.*
import pers.hpcx.aircraftwar.transfer.ClientTransferService
import kotlin.math.max

class GameView(
    context: Context,
    private val isHost: Boolean,
    playerId: String,
    private val transferService: ClientTransferService,
    musicEnabled: Boolean = true,
    private val onGameOver: (FrameSnapshot) -> Unit = {},
    private val onReturnToMenu: () -> Unit = {},
) : SurfaceView(context), SurfaceHolder.Callback {
    
    private val controller = PlayerController(playerId)
    private val sprites = SpriteRepository(context)
    private val audio = if (musicEnabled) AudioController(context) else null
    
    @Volatile
    private var running = false
    
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
        audio?.onHostResume()
        transferService.sendCommand(controller.createJoinCommand(isHost))
        running = true
        
        choreographer.postFrameCallback(object : FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!running) return
                latestSnapshot?.run { drawFrame(this) }
                choreographer.postFrameCallback(this)
            }
        })
        
        Thread {
            Log.i(GameView::class.simpleName, "Snapshot receive thread started")
            try {
                while (running) {
                    val snapshot = transferService.receiveSnapshot(100L) ?: continue
                    latestSnapshot = snapshot
                    startButtonVisible = isHost && !snapshot.started
                    Log.d(GameView::class.simpleName, "Received snapshot: ${snapshot.elapsedTimeSec}")
                    audio?.sync(snapshot, snapshot.events)
                }
            } catch (e: Exception) {
                Log.e(GameView::class.simpleName, "Snapshot receive thread error", e)
            } finally {
                Log.i(GameView::class.simpleName, "Snapshot receive thread stopped")
            }
        }.start()
    }
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        transferService.sendCommand(controller.createLeaveCommand())
        audio?.onHostPause()
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
            if (!frame.started) {
                drawLobbyOverlay(canvas, contentRect, viewport)
            }
            if (frame.gameOver) {
                drawDeathOverlay(canvas, contentRect, viewport)
            }
            if (startButtonVisible) {
                drawStartButtons(canvas, contentRect)
            }
            if (!isHost && !frame.started) {
                drawGuestExitButton(canvas, contentRect, viewport)
            }
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }
    
    private fun drawScrollingBackground(canvas: Canvas, backgroundBitmap: Bitmap, contentRect: RectF) {
        if (contentRect.height() <= 0f) return
        
        backgroundOffsetPx += 10
        backgroundOffsetPx %= contentRect.height()
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
    
    private fun drawLobbyOverlay(canvas: Canvas, contentRect: RectF, viewport: WorldViewport) {
        val bannerHeight = contentRect.height() * 0.18f
        val bannerTop = contentRect.centerY() - bannerHeight * 0.5f
        val bannerRect = RectF(contentRect.left, bannerTop, contentRect.right, bannerTop + bannerHeight)
        canvas.drawRect(bannerRect, deathBannerPaint)
        
        val titleSize = max(34f, viewport.scale * 0.5f)
        val subtitleSize = max(18f, viewport.scale * 0.25f)
        deathTitlePaint.textSize = titleSize
        deathSubtitlePaint.textSize = subtitleSize
        
        val title = if (isHost) "房间已创建" else "已加入房间"
        val subtitle = if (isHost) "点击下方“开始游戏”后开战" else "等待房主开始游戏，或点击下方退出"
        val titleBaseline = bannerRect.centerY() - (deathTitlePaint.fontMetrics.ascent + deathTitlePaint.fontMetrics.descent) * 0.5f - subtitleSize * 0.28f
        val subtitleBaseline = titleBaseline + subtitleSize * 1.8f
        canvas.drawText(title, bannerRect.centerX(), titleBaseline, deathTitlePaint)
        canvas.drawText(subtitle, bannerRect.centerX(), subtitleBaseline, deathSubtitlePaint)
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
        
        if (!snapshot.started && isHost && event.action == MotionEvent.ACTION_DOWN) {
            val viewport = snapshot.viewport
            val (startRect, menuRect) = getStartButtonsRect(viewport.contentRect, viewport)
            
            if (startRect.contains(event.x, event.y)) {
                startButtonVisible = false
                transferService.sendCommand(StartGameCommand)
                return true
            }
            
            if (menuRect.contains(event.x, event.y)) {
                transferService.sendCommand(controller.createLeaveCommand())
                post { onReturnToMenu() }
                return true
            }
        }
        
        if (!snapshot.started && !isHost && event.action == MotionEvent.ACTION_DOWN) {
            val viewport = snapshot.viewport
            val exitRect = getGuestExitButtonRect(viewport.contentRect, viewport)
            if (exitRect.contains(event.x, event.y)) {
                transferService.sendCommand(controller.createLeaveCommand())
                post { onReturnToMenu() }
            }
            return true
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
        audio?.release()
    }
    
    private fun drawStartButtons(canvas: Canvas, contentRect: RectF) {
        val buttonWidth = contentRect.width() * 0.38f
        val buttonHeight = contentRect.height() * 0.05f
        val gap = contentRect.width() * 0.05f
        val totalWidth = buttonWidth * 2 + gap
        val startX = contentRect.centerX() - totalWidth * 0.5f
        val buttonTop = contentRect.bottom - buttonHeight * 1.5f
        
        val startButtonRect = RectF(startX, buttonTop, startX + buttonWidth, buttonTop + buttonHeight)
        canvas.drawRoundRect(startButtonRect, buttonHeight * 0.2f, buttonHeight * 0.2f, startButtonBgPaint)
        canvas.drawRoundRect(startButtonRect, buttonHeight * 0.2f, buttonHeight * 0.2f, startButtonBorderPaint)
        
        startButtonTextPaint.textSize = buttonHeight * 0.45f
        val textX = startButtonRect.centerX()
        val textY = startButtonRect.centerY() - (startButtonTextPaint.fontMetrics.ascent + startButtonTextPaint.fontMetrics.descent) * 0.5f
        canvas.drawText("开始游戏", textX, textY, startButtonTextPaint)
        
        val menuButtonLeft = startX + buttonWidth + gap
        val menuButtonRect = RectF(menuButtonLeft, buttonTop, menuButtonLeft + buttonWidth, buttonTop + buttonHeight)
        canvas.drawRoundRect(menuButtonRect, buttonHeight * 0.2f, buttonHeight * 0.2f, menuButtonBgPaint)
        canvas.drawRoundRect(menuButtonRect, buttonHeight * 0.2f, buttonHeight * 0.2f, menuButtonBorderPaint)
        
        menuButtonTextPaint.textSize = buttonHeight * 0.45f
        val menuTextX = menuButtonRect.centerX()
        val menuTextY = menuButtonRect.centerY() - (menuButtonTextPaint.fontMetrics.ascent + menuButtonTextPaint.fontMetrics.descent) * 0.5f
        canvas.drawText("返回菜单", menuTextX, menuTextY, menuButtonTextPaint)
    }
    
    private fun drawGuestExitButton(canvas: Canvas, contentRect: RectF, viewport: WorldViewport) {
        val rect = getGuestExitButtonRect(contentRect, viewport)
        val radius = rect.height() * 0.2f
        canvas.drawRoundRect(rect, radius, radius, menuButtonBgPaint)
        canvas.drawRoundRect(rect, radius, radius, menuButtonBorderPaint)
        menuButtonTextPaint.textSize = rect.height() * 0.36f
        val textY = rect.centerY() - (menuButtonTextPaint.fontMetrics.ascent + menuButtonTextPaint.fontMetrics.descent) * 0.5f
        canvas.drawText("退出房间", rect.centerX(), textY, menuButtonTextPaint)
    }
    
    private fun getStartButtonsRect(contentRect: RectF, viewport: WorldViewport): Pair<RectF, RectF> {
        val buttonWidth = contentRect.width() * 0.38f
        val buttonHeight = viewport.scale * 0.8f
        val gap = viewport.scale * 0.15f
        val totalWidth = buttonWidth * 2 + gap
        val startX = contentRect.centerX() - totalWidth / 2f
        val buttonTop = contentRect.bottom - buttonHeight - viewport.scale * 0.3f
        
        val startRect = RectF(startX, buttonTop, startX + buttonWidth, buttonTop + buttonHeight)
        val menuRect = RectF(startX + buttonWidth + gap, buttonTop, startX + buttonWidth * 2 + gap, buttonTop + buttonHeight)
        
        return Pair(startRect, menuRect)
    }
    
    private fun getGuestExitButtonRect(contentRect: RectF, viewport: WorldViewport): RectF {
        val buttonWidth = contentRect.width() * 0.42f
        val buttonHeight = viewport.scale * 0.8f
        val left = contentRect.centerX() - buttonWidth / 2f
        val top = contentRect.bottom - buttonHeight - viewport.scale * 0.35f
        return RectF(left, top, left + buttonWidth, top + buttonHeight)
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
    private val menuButtonBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(220, 120, 120, 120)
    }
    private val menuButtonBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.argb(255, 255, 255, 255)
    }
    private val menuButtonTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
