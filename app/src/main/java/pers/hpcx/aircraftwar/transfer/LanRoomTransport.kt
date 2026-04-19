package pers.hpcx.aircraftwar.transfer

import android.util.Log
import org.json.JSONObject
import pers.hpcx.aircraftwar.kernal.GameDifficulty
import java.net.*
import java.util.Collections
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

internal object LanRoomProtocol {
    
    const val UDP_DISCOVERY_PORT = 22346
    private const val PROTOCOL_VERSION = 1
    
    fun encodeAdvertisement(
        roomId: String,
        hostPlayerId: String,
        difficulty: GameDifficulty,
        tcpPort: Int,
        hostAddress: String,
    ): ByteArray {
        return JSONObject().apply {
            put("version", PROTOCOL_VERSION)
            put("roomId", roomId)
            put("hostPlayerId", hostPlayerId)
            put("difficulty", difficulty.name)
            put("tcpPort", tcpPort)
            put("hostAddress", hostAddress)
        }.toString().toByteArray(Charsets.UTF_8)
    }
    
    fun decodeAdvertisement(data: ByteArray, size: Int, sourceAddress: InetAddress): LanRoomAdvertisement? {
        return try {
            val json = JSONObject(String(data, 0, size, Charsets.UTF_8))
            if (json.optInt("version", -1) != PROTOCOL_VERSION) return null
            LanRoomAdvertisement(
                roomId = json.getString("roomId"),
                hostPlayerId = json.getString("hostPlayerId"),
                difficulty = GameDifficulty.valueOf(json.getString("difficulty")),
                tcpPort = json.getInt("tcpPort"),
                hostAddress = json.optString("hostAddress").ifBlank { sourceAddress.hostAddress ?: "" },
            )
        } catch (e: Exception) {
            Log.w("LanRoomProtocol", "Failed to decode room advertisement", e)
            null
        }
    }
    
    fun findBroadcastAddresses(): List<InetAddress> {
        val result = linkedSetOf<InetAddress>()
        result += InetAddress.getByName("255.255.255.255")
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        interfaces.forEach { networkInterface ->
            if (!networkInterface.isUp || networkInterface.isLoopback) return@forEach
            networkInterface.interfaceAddresses.forEach { address ->
                val broadcast = address.broadcast
                if (broadcast is Inet4Address) result += broadcast
            }
        }
        return result.toList()
    }
    
    fun findLocalIpv4Address(): String {
        return try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            interfaces.forEach { networkInterface ->
                if (!networkInterface.isUp || networkInterface.isLoopback) return@forEach
                Collections.list(networkInterface.inetAddresses).forEach { address ->
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        return address.hostAddress ?: "127.0.0.1"
                    }
                }
            }
            "127.0.0.1"
        } catch (e: Exception) {
            Log.w("LanRoomProtocol", "Failed to resolve local address", e)
            "127.0.0.1"
        }
    }
}

data class LanRoomAdvertisement(
    val roomId: String,
    val hostPlayerId: String,
    val difficulty: GameDifficulty,
    val tcpPort: Int,
    val hostAddress: String,
)

class LanRoomBroadcaster(
    private val hostPlayerId: String,
    private val difficulty: GameDifficulty,
    private val tcpPort: Int,
    private val intervalMs: Long = 1_000L,
) {
    
    val roomId: String = UUID.randomUUID().toString()
    
    private val running = AtomicBoolean(false)
    private var thread: Thread? = null
    
    fun start() {
        if (!running.compareAndSet(false, true)) return
        thread = Thread {
            val hostAddress = LanRoomProtocol.findLocalIpv4Address()
            val payload = LanRoomProtocol.encodeAdvertisement(
                roomId = roomId,
                hostPlayerId = hostPlayerId,
                difficulty = difficulty,
                tcpPort = tcpPort,
                hostAddress = hostAddress,
            )
            DatagramSocket().use { socket ->
                socket.broadcast = true
                while (running.get()) {
                    try {
                        LanRoomProtocol.findBroadcastAddresses().forEach { address ->
                            val packet = DatagramPacket(payload, payload.size, address, LanRoomProtocol.UDP_DISCOVERY_PORT)
                            socket.send(packet)
                        }
                    } catch (e: Exception) {
                        Log.w("LanRoomBroadcaster", "Broadcast failed", e)
                    }
                    try {
                        Thread.sleep(intervalMs)
                    } catch (_: InterruptedException) {
                        break
                    }
                }
            }
        }.apply {
            name = "lan-room-broadcaster"
            isDaemon = true
            start()
        }
    }
    
    fun shutdown() {
        if (!running.compareAndSet(true, false)) return
        thread?.interrupt()
        thread = null
    }
}

class LanRoomScanner(
    private val onRoomDiscovered: (LanRoomAdvertisement) -> Unit,
) {
    
    private val running = AtomicBoolean(false)
    private var thread: Thread? = null
    
    fun start() {
        if (!running.compareAndSet(false, true)) return
        thread = Thread {
            DatagramSocket(null).use { socket ->
                socket.reuseAddress = true
                socket.broadcast = true
                socket.bind(java.net.InetSocketAddress(LanRoomProtocol.UDP_DISCOVERY_PORT))
                socket.soTimeout = 1_000
                val buffer = ByteArray(2_048)
                while (running.get()) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket.receive(packet)
                        LanRoomProtocol.decodeAdvertisement(packet.data, packet.length, packet.address)
                            ?.let(onRoomDiscovered)
                    } catch (_: java.net.SocketTimeoutException) {
                    } catch (e: Exception) {
                        if (running.get()) {
                            Log.w("LanRoomScanner", "Scan failed", e)
                        }
                    }
                }
            }
        }.apply {
            name = "lan-room-scanner"
            isDaemon = true
            start()
        }
    }
    
    fun shutdown() {
        if (!running.compareAndSet(true, false)) return
        thread?.interrupt()
        thread = null
    }
}
