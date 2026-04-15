package com.example.aircraftwar.transfer

import android.util.Log
import com.example.aircraftwar.engine.*
import com.example.aircraftwar.engine.SerializationType.*
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.MILLISECONDS

class NetworkTransferService(
    private val remote: InetSocketAddress,
) : ObjectTransferService {
    
    private var timeoutMs = 0L
    private var socket = DatagramSocket()
    private val sendQueue = LinkedBlockingQueue<ByteArray>()
    private val commandReceiveQueue = LinkedBlockingQueue<GameCommand>()
    private val snapshotReceiveQueue = LinkedBlockingQueue<FrameSnapshot>()
    private val executor = Executors.newCachedThreadPool()
    
    val localPort: Int get() = socket.localPort
    
    override fun setTimeout(timeoutMs: Long) {
        this.timeoutMs = timeoutMs
    }
    
    override fun sendCommand(command: GameCommand) {
        sendQueue.offer(JSONObject().apply {
            put(
                "type", when (command) {
                    is StartGameCommand -> COMMAND_START_GAME.name
                    is PlayerJoinRedCommand -> COMMAND_PLAYER_JOIN_RED.name
                    is PlayerJoinBlueCommand -> COMMAND_PLAYER_JOIN_BLUE.name
                    is PlayerMoveCommand -> COMMAND_PLAYER_MOVE.name
                    is PlayerStopCommand -> COMMAND_PLAYER_STOP.name
                    is PlayerLeaveCommand -> COMMAND_PLAYER_LEAVE.name
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
        }.toString().toByteArray())
    }
    
    override fun sendSnapshot(snapshot: FrameSnapshot) {
        sendQueue.offer(JSONObject().apply {
            put("type", SNAPSHOT_FRAME.name)
            put("payload", snapshot.toJson())
        }.toString().toByteArray())
    }
    
    override fun receiveCommand(): GameCommand? {
        return commandReceiveQueue.poll(timeoutMs, MILLISECONDS)
    }
    
    override fun receiveSnapshot(): FrameSnapshot? {
        return snapshotReceiveQueue.poll(timeoutMs, MILLISECONDS)
    }
    
    init {
        executor.submit {
            try {
                while (!socket.isClosed) {
                    val data = sendQueue.take()
                    val packet = DatagramPacket(data, data.size, remote)
                    socket.send(packet)
                }
            } catch (e: Exception) {
                Log.e("UdpTransferService", "Sender error", e)
            }
        }
        
        executor.submit {
            try {
                val buf = ByteArray(65536)
                val packet = DatagramPacket(buf, buf.size)
                while (!socket.isClosed) {
                    socket.receive(packet)
                    if (packet.address != remote) continue
                    val message = JSONObject(String(packet.data, 0, packet.length))
                    val type = message.getString("type")
                    val payload = message.getString("payload")
                    when (SerializationType.valueOf(type)) {
                        COMMAND_START_GAME -> commandReceiveQueue.add(StartGameCommand)
                        COMMAND_PLAYER_JOIN_RED -> commandReceiveQueue.add(PlayerJoinRedCommand.fromJson(payload))
                        COMMAND_PLAYER_JOIN_BLUE -> commandReceiveQueue.add(PlayerJoinBlueCommand.fromJson(payload))
                        COMMAND_PLAYER_MOVE -> commandReceiveQueue.add(PlayerMoveCommand.fromJson(payload))
                        COMMAND_PLAYER_STOP -> commandReceiveQueue.add(PlayerStopCommand.fromJson(payload))
                        COMMAND_PLAYER_LEAVE -> commandReceiveQueue.add(PlayerLeaveCommand.fromJson(payload))
                        SNAPSHOT_FRAME -> snapshotReceiveQueue.add(FrameSnapshot.fromJson(payload))
                    }
                }
            } catch (e: Exception) {
                Log.e("UdpTransferService", "Receiver error", e)
            }
        }
    }
}
