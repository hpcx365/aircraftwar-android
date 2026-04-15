package com.example.aircraftwar.transfer

import com.example.aircraftwar.engine.FrameSnapshot
import com.example.aircraftwar.engine.GameCommand
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.MILLISECONDS

class LoopbackTransferService : ObjectTransferService {
    
    private var timeoutMs = 0L
    private val commandQueue = LinkedBlockingQueue<GameCommand>()
    private val snapshotQueue = LinkedBlockingQueue<FrameSnapshot>()
    
    override fun setTimeout(timeoutMs: Long) {
        this.timeoutMs = timeoutMs
    }
    
    override fun sendCommand(command: GameCommand) {
        commandQueue.offer(command)
    }
    
    override fun sendSnapshot(snapshot: FrameSnapshot) {
        snapshotQueue.offer(snapshot)
    }
    
    override fun receiveCommand(): GameCommand? {
        return commandQueue.poll(timeoutMs, MILLISECONDS)
    }
    
    override fun receiveSnapshot(): FrameSnapshot? {
        return snapshotQueue.poll(timeoutMs, MILLISECONDS)
    }
}
