package pers.hpcx.aircraftwar.client

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import pers.hpcx.aircraftwar.kernal.FrameSnapshot
import pers.hpcx.aircraftwar.kernal.GameDifficulty
import pers.hpcx.aircraftwar.kernal.GameSession
import pers.hpcx.aircraftwar.transfer.ClientNetworkTransferService
import pers.hpcx.aircraftwar.transfer.ServerNetworkTransferService
import java.net.InetSocketAddress

class GameActivity : AppCompatActivity() {
    
    companion object {
        
        private const val LOCAL_TEST_PORT = 22345
        private const val UPDATE_INTERVAL_MS = 20L
    }
    
    @Volatile
    private var gameSession: GameSession? = null
    
    @Volatile
    private var localServerTransferService: ServerNetworkTransferService? = null
    
    @Volatile
    private var localClientTransferService: ClientNetworkTransferService? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val difficulty = GameDifficulty.valueOf(this.intent.getStringExtra("difficulty")!!)
        gameSession = GameSession(difficulty)
        
        localServerTransferService = ServerNetworkTransferService(LOCAL_TEST_PORT)
        localClientTransferService = ClientNetworkTransferService(
            InetSocketAddress("127.0.0.1", LOCAL_TEST_PORT)
        )
        
        val database = LeaderboardDatabase(this)
        val musicEnabled = loadMusicSettings()
        val gameView = GameView(
            this,
            isHost = true,
            localClientTransferService!!,
            musicEnabled,
            onGameOver = { saveScoreAndShowLeaderboard(it, database) },
            onReturnToMenu = { returnToMainMenu() }
        )
        setContentView(gameView)
        
        Thread {
            Log.i(GameActivity::class.simpleName, "Server transfer thread started")
            try {
                while (true) {
                    val session = gameSession ?: return@Thread
                    val transferService = localServerTransferService
                    if (transferService == null) {
                        Log.w(GameActivity::class.simpleName, "Server transfer service not available")
                        continue
                    }
                    val command = transferService.receiveCommand(100L) ?: continue
                    session.submitCommand(command)
                }
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
                    val transferService = localServerTransferService
                    if (transferService == null) {
                        Log.w(GameActivity::class.simpleName, "Server transfer service not available")
                        continue
                    }
                    transferService.sendSnapshot(snapshot)
                }
            } finally {
                Log.i(GameActivity::class.simpleName, "Server session thread stopped")
            }
        }.start()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        gameSession = null
        localServerTransferService?.shutdown()
        localServerTransferService = null
        localClientTransferService = null
    }
    
    private fun saveScoreAndShowLeaderboard(frame: FrameSnapshot, database: LeaderboardDatabase) {
        database.insertScore(
            score = frame.scoreRed + frame.scoreBlue,
            difficulty = frame.difficulty,
            duration = frame.elapsedTimeSec
        )
        val intent = Intent(this, LeaderboardActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun returnToMainMenu() {
        val intent = Intent(this, DifficultySelectActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun loadMusicSettings(): Boolean {
        val prefs = getSharedPreferences("game_settings", MODE_PRIVATE)
        return prefs.getBoolean("music_enabled", true)
    }
}
