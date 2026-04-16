package pers.hpcx.aircraftwar.transfer

import pers.hpcx.aircraftwar.kernal.FrameSnapshot
import pers.hpcx.aircraftwar.kernal.GameCommand

interface ServerTransferService {
    
    fun sendSnapshot(snapshot: FrameSnapshot)
    
    fun receiveCommand(timeoutMs: Long): GameCommand?
}
