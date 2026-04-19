package pers.hpcx.aircraftwar.client

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import pers.hpcx.aircraftwar.kernal.GameDifficulty
import pers.hpcx.aircraftwar.transfer.LanRoomAdvertisement
import pers.hpcx.aircraftwar.transfer.LanRoomScanner
import java.util.concurrent.ConcurrentHashMap

class LobbySearchActivity : AppCompatActivity() {
    
    companion object {
        
        const val EXTRA_PLAYER_ID = "player_id"
        private const val ROOM_EXPIRE_MS = 3_500L
    }
    
    private data class RoomEntry(
        val room: LanRoomAdvertisement,
        val seenAt: Long,
    )
    
    private lateinit var root: FrameLayout
    private lateinit var roomListContainer: LinearLayout
    private lateinit var emptyText: TextView
    
    private val rooms = ConcurrentHashMap<String, RoomEntry>()
    private var scanner: LanRoomScanner? = null
    private var playerId: String = ""
    private val pruneTicker = object : Runnable {
        override fun run() {
            pruneExpiredRooms()
            renderRooms()
            root.postDelayed(this, 1_000L)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerId = intent.getStringExtra(EXTRA_PLAYER_ID).orEmpty()
        root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        setContentView(root)
        root.addView(buildContent())
    }
    
    override fun onStart() {
        super.onStart()
        startScan()
        root.post(pruneTicker)
    }
    
    override fun onStop() {
        super.onStop()
        root.removeCallbacks(pruneTicker)
        stopScan()
    }
    
    private fun buildContent(): View {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        
        val title = TextView(this).apply {
            text = "局域网房间搜索"
            setTextColor(Color.WHITE)
            textSize = 26f
            gravity = Gravity.CENTER_HORIZONTAL
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val subtitle = TextView(this).apply {
            text = "点击房间即可加入"
            setTextColor(Color.argb(220, 220, 220, 220))
            textSize = 15f
        }
        
        panel.addView(title)
        panel.addView(
            subtitle, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(8); bottomMargin = dp(16) })
        
        panel.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(AppCompatButton(context).apply {
                    text = "刷新"
                    isAllCaps = false
                    setOnClickListener {
                        rooms.clear()
                        renderRooms()
                        restartScan()
                    }
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(8)
                })
                addView(AppCompatButton(context).apply {
                    text = "返回"
                    isAllCaps = false
                    setOnClickListener { finish() }
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = dp(16) }
        )
        
        emptyText = TextView(this).apply {
            text = "正在搜索房间..."
            setTextColor(Color.argb(200, 255, 255, 255))
            textSize = 16f
            gravity = Gravity.CENTER_HORIZONTAL
        }
        panel.addView(
            emptyText, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = dp(12) })
        
        roomListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        val scrollView = ScrollView(this).apply {
            addView(
                roomListContainer, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            )
        }
        panel.addView(
            scrollView, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            )
        )
        
        return panel
    }
    
    private fun startScan() {
        if (scanner != null) return
        scanner = LanRoomScanner { room ->
            val now = System.currentTimeMillis()
            rooms[room.roomId] = RoomEntry(room, now)
            runOnUiThread {
                pruneExpiredRooms(now)
                renderRooms()
            }
        }.also { it.start() }
    }
    
    private fun stopScan() {
        scanner?.shutdown()
        scanner = null
    }
    
    private fun restartScan() {
        stopScan()
        startScan()
    }
    
    private fun pruneExpiredRooms(now: Long = System.currentTimeMillis()) {
        val expiredKeys = rooms.entries
            .filter { now - it.value.seenAt > ROOM_EXPIRE_MS }
            .map { it.key }
        expiredKeys.forEach(rooms::remove)
    }
    
    private fun renderRooms() {
        roomListContainer.removeAllViews()
        val sortedRooms = rooms.values
            .sortedByDescending { it.seenAt }
            .map { it.room }
        
        emptyText.visibility = if (sortedRooms.isEmpty()) View.VISIBLE else View.GONE
        emptyText.text = if (sortedRooms.isEmpty()) "未发现可加入房间，请确认房主已开启联机并停留在开始前界面" else "发现 ${sortedRooms.size} 个房间"
        
        sortedRooms.forEach { room ->
            roomListContainer.addView(createRoomCard(room))
        }
    }
    
    private fun createRoomCard(room: LanRoomAdvertisement): View {
        val difficultyText = when (room.difficulty) {
            GameDifficulty.EASY -> "简单"
            GameDifficulty.NORMAL -> "普通"
            GameDifficulty.HARD -> "困难"
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(40, 40, 40))
            setPadding(dp(16), dp(14), dp(16), dp(14))
            isClickable = true
            isFocusable = true
            setOnClickListener { joinRoom(room) }
            
            addView(TextView(context).apply {
                text = "房主：${room.hostPlayerId}"
                setTextColor(Color.WHITE)
                textSize = 18f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            addView(
                TextView(context).apply {
                    text = "难度：$difficultyText"
                    setTextColor(Color.argb(220, 230, 230, 230))
                    textSize = 15f
                }, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = dp(6) })
            addView(
                TextView(context).apply {
                    text = "地址：${room.hostAddress}:${room.tcpPort}"
                    setTextColor(Color.argb(220, 180, 220, 255))
                    textSize = 14f
                }, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = dp(4) })
            addView(
                TextView(context).apply {
                    text = "点击加入后将进入等待界面"
                    setTextColor(Color.argb(180, 255, 255, 255))
                    textSize = 13f
                }, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = dp(8) })
            
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(12)
            }
        }
    }
    
    private fun joinRoom(room: LanRoomAdvertisement) {
        stopScan()
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra(GameActivity.EXTRA_DIFFICULTY, room.difficulty.name)
            putExtra(GameActivity.EXTRA_PLAYER_ID, playerId)
            putExtra(GameActivity.EXTRA_IS_HOST, false)
            putExtra(GameActivity.EXTRA_IS_ONLINE, true)
            putExtra(GameActivity.EXTRA_SERVER_HOST, room.hostAddress)
            putExtra(GameActivity.EXTRA_SERVER_PORT, room.tcpPort)
        }
        startActivity(intent)
    }
    
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
