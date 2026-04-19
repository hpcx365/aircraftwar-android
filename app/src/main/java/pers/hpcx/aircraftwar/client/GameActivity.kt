package pers.hpcx.aircraftwar.client

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import pers.hpcx.aircraftwar.kernal.FrameSnapshot
import pers.hpcx.aircraftwar.kernal.GameDifficulty
import pers.hpcx.aircraftwar.kernal.GameSession
import pers.hpcx.aircraftwar.leaderboard.LeaderboardDatabase
import pers.hpcx.aircraftwar.leaderboard.LeaderboardRecord
import pers.hpcx.aircraftwar.transfer.*
import java.net.InetSocketAddress

class GameActivity : AppCompatActivity() {
    
    companion object {
        
        const val DEFAULT_GAME_PORT = 22345
        const val EXTRA_DIFFICULTY = "difficulty"
        const val EXTRA_PLAYER_ID = "player_id"
        const val EXTRA_IS_HOST = "is_host"
        const val EXTRA_IS_ONLINE = "is_online"
        const val EXTRA_SERVER_HOST = "server_host"
        const val EXTRA_SERVER_PORT = "server_port"
        
        private const val UPDATE_INTERVAL_MS = 20L
    }
    
    @Volatile
    private var gameSession: GameSession? = null
    
    @Volatile
    private var serverTransferService: ServerTransferService? = null
    
    @Volatile
    private var clientTransferService: ClientTransferService? = null
    
    @Volatile
    private var roomBroadcaster: LanRoomBroadcaster? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val difficulty = GameDifficulty.valueOf(intent.getStringExtra(EXTRA_DIFFICULTY)!!)
        val isHost = intent.getBooleanExtra(EXTRA_IS_HOST, true)
        val isOnline = intent.getBooleanExtra(EXTRA_IS_ONLINE, false)
        val playerId = intent.getStringExtra(EXTRA_PLAYER_ID).orEmpty().ifBlank { "player" }
        val serverHost = intent.getStringExtra(EXTRA_SERVER_HOST).orEmpty().ifBlank { "127.0.0.1" }
        val serverPort = intent.getIntExtra(EXTRA_SERVER_PORT, DEFAULT_GAME_PORT)
        
        if (isHost) {
            startHostSession(difficulty, playerId, isOnline, serverPort)
        } else {
            startGuestSession(serverHost, serverPort)
        }
        
        val database = LeaderboardDatabase(this)
        val musicEnabled = getSharedPreferences("game_settings", MODE_PRIVATE).getBoolean("music_enabled", true)
        val gameView = GameView(
            context = this,
            isHost = isHost,
            playerId = playerId,
            transferService = clientTransferService!!,
            musicEnabled = musicEnabled,
            onGameOver = { frame ->
                if (isHost) saveScoreAndShowLeaderboard(frame, database) else returnToMainMenu()
            },
            onReturnToMenu = { returnToMainMenu() }
        )
        setContentView(gameView)
    }
    
    private fun startHostSession(difficulty: GameDifficulty, playerId: String, isOnline: Boolean, serverPort: Int) {
        gameSession = GameSession(difficulty)
        
        if (isOnline) {
            serverTransferService = ServerNetworkTransferService(serverPort)
            clientTransferService = ClientNetworkTransferService(InetSocketAddress("127.0.0.1", serverPort))
            roomBroadcaster = LanRoomBroadcaster(
                hostPlayerId = playerId,
                difficulty = difficulty,
                tcpPort = serverPort,
            ).also { it.start() }
        } else {
            val loopback = LoopbackTransferService()
            serverTransferService = loopback
            clientTransferService = loopback
        }
        
        Thread {
            Log.i(GameActivity::class.simpleName, "Server transfer thread started")
            try {
                while (true) {
                    val session = gameSession ?: return@Thread
                    val transferService = serverTransferService ?: return@Thread
                    val command = transferService.receiveCommand(100L) ?: continue
                    session.submitCommand(command)
                }
            } catch (e: Exception) {
                Log.e(GameActivity::class.simpleName, "Server transfer thread error", e)
            } finally {
                Log.i(GameActivity::class.simpleName, "Server transfer thread stopped")
            }
        }.start()
        
        Thread {
            Log.i(GameActivity::class.simpleName, "Server session thread started")
            try {
                while (true) {
                    Thread.sleep(UPDATE_INTERVAL_MS)
                    val session = gameSession ?: return@Thread
                    session.update(UPDATE_INTERVAL_MS * 0.001f)
                    val snapshot = session.snapshot()
                    if (snapshot.started) {
                        roomBroadcaster?.shutdown()
                        roomBroadcaster = null
                    }
                    val transferService = serverTransferService ?: return@Thread
                    transferService.sendSnapshot(snapshot)
                }
            } catch (e: Exception) {
                Log.e(GameActivity::class.simpleName, "Server session thread error", e)
            } finally {
                Log.i(GameActivity::class.simpleName, "Server session thread stopped")
            }
        }.start()
    }
    
    private fun startGuestSession(serverHost: String, serverPort: Int) {
        clientTransferService = ClientNetworkTransferService(
            InetSocketAddress(serverHost, serverPort)
        )
    }
    
    override fun onDestroy() {
        super.onDestroy()
        roomBroadcaster?.shutdown()
        (serverTransferService as? ServerNetworkTransferService)?.shutdown()
        (clientTransferService as? ClientNetworkTransferService)?.shutdown()
        gameSession = null
        roomBroadcaster = null
        serverTransferService = null
        clientTransferService = null
    }
    
    private fun saveScoreAndShowLeaderboard(frame: FrameSnapshot, database: LeaderboardDatabase) {
        database.insertScore(
            LeaderboardRecord(
                id = -1,
                redPlayerId = frame.redPlayerId,
                bluePlayerId = frame.bluePlayerId,
                difficulty = frame.difficulty,
                totalScore = frame.scoreRed + frame.scoreBlue,
                duration = frame.elapsedTimeSec,
                date = System.currentTimeMillis(),
                uploaded = false,
            )
        )
        val intent = Intent(this, LeaderboardActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun returnToMainMenu() {
        (clientTransferService as? ClientNetworkTransferService)?.shutdown()
        roomBroadcaster?.shutdown()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
