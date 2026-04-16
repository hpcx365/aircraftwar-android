package pers.hpcx.aircraftwar.transfer

import pers.hpcx.aircraftwar.kernal.FrameSnapshot
import pers.hpcx.aircraftwar.kernal.GameCommand
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.MILLISECONDS

class LoopbackTransferService : ClientTransferService, ServerTransferService {
    
    private val commandQueue = LinkedBlockingQueue<GameCommand>()
    private val snapshotQueue = LinkedBlockingQueue<FrameSnapshot>()
    
    override fun sendCommand(command: GameCommand) {
        commandQueue.offer(command)
    }
    
    override fun sendSnapshot(snapshot: FrameSnapshot) {
        snapshotQueue.offer(snapshot)
    }
    
    override fun receiveCommand(timeoutMs: Long): GameCommand? {
        return commandQueue.poll(timeoutMs, MILLISECONDS)
    }
    
    override fun receiveSnapshot(timeoutMs: Long): FrameSnapshot? {
        return snapshotQueue.poll(timeoutMs, MILLISECONDS)
    }
}
