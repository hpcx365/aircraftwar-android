package com.example.aircraftwar.ui

import android.view.MotionEvent
import com.example.aircraftwar.engine.*

interface PlayerInputController {
    
    fun onMotionEvent(event: MotionEvent, viewport: WorldViewport): List<GameCommand>
}

class TouchPlayerInputController(
    private val playerId: PlayerId = PlayerId.LOCAL,
) : PlayerInputController {
    
    private var nextSequence = 0L
    
    override fun onMotionEvent(event: MotionEvent, viewport: WorldViewport): List<GameCommand> {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                if (!viewport.containsScreenPoint(event.x, event.y)) {
                    emptyList()
                } else {
                    val target = Vec(
                        x = viewport.screenToWorldX(event.x),
                        y = viewport.screenToWorldY(event.y),
                    )
                    listOf(
                        UpdatePlayerIntent(
                            playerId = playerId,
                            sequence = nextSequence++,
                            intent = PlayerIntent(
                                moveTarget = target,
                                primaryFirePressed = true,
                            ),
                        )
                    )
                }
            }
            
            else                    -> emptyList()
        }
    }
}
