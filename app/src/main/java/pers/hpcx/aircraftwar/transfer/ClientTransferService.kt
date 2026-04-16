package pers.hpcx.aircraftwar.transfer

import pers.hpcx.aircraftwar.kernal.FrameSnapshot
import pers.hpcx.aircraftwar.kernal.GameCommand

interface ClientTransferService {
    
    fun sendCommand(command: GameCommand)
    
    fun receiveSnapshot(timeoutMs: Long): FrameSnapshot?
}
