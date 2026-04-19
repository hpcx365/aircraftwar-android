package pers.hpcx.aircraftwar.client

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SwitchCompat
import pers.hpcx.aircraftwar.kernal.GameDifficulty

class MainActivity : AppCompatActivity() {
    
    private lateinit var rootContainer: FrameLayout
    private lateinit var playerIdInput: EditText
    private lateinit var musicToggleButton: AppCompatButton
    private lateinit var onlineToggle: SwitchCompat
    
    private var isMusicEnabled = true
    private var isOnlineEnabled = false
    
    companion object {
        
        private const val PREFS_NAME = "game_settings"
        private const val KEY_MUSIC_ENABLED = "music_enabled"
        private const val KEY_ONLINE_ENABLED = "online_enabled"
        private const val KEY_PLAYER_ID = "player_id"
        private const val MAX_PLAYER_ID_LENGTH = 24
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadSettings()
        rootContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }
        setContentView(rootContainer)
        showHomeMenu()
    }
    
    private fun showHomeMenu() {
        rootContainer.removeAllViews()
        rootContainer.addView(buildHomeMenu())
    }
    
    private fun buildHomeMenu(): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(28), dp(28), dp(28))
        }
        
        val title = TextView(this).apply {
            text = "飞机大战"
            setTextColor(Color.WHITE)
            textSize = 30f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val subtitle = TextView(this).apply {
            text = "输入玩家名后可单机开始或加入局域网房间"
            setTextColor(Color.argb(220, 230, 230, 230))
            textSize = 16f
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
                bottomMargin = dp(24)
            }
        )
        
        panel.addView(createPlayerIdInput())
        panel.addView(createJoinRoomButton())
        panel.addView(createOnlineToggle())
        
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
    
    private fun createPlayerIdInput(): View {
        playerIdInput = EditText(this).apply {
            hint = "输入玩家名"
            setText(loadPlayerId())
            setHintTextColor(Color.argb(160, 255, 255, 255))
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER_VERTICAL
            setSingleLine(true)
            filters = arrayOf(InputFilter.LengthFilter(MAX_PLAYER_ID_LENGTH))
            setPadding(dp(18), dp(16), dp(18), dp(16))
            setBackgroundColor(Color.rgb(40, 40, 40))
        }
        return playerIdInput.apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(12)
            }
        }
    }
    
    private fun createJoinRoomButton(): View {
        return AppCompatButton(this).apply {
            text = "加入房间"
            isAllCaps = false
            gravity = Gravity.CENTER
            textSize = 18f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(33, 150, 243))
            setPadding(dp(18), dp(16), dp(18), dp(16))
            setOnClickListener { openRoomSearch() }
            
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(12)
            }
        }
    }
    
    private fun createOnlineToggle(): View {
        onlineToggle = SwitchCompat(this).apply {
            text = "开启联机"
            isChecked = isOnlineEnabled
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(dp(6), dp(10), dp(6), dp(10))
            setOnCheckedChangeListener { _, checked ->
                isOnlineEnabled = checked
                saveSettings()
            }
        }
        return onlineToggle.apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(8)
            }
        }
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
    
    private fun startGame(difficulty: GameDifficulty) {
        val playerId = resolvePlayerIdOrToast() ?: return
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra(GameActivity.EXTRA_DIFFICULTY, difficulty.name)
            putExtra(GameActivity.EXTRA_PLAYER_ID, playerId)
            putExtra(GameActivity.EXTRA_IS_HOST, true)
            putExtra(GameActivity.EXTRA_IS_ONLINE, isOnlineEnabled)
            putExtra(GameActivity.EXTRA_SERVER_PORT, GameActivity.DEFAULT_GAME_PORT)
        }
        startActivity(intent)
    }
    
    private fun openRoomSearch() {
        val playerId = resolvePlayerIdOrToast() ?: return
        val intent = Intent(this, LobbySearchActivity::class.java).apply {
            putExtra(LobbySearchActivity.EXTRA_PLAYER_ID, playerId)
        }
        startActivity(intent)
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
    
    private fun openLeaderboard() {
        startActivity(Intent(this, LeaderboardActivity::class.java))
    }
    
    private fun toggleMusic() {
        isMusicEnabled = !isMusicEnabled
        saveSettings()
        musicToggleButton.text = if (isMusicEnabled) "🔊 音乐: 开" else "🔇 音乐: 关"
        musicToggleButton.setBackgroundColor(if (isMusicEnabled) Color.rgb(76, 175, 80) else Color.rgb(56, 56, 56))
    }
    
    private fun resolvePlayerIdOrToast(): String? {
        val playerId = playerIdInput.text?.toString()?.trim().orEmpty()
            .replace(" ", "_")
            .take(MAX_PLAYER_ID_LENGTH)
        return if (playerId.isBlank()) {
            Toast.makeText(this, "请先输入玩家名", Toast.LENGTH_SHORT).show()
            null
        } else {
            savePlayerId(playerId)
            playerId
        }
    }
    
    private fun loadSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        isMusicEnabled = prefs.getBoolean(KEY_MUSIC_ENABLED, true)
        isOnlineEnabled = prefs.getBoolean(KEY_ONLINE_ENABLED, false)
    }
    
    private fun saveSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_MUSIC_ENABLED, isMusicEnabled)
            .putBoolean(KEY_ONLINE_ENABLED, isOnlineEnabled)
            .apply()
    }
    
    private fun loadPlayerId(): String {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return prefs.getString(KEY_PLAYER_ID, "") ?: ""
    }
    
    private fun savePlayerId(playerId: String) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit().putString(KEY_PLAYER_ID, playerId).apply()
    }
    
    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
