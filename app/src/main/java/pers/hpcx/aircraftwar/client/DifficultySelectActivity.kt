package pers.hpcx.aircraftwar.client

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import pers.hpcx.aircraftwar.kernal.GameDifficulty

class DifficultySelectActivity : AppCompatActivity() {
    
    private lateinit var rootContainer: FrameLayout
    private lateinit var musicToggleButton: AppCompatButton
    private var isMusicEnabled = true
    
    companion object {
        
        private const val PREFS_NAME = "game_settings"
        private const val KEY_MUSIC_ENABLED = "music_enabled"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadMusicSettings()
        rootContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }
        setContentView(rootContainer)
        showDifficultyMenu()
    }
    
    private fun showDifficultyMenu() {
        rootContainer.removeAllViews()
        rootContainer.addView(buildDifficultyMenu())
    }
    
    private fun startGame(difficulty: GameDifficulty) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra("difficulty", difficulty.name)
        }
        startActivity(intent)
    }
    
    private fun buildDifficultyMenu(): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(32), dp(28), dp(32))
        }
        
        val title = TextView(this).apply {
            text = "飞机大战"
            setTextColor(Color.WHITE)
            textSize = 30f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val subtitle = TextView(this).apply {
            text = "选择游戏难度"
            setTextColor(Color.argb(220, 230, 230, 230))
            textSize = 18f
            gravity = Gravity.CENTER
        }
        
        panel.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        )
        panel.addView(
            subtitle,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(12)
                bottomMargin = dp(28)
            }
        )
        
        GameDifficulty.entries.forEach { difficulty ->
            panel.addView(createDifficultyButton(difficulty))
        }
        
        panel.addView(createLeaderboardButton())
        panel.addView(createMusicToggleButton())
        
        return FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(
                panel,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER,
                )
            )
        }
    }
    
    private fun createLeaderboardButton(): View {
        return AppCompatButton(this).apply {
            text = "排行榜"
            isAllCaps = false
            gravity = Gravity.CENTER
            textSize = 18f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(56, 56, 56))
            setPadding(dp(18), dp(16), dp(18), dp(16))
            setOnClickListener { openLeaderboard() }
            
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(12)
            }
        }
    }
    
    private fun openLeaderboard() {
        val intent = Intent(this, LeaderboardActivity::class.java)
        startActivity(intent)
    }
    
    private fun createMusicToggleButton(): View {
        musicToggleButton = AppCompatButton(this).apply {
            text = if (isMusicEnabled) "🔊 音乐: 开" else "🔇 音乐: 关"
            isAllCaps = false
            gravity = Gravity.CENTER
            textSize = 18f
            setTextColor(Color.WHITE)
            setBackgroundColor(if (isMusicEnabled) Color.rgb(76, 175, 80) else Color.rgb(56, 56, 56))
            setPadding(dp(18), dp(16), dp(18), dp(16))
            setOnClickListener { toggleMusic() }
            
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(12)
            }
        }
        return musicToggleButton
    }
    
    private fun toggleMusic() {
        isMusicEnabled = !isMusicEnabled
        saveMusicSettings()
        musicToggleButton.text = if (isMusicEnabled) "🔊 音乐: 开" else "🔇 音乐: 关"
        musicToggleButton.setBackgroundColor(if (isMusicEnabled) Color.rgb(76, 175, 80) else Color.rgb(56, 56, 56))
    }
    
    private fun loadMusicSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        isMusicEnabled = prefs.getBoolean(KEY_MUSIC_ENABLED, true)
    }
    
    private fun saveMusicSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_MUSIC_ENABLED, isMusicEnabled).apply()
    }
    
    private fun createDifficultyButton(difficulty: GameDifficulty): View {
        return AppCompatButton(this).apply {
            text = when (difficulty) {
                GameDifficulty.EASY -> "简单"
                GameDifficulty.NORMAL -> "普通"
                GameDifficulty.HARD -> "困难"
            }
            isAllCaps = false
            gravity = Gravity.CENTER
            textSize = 18f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(56, 56, 56))
            setPadding(dp(18), dp(16), dp(18), dp(16))
            setOnClickListener { startGame(difficulty) }
            
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(12)
            }
        }
    }
    
    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
