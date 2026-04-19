package pers.hpcx.aircraftwar.client

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import pers.hpcx.aircraftwar.leaderboard.CloudLeaderboardApi
import pers.hpcx.aircraftwar.leaderboard.CloudLeaderboardConfig
import pers.hpcx.aircraftwar.leaderboard.LeaderboardDatabase
import pers.hpcx.aircraftwar.leaderboard.LeaderboardRecord

class LeaderboardActivity : AppCompatActivity() {
    
    private enum class LeaderboardMode {
        LOCAL,
        WORLD,
    }
    
    private lateinit var rootContainer: FrameLayout
    private lateinit var database: LeaderboardDatabase
    private lateinit var cloudApi: CloudLeaderboardApi
    private val selectedIds = mutableSetOf<Int>()
    private var currentMode = LeaderboardMode.LOCAL
    private var worldScores: List<LeaderboardRecord> = emptyList()
    private var statusMessage: String = "本地记录可上传到云端排行榜"
    private var isBusy: Boolean = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = LeaderboardDatabase(this)
        cloudApi = CloudLeaderboardApi(this)
        rootContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }
        setContentView(rootContainer)
        render()
    }
    
    private fun render() {
        rootContainer.removeAllViews()
        rootContainer.addView(buildScreen())
    }
    
    private fun buildScreen(): View {
        val mainPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(22), dp(24), dp(22), dp(24))
        }
        
        val title = TextView(this).apply {
            text = "排行榜"
            setTextColor(Color.WHITE)
            textSize = 30f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val subtitle = TextView(this).apply {
            text = when (currentMode) {
                LeaderboardMode.LOCAL -> "本地记录可选择上传"
                LeaderboardMode.WORLD -> "云端地址：${CloudLeaderboardConfig.getBaseUrl(this@LeaderboardActivity)}"
            }
            setTextColor(Color.argb(220, 220, 220, 220))
            textSize = 13f
            gravity = Gravity.CENTER
        }
        val status = TextView(this).apply {
            text = statusMessage
            setTextColor(if (isBusy) Color.rgb(255, 193, 7) else Color.rgb(160, 220, 255))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(10), dp(8), dp(10))
        }
        
        mainPanel.addView(title)
        mainPanel.addView(
            subtitle,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(8)
            }
        )
        mainPanel.addView(
            createModeSwitcher(),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                topMargin = dp(16)
            }
        )
        mainPanel.addView(status)
        
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ).apply {
                topMargin = dp(10)
                bottomMargin = dp(18)
            }
        }
        val listPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        when (currentMode) {
            LeaderboardMode.LOCAL -> populateLocalList(listPanel)
            LeaderboardMode.WORLD -> populateWorldList(listPanel)
        }
        
        scrollView.addView(listPanel)
        mainPanel.addView(scrollView)
        mainPanel.addView(createBottomButtons())
        
        return FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(
                mainPanel,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER,
                )
            )
        }
    }
    
    private fun createModeSwitcher(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(createModeButton("本地记录", LeaderboardMode.LOCAL), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(8)
            })
            addView(createModeButton("世界排行", LeaderboardMode.WORLD), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
    }
    
    private fun createModeButton(text: String, mode: LeaderboardMode): View {
        val selected = currentMode == mode
        return AppCompatButton(this).apply {
            this.text = text
            isAllCaps = false
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(if (selected) Color.rgb(33, 150, 243) else Color.rgb(56, 56, 56))
            setPadding(dp(16), dp(14), dp(16), dp(14))
            isEnabled = !selected && !isBusy
            setOnClickListener {
                currentMode = mode
                selectedIds.clear()
                statusMessage = if (mode == LeaderboardMode.LOCAL) {
                    "本地记录可上传到云端排行榜"
                } else {
                    "正在刷新世界排行榜..."
                }
                render()
                if (mode == LeaderboardMode.WORLD) {
                    refreshWorldLeaderboard()
                }
            }
        }
    }
    
    private fun populateLocalList(container: LinearLayout) {
        val scores = database.getScores()
        if (scores.isEmpty()) {
            container.addView(createEmptyText("暂无本地记录，快去挑战吧！"))
            return
        }
        scores.forEachIndexed { index, entry ->
            container.addView(createCard(index + 1, entry))
        }
    }
    
    private fun populateWorldList(container: LinearLayout) {
        if (isBusy && worldScores.isEmpty()) {
            container.addView(createEmptyText("正在加载世界排行榜..."))
            return
        }
        if (worldScores.isEmpty()) {
            container.addView(createEmptyText("暂无云端数据，点击右下角刷新"))
            return
        }
        worldScores.forEachIndexed { index, entry ->
            container.addView(createCard(index + 1, entry))
        }
    }
    
    private fun createEmptyText(message: String): View {
        return TextView(this).apply {
            text = message
            setTextColor(Color.argb(220, 230, 230, 230))
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(40), dp(20), dp(40))
        }
    }
    
    private fun createCard(rank: Int, record: LeaderboardRecord): View {
        val selected = selectedIds.contains(record.id)
        val statusColor = if (record.uploaded) Color.rgb(76, 175, 80) else Color.rgb(255, 193, 7)
        val rankText = when (rank) {
            1 -> "🥇 #$rank"
            2 -> "🥈 #$rank"
            3 -> "🥉 #$rank"
            else -> "#$rank"
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            setBackgroundColor(
                if (selected) {
                    Color.rgb(70, 50, 30)
                } else {
                    Color.rgb(35, 35, 35)
                }
            )
            isClickable = true
            isFocusable = true
            setOnClickListener {
                if (isBusy) return@setOnClickListener
                toggleSelection(record.id)
            }
            addView(createLineText("$rankText   ${record.pairName}   ${record.totalScore}", Color.WHITE, 18f, true))
            addView(createLineText("${record.difficulty.name}   ${record.getFormattedDuration()}   ${record.getFormattedDate()}", Color.rgb(190, 220, 255), 13f))
            addView(createLineText(if (record.uploaded) "已上传" else "未上传", statusColor, 12f))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(12)
            }
        }
    }
    
    private val LeaderboardRecord.pairName: String
        get() = if (redPlayerId != null && bluePlayerId != null) "$redPlayerId | $bluePlayerId" else redPlayerId ?: bluePlayerId ?: ""
    
    private fun createLineText(text: String, color: Int, size: Float, bold: Boolean = false): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(color)
            textSize = size
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(4)
            }
        }
    }
    
    private fun createBottomButtons(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(createBackButton(), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(8)
            })
            addView(createPrimaryActionButton(), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(8)
            })
            addView(createSecondaryActionButton(), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
    }
    
    private fun createBackButton(): View {
        return AppCompatButton(this).apply {
            text = "返回"
            isAllCaps = false
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(56, 56, 56))
            setPadding(dp(18), dp(14), dp(18), dp(14))
            isEnabled = !isBusy
            setOnClickListener { finish() }
        }
    }
    
    private fun createPrimaryActionButton(): View {
        return AppCompatButton(this).apply {
            isAllCaps = false
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(dp(18), dp(14), dp(18), dp(14))
            when (currentMode) {
                LeaderboardMode.LOCAL -> {
                    text = if (selectedIds.isEmpty()) "删除选中" else "删除 ${selectedIds.size} 条"
                    setBackgroundColor(if (selectedIds.isEmpty() || isBusy) Color.rgb(80, 80, 80) else Color.rgb(180, 60, 60))
                    isEnabled = selectedIds.isNotEmpty() && !isBusy
                    setOnClickListener { deleteSelectedScores() }
                }
                LeaderboardMode.WORLD -> {
                    text = if (isBusy) "刷新中" else "刷新"
                    setBackgroundColor(Color.rgb(33, 150, 243))
                    isEnabled = !isBusy
                    setOnClickListener { refreshWorldLeaderboard() }
                }
            }
        }
    }
    
    private fun createSecondaryActionButton(): View {
        return AppCompatButton(this).apply {
            isAllCaps = false
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(dp(18), dp(14), dp(18), dp(14))
            when (currentMode) {
                LeaderboardMode.LOCAL -> {
                    val uploadableCount = database.getScores(selectedIds).size
                    text = if (selectedIds.isEmpty()) "上传选中" else "上传 $uploadableCount 条"
                    setBackgroundColor(if (uploadableCount == 0 || isBusy) Color.rgb(80, 80, 80) else Color.rgb(76, 175, 80))
                    isEnabled = uploadableCount > 0 && !isBusy
                    setOnClickListener { uploadSelectedScores() }
                }
                LeaderboardMode.WORLD -> {
                    text = "服务器"
                    setBackgroundColor(Color.rgb(76, 175, 80))
                    isEnabled = !isBusy
                    setOnClickListener { showServerAddressDialog() }
                }
            }
        }
    }
    
    private fun toggleSelection(id: Int) {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id)
        } else {
            selectedIds.add(id)
        }
        render()
    }
    
    private fun deleteSelectedScores() {
        if (selectedIds.isEmpty()) return
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除选中的 ${selectedIds.size} 条本地记录吗？")
            .setPositiveButton("确定") { _, _ ->
                selectedIds.forEach { id ->
                    database.deleteScore(id)
                }
                selectedIds.clear()
                statusMessage = "已删除选中记录"
                render()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun uploadSelectedScores() {
        if (selectedIds.isEmpty() || isBusy) return
        val entries = database.getScores(selectedIds)
        if (entries.isEmpty()) {
            Toast.makeText(this, "选中记录均不可上传", Toast.LENGTH_SHORT).show()
            return
        }
        isBusy = true
        statusMessage = "正在上传 ${entries.size} 条记录..."
        render()
        Thread {
            var successCount = 0
            var failedCount = 0
            entries.forEach { entry ->
                val response = try {
                    cloudApi.uploadRecord(entry)
                } catch (ex: Exception) {
                    Log.e(LeaderboardActivity::class.simpleName, "Failed to upload record", ex)
                    null
                }
                if (response != null && response.updated) {
                    database.markUploaded(entry.id)
                    successCount += 1
                } else {
                    database.markUploadFailed(entry.id)
                    failedCount += 1
                }
            }
            runOnUiThread {
                isBusy = false
                selectedIds.clear()
                statusMessage = "上传完成：成功 $successCount 条，失败 $failedCount 条"
                render()
            }
        }.start()
    }
    
    private fun refreshWorldLeaderboard() {
        if (isBusy) return
        isBusy = true
        statusMessage = "正在刷新世界排行榜..."
        render()
        Thread {
            try {
                val records = cloudApi.fetchWorldRecords()
                runOnUiThread {
                    worldScores = records
                    isBusy = false
                    statusMessage = if (records.isEmpty()) "云端暂无记录" else "世界排行榜已更新，共 ${records.size} 条"
                    render()
                }
            } catch (ex: Exception) {
                runOnUiThread {
                    isBusy = false
                    statusMessage = ex.message ?: "刷新失败"
                    render()
                }
            }
        }.start()
    }
    
    private fun showServerAddressDialog() {
        val input = EditText(this).apply {
            setText(CloudLeaderboardConfig.getBaseUrl(this@LeaderboardActivity))
            hint = "例如：http://your-server-ip:8080"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.argb(160, 255, 255, 255))
            setBackgroundColor(Color.rgb(40, 40, 40))
            setPadding(dp(16), dp(16), dp(16), dp(16))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        }
        AlertDialog.Builder(this)
            .setTitle("云端服务器地址")
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                val baseUrl = input.text?.toString()?.trim().orEmpty()
                if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                    Toast.makeText(this, "地址必须以 http:// 或 https:// 开头", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                CloudLeaderboardConfig.saveBaseUrl(this, baseUrl)
                statusMessage = "服务器地址已更新"
                render()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
