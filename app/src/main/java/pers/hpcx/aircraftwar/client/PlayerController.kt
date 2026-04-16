package pers.hpcx.aircraftwar.client

import android.view.MotionEvent
import pers.hpcx.aircraftwar.kernal.PlayerCommand
import pers.hpcx.aircraftwar.kernal.PlayerMoveCommand
import pers.hpcx.aircraftwar.kernal.PlayerStopCommand
import pers.hpcx.aircraftwar.kernal.Vec

class PlayerController(
    private val playerId: String,
) {
    
    private var commandSequence = 1
    
    fun onMotionEvent(event: MotionEvent, viewport: WorldViewport): PlayerCommand? {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> PlayerMoveCommand(
                playerId = playerId,
                sequence = commandSequence++,
                targetPosition = Vec(
                    x = viewport.screenToWorldX(event.x),
                    y = viewport.screenToWorldY(event.y),
                ),
            )
            MotionEvent.ACTION_UP -> PlayerStopCommand(
                playerId = playerId,
                sequence = commandSequence++
            )
            else -> null
        }
    }
}
