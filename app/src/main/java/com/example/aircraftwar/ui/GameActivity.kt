package com.example.aircraftwar.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.aircraftwar.data.LeaderboardDatabase
import com.example.aircraftwar.engine.FrameSnapshot
import com.example.aircraftwar.engine.GameDifficulty
import com.example.aircraftwar.engine.GameSession
import com.example.aircraftwar.transfer.LoopbackTransferService

class GameActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val difficulty = GameDifficulty.valueOf(this.intent.getStringExtra("difficulty")!!)
        val session = GameSession(difficulty)
        
        val localTransferService = LoopbackTransferService()
        localTransferService.setTimeout(100L)
        
        val database = LeaderboardDatabase(this)
        val gameView = GameView(this, isHost = true, localTransferService) {
            saveScoreAndShowLeaderboard(it, database)
        }
        setContentView(gameView)
        
        Thread {
            Log.d("GameActivity", "Waiting for command...")
            while (!session.isOver()) {
                val command = localTransferService.receiveCommand()
                if (command != null) {
                    session.submitCommand(command)
                    Log.d("GameActivity", "Submitted command: $command")
                }
            }
        }.start()
        
        Thread {
            Log.d("GameActivity", "Launching game session...")
            while (!session.isOver()) {
                session.update(0.02f)
                val snapshot = session.snapshot()
                localTransferService.sendSnapshot(snapshot)
                Thread.sleep(20)
            }
        }.start()
    }
    
    private fun saveScoreAndShowLeaderboard(frame: FrameSnapshot, database: LeaderboardDatabase) {
        database.insertScore(
            score = (frame.scoreRed ?: 0) + (frame.scoreBlue ?: 0),
            difficulty = frame.difficulty,
            duration = frame.elapsedTimeSec
        )
        val intent = Intent(this, LeaderboardActivity::class.java)
        startActivity(intent)
        finish()
    }
}
