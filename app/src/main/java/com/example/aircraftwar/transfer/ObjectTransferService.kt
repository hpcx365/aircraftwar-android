package com.example.aircraftwar.transfer

import com.example.aircraftwar.engine.FrameSnapshot
import com.example.aircraftwar.engine.GameCommand

interface ObjectTransferService {
    
    fun setTimeout(timeoutMs: Long)
    
    fun sendCommand(command: GameCommand)
    
    fun sendSnapshot(snapshot: FrameSnapshot)
    
    fun receiveCommand(): GameCommand?
    
    fun receiveSnapshot(): FrameSnapshot?
}
