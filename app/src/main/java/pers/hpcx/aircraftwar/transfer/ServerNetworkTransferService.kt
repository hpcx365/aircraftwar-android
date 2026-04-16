package pers.hpcx.aircraftwar.transfer

import android.util.Log
import pers.hpcx.aircraftwar.kernal.FrameSnapshot
import pers.hpcx.aircraftwar.kernal.GameCommand
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicLong

class ServerNetworkTransferService(
    private val port: Int,
    private val idleTimeoutMs: Long = 10_000,
    sendQueueCapacity: Int = 32,
    receiveQueueCapacity: Int = 256,
    private val clientSendQueueCapacity: Int = 8,
) : ServerTransferService {
    
    private data class ClientConnection(
        val id: String,
        val socket: Socket,
        val input: DataInputStream,
        val output: DataOutputStream,
        val outboundQueue: LinkedBlockingQueue<ByteArray>,
        val lastActiveAt: AtomicLong = AtomicLong(System.currentTimeMillis()),
    )
    
    private val serverSocket = ServerSocket(port).apply {
        reuseAddress = true
        soTimeout = 1_000
    }
    private val clients = ConcurrentHashMap<String, ClientConnection>()
    private val sendQueue = LinkedBlockingQueue<FrameSnapshot>(sendQueueCapacity)
    private val receiveQueue = LinkedBlockingQueue<GameCommand>(receiveQueueCapacity)
    private val executor = Executors.newCachedThreadPool()
    
    override fun sendSnapshot(snapshot: FrameSnapshot) {
        TcpTransferProtocol.offerDroppingOldest(sendQueue, snapshot)
    }
    
    override fun receiveCommand(timeoutMs: Long): GameCommand? {
        return receiveQueue.poll(timeoutMs, MILLISECONDS)
    }
    
    init {
        executor.submit { runAcceptLoop() }
        executor.submit { runDispatchLoop() }
        executor.submit { runIdleScanner() }
    }
    
    private fun runAcceptLoop() {
        while (!serverSocket.isClosed) {
            try {
                val socket = serverSocket.accept()
                socket.tcpNoDelay = true
                socket.keepAlive = false
                val client = ClientConnection(
                    id = "${socket.inetAddress.hostAddress}:${socket.port}",
                    socket = socket,
                    input = DataInputStream(BufferedInputStream(socket.getInputStream())),
                    output = DataOutputStream(BufferedOutputStream(socket.getOutputStream())),
                    outboundQueue = LinkedBlockingQueue(clientSendQueueCapacity),
                )
                clients[client.id] = client
                Log.i(ServerNetworkTransferService::class.simpleName, "Client connected: ${client.id}")
                executor.submit { runClientReceiver(client) }
                executor.submit { runClientSender(client) }
            } catch (_: SocketTimeoutException) {
            } catch (e: Exception) {
                Log.e(ServerNetworkTransferService::class.simpleName, "Accept error", e)
            }
        }
    }
    
    private fun runDispatchLoop() {
        while (!serverSocket.isClosed) {
            try {
                val snapshot = sendQueue.take()
                val payload = TcpTransferProtocol.encodeSnapshot(snapshot)
                clients.values.forEach { client ->
                    TcpTransferProtocol.offerDroppingOldest(client.outboundQueue, payload)
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            } catch (e: Exception) {
                Log.e(ServerNetworkTransferService::class.simpleName, "Dispatch error", e)
            }
        }
    }
    
    private fun runClientSender(client: ClientConnection) {
        try {
            while (!client.socket.isClosed) {
                val payload = client.outboundQueue.poll(1, MILLISECONDS) ?: continue
                TcpTransferProtocol.writeFrame(client.output, payload)
                client.lastActiveAt.set(System.currentTimeMillis())
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (_: SocketException) {
            Log.i(ServerNetworkTransferService::class.simpleName, "Client socket closed: ${client.id}")
        } catch (e: Exception) {
            closeClient(client, "sender failed", e)
        } finally {
            closeClient(client, "sender stopped")
        }
    }
    
    private fun runClientReceiver(client: ClientConnection) {
        try {
            while (!client.socket.isClosed) {
                val payload = TcpTransferProtocol.readFrame(client.input)
                client.lastActiveAt.set(System.currentTimeMillis())
                val command = TcpTransferProtocol.decodeCommand(payload)
                TcpTransferProtocol.offerDroppingOldest(receiveQueue, command)
            }
        } catch (_: EOFException) {
            Log.i(ServerNetworkTransferService::class.simpleName, "Client disconnected: ${client.id}")
        } catch (_: SocketException) {
            Log.i(ServerNetworkTransferService::class.simpleName, "Client socket closed: ${client.id}")
        } catch (e: Exception) {
            closeClient(client, "receiver failed", e)
        } finally {
            closeClient(client, "receiver stopped")
        }
    }
    
    private fun runIdleScanner() {
        while (!serverSocket.isClosed) {
            try {
                val now = System.currentTimeMillis()
                clients.values.forEach { client ->
                    if (now - client.lastActiveAt.get() >= idleTimeoutMs) {
                        closeClient(client, "idle timeout")
                    }
                }
                Thread.sleep(1_000)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            } catch (e: Exception) {
                Log.w(ServerNetworkTransferService::class.simpleName, "Idle scan failed", e)
            }
        }
    }
    
    private fun closeClient(client: ClientConnection, reason: String, error: Throwable? = null) {
        if (clients.remove(client.id, client)) {
            try {
                client.socket.close()
            } catch (_: Exception) {
            }
            if (error == null) {
                Log.i(ServerNetworkTransferService::class.simpleName, "Client removed: ${client.id}, reason=$reason")
            } else {
                Log.w(ServerNetworkTransferService::class.simpleName, "Client removed: ${client.id}, reason=$reason", error)
            }
        }
    }
    
    fun shutdown() {
        try {
            serverSocket.close()
        } catch (_: Exception) {
        }
        executor.shutdownNow()
        clients.values.forEach { client ->
            try {
                client.socket.close()
            } catch (_: Exception) {
            }
        }
        clients.clear()
        Log.i(ServerNetworkTransferService::class.simpleName, "Server shutdown complete")
    }
}
