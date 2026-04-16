package pers.hpcx.aircraftwar.transfer

import org.json.JSONObject
import pers.hpcx.aircraftwar.kernal.*
import pers.hpcx.aircraftwar.kernal.CommandType.*
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.concurrent.BlockingQueue

internal object TcpTransferProtocol {
    
    fun encodeCommand(command: GameCommand): ByteArray {
        return JSONObject().apply {
            put(
                "type", when (command) {
                    is StartGameCommand -> START_GAME.name
                    is PlayerJoinRedCommand -> PLAYER_JOIN_RED.name
                    is PlayerJoinBlueCommand -> PLAYER_JOIN_BLUE.name
                    is PlayerMoveCommand -> PLAYER_MOVE.name
                    is PlayerStopCommand -> PLAYER_STOP.name
                    is PlayerLeaveCommand -> PLAYER_LEAVE.name
                }
            )
            put(
                "payload", when (command) {
                    is StartGameCommand -> ""
                    is PlayerJoinRedCommand -> command.toJson()
                    is PlayerJoinBlueCommand -> command.toJson()
                    is PlayerMoveCommand -> command.toJson()
                    is PlayerStopCommand -> command.toJson()
                    is PlayerLeaveCommand -> command.toJson()
                }
            )
        }.toString().toByteArray()
    }
    
    fun decodeCommand(data: ByteArray): GameCommand {
        val message = JSONObject(String(data))
        val type = message.getString("type")
        val payload = message.getString("payload")
        return when (valueOf(type)) {
            START_GAME -> StartGameCommand
            PLAYER_JOIN_RED -> PlayerJoinRedCommand.fromJson(payload)
            PLAYER_JOIN_BLUE -> PlayerJoinBlueCommand.fromJson(payload)
            PLAYER_MOVE -> PlayerMoveCommand.fromJson(payload)
            PLAYER_STOP -> PlayerStopCommand.fromJson(payload)
            PLAYER_LEAVE -> PlayerLeaveCommand.fromJson(payload)
        }
    }
    
    fun encodeSnapshot(snapshot: FrameSnapshot): ByteArray {
        return snapshot.toJson().toByteArray()
    }
    
    fun decodeSnapshot(data: ByteArray): FrameSnapshot {
        return FrameSnapshot.fromJson(String(data))
    }
    
    fun writeFrame(output: DataOutputStream, payload: ByteArray) {
        synchronized(output) {
            output.writeInt(payload.size)
            output.write(payload)
            output.flush()
        }
    }
    
    fun readFrame(input: DataInputStream): ByteArray {
        val length = input.readInt()
        require(length >= 0) { "Negative frame length: $length" }
        val payload = ByteArray(length)
        input.readFully(payload)
        return payload
    }
    
    fun <T> offerDroppingOldest(queue: BlockingQueue<T>, value: T) {
        while (!queue.offer(value)) {
            queue.poll() ?: return
        }
    }
}
