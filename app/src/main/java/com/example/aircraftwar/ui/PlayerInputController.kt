package com.example.aircraftwar.ui

import android.view.MotionEvent
import com.example.aircraftwar.engine.PlayerCommand
import com.example.aircraftwar.engine.PlayerMoveCommand
import com.example.aircraftwar.engine.Vec

interface PlayerInputController {
    
    fun onMotionEvent(event: MotionEvent, viewport: WorldViewport): List<PlayerCommand>
}

class TouchPlayerInputController(
    private val playerId: String,
) : PlayerInputController {
    
    private var nextSequence = 1
    
    override fun onMotionEvent(event: MotionEvent, viewport: WorldViewport): List<PlayerCommand> {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                val target = Vec(
                    x = viewport.screenToWorldX(event.x),
                    y = viewport.screenToWorldY(event.y),
                )
                listOf(
                    PlayerMoveCommand(
                        playerId = playerId,
                        sequence = nextSequence++,
                        targetPosition = target,
                    )
                )
            }
            else -> emptyList()
        }
    }
}
