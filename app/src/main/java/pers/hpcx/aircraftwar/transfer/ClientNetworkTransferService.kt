package pers.hpcx.aircraftwar.transfer

import android.util.Log
import pers.hpcx.aircraftwar.kernal.FrameSnapshot
import pers.hpcx.aircraftwar.kernal.GameCommand
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicLong

class ClientNetworkTransferService(
    private val server: InetSocketAddress,
    private val connectTimeoutMs: Int = 1_500,
    private val reconnectIntervalMs: Long = 1_000,
    private val idleTimeoutMs: Long = 10_000,
    sendQueueCapacity: Int = 256,
    receiveQueueCapacity: Int = 64,
) : ClientTransferService {
    
    private data class Connection(
        val socket: Socket,
        val input: DataInputStream,
        val output: DataOutputStream,
        val lastDemandAt: AtomicLong = AtomicLong(System.currentTimeMillis()),
    )
    
    private val sendQueue = LinkedBlockingQueue<GameCommand>(sendQueueCapacity)
    private val receiveQueue = LinkedBlockingQueue<FrameSnapshot>(receiveQueueCapacity)
    private val executor = Executors.newCachedThreadPool()
    private val lastDemandAt = AtomicLong(0L)
    private val lastConnectAttemptAt = AtomicLong(0L)
    
    @Volatile
    private var connection: Connection? = null
    
    override fun sendCommand(command: GameCommand) {
        touchDemand()
        TcpTransferProtocol.offerDroppingOldest(sendQueue, command)
    }
    
    override fun receiveSnapshot(timeoutMs: Long): FrameSnapshot? {
        touchDemand()
        return receiveQueue.poll(timeoutMs, MILLISECONDS)
    }
    
    init {
        executor.submit { runConnectionManager() }
        executor.submit { runSender() }
    }
    
    private fun runConnectionManager() {
        while (true) {
            try {
                val now = System.currentTimeMillis()
                val current = connection
                when {
                    current == null -> {
                        val shouldConnect = !sendQueue.isEmpty() || now - lastDemandAt.get() < idleTimeoutMs
                        if (shouldConnect && now - lastConnectAttemptAt.get() >= reconnectIntervalMs) {
                            lastConnectAttemptAt.set(now)
                            connect()
                        }
                    }
                    now - current.lastDemandAt.get() >= idleTimeoutMs && sendQueue.isEmpty() -> {
                        closeConnection(current, "idle timeout")
                    }
                }
            } catch (e: Exception) {
                Log.w(ClientNetworkTransferService::class.simpleName, "Connection manager tick failed", e)
            }
            Thread.sleep(200)
        }
    }
    
    private fun connect() {
        synchronized(this) {
            if (connection != null) return
            try {
                val socket = Socket()
                socket.tcpNoDelay = true
                socket.keepAlive = false
                socket.connect(server, connectTimeoutMs)
                val next = Connection(
                    socket = socket,
                    input = DataInputStream(BufferedInputStream(socket.getInputStream())),
                    output = DataOutputStream(BufferedOutputStream(socket.getOutputStream())),
                    lastDemandAt = AtomicLong(System.currentTimeMillis()),
                )
                connection = next
                Log.i(ClientNetworkTransferService::class.simpleName, "Connected to $server")
                executor.submit { runReceiver(next) }
            } catch (e: Exception) {
                Log.w(ClientNetworkTransferService::class.simpleName, "Connect to $server failed", e)
            }
        }
    }
    
    private fun runSender() {
        while (true) {
            try {
                val command = sendQueue.take()
                val payload = TcpTransferProtocol.encodeCommand(command)
                
                while (true) {
                    touchDemand()
                    val current = connection
                    if (current == null) {
                        Thread.sleep(100)
                        continue
                    }
                    try {
                        TcpTransferProtocol.writeFrame(current.output, payload)
                        current.lastDemandAt.set(System.currentTimeMillis())
                        Log.d(ClientNetworkTransferService::class.simpleName, "Command sent: $command")
                        break
                    } catch (e: Exception) {
                        Log.w(
                            ClientNetworkTransferService::class.simpleName,
                            "Send failed, will retry after reconnect: $command",
                            e
                        )
                        closeConnection(current, "send failed")
                        Thread.sleep(100)
                    }
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            } catch (e: Exception) {
                Log.e(ClientNetworkTransferService::class.simpleName, "Sender error", e)
            }
        }
    }
    
    private fun runReceiver(conn: Connection) {
        try {
            while (connection === conn && !conn.socket.isClosed) {
                val payload = TcpTransferProtocol.readFrame(conn.input)
                conn.lastDemandAt.set(System.currentTimeMillis())
                val snapshot = TcpTransferProtocol.decodeSnapshot(payload)
                TcpTransferProtocol.offerDroppingOldest(receiveQueue, snapshot)
            }
        } catch (_: EOFException) {
            Log.i(ClientNetworkTransferService::class.simpleName, "Server closed connection")
        } catch (_: SocketException) {
            Log.i(ClientNetworkTransferService::class.simpleName, "Client socket closed")
        } catch (_: SocketTimeoutException) {
            Log.i(ClientNetworkTransferService::class.simpleName, "Client socket timeout")
        } catch (e: Exception) {
            Log.w(ClientNetworkTransferService::class.simpleName, "Receiver error", e)
        } finally {
            closeConnection(conn, "receiver stopped")
        }
    }
    
    private fun touchDemand() {
        val now = System.currentTimeMillis()
        lastDemandAt.set(now)
        connection?.lastDemandAt?.set(now)
    }
    
    private fun closeConnection(conn: Connection, reason: String) {
        synchronized(this) {
            if (connection !== conn) return
            connection = null
            try {
                conn.socket.close()
            } catch (_: Exception) {
            }
            Log.i(ClientNetworkTransferService::class.simpleName, "Disconnected from $server: $reason")
        }
    }
}
