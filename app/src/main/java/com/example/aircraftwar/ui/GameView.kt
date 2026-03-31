package com.example.aircraftwar.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.aircraftwar.engine.EntityState
import com.example.aircraftwar.engine.EntityType
import com.example.aircraftwar.engine.GameConstants
import com.example.aircraftwar.engine.GameEngine

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {
    private val engine = GameEngine()
    private val paint = Paint().apply { style = Paint.Style.FILL }

    @Volatile
    private var running = false
    private var renderThread: Thread? = null

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
            drawFrame(engine.snapshot())
        }
    }

    private fun drawFrame(states: List<EntityState>) {
        val canvas: Canvas = holder.lockCanvas() ?: return
        try {
            canvas.drawColor(Color.BLACK)
            val sx = width / GameConstants.WORLD_WIDTH
            val sy = height / GameConstants.WORLD_HEIGHT

            states.forEach { s ->
                paint.color = when (s.type) {
                    EntityType.HERO -> Color.GREEN
                    EntityType.ENEMY -> Color.RED
                    EntityType.HERO_BULLET -> Color.YELLOW
                    EntityType.ENEMY_BULLET -> Color.CYAN
                }
                val left = (s.x - s.width / 2) * sx
                val right = (s.x + s.width / 2) * sx
                val top = height - (s.y + s.height / 2) * sy
                val bottom = height - (s.y - s.height / 2) * sy
                canvas.drawRect(left, top, right, bottom, paint)
            }
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_DOWN) {
            val worldX = event.x / width * GameConstants.WORLD_WIDTH
            engine.moveHero(worldX)
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        running = true
        renderThread = Thread(this, "game-loop").also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        renderThread?.join(500)
    }
}
