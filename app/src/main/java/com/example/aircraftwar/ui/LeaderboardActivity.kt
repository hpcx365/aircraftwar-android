package com.example.aircraftwar.ui

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
import com.example.aircraftwar.data.LeaderboardDatabase

class LeaderboardActivity : AppCompatActivity() {
    
    private lateinit var rootContainer: FrameLayout
    private lateinit var database: LeaderboardDatabase
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = LeaderboardDatabase(this)
        rootContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }
        setContentView(rootContainer)
        showLeaderboard()
    }
    
    private fun showLeaderboard() {
        rootContainer.removeAllViews()
        rootContainer.addView(buildLeaderboardScreen())
    }
    
    private fun buildLeaderboardScreen(): View {
        val mainPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(32), dp(28), dp(32))
        }
        
        val title = TextView(this).apply {
            text = "排行榜"
            setTextColor(Color.WHITE)
            textSize = 30f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        
        mainPanel.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        )
        
        // 获取排行榜数据
        val scores = database.getTopScores(20)
        
        // 创建滚动视图
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                topMargin = dp(20)
                bottomMargin = dp(20)
            }
        }
        
        val scoreListPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        
        if (scores.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "暂无记录，快来挑战吧！"
                setTextColor(Color.argb(220, 230, 230, 230))
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(dp(20), dp(40), dp(20), dp(40))
            }
            scoreListPanel.addView(emptyText)
        } else {
            // 添加表头
            scoreListPanel.addView(createHeaderRow())
            
            // 添加分数记录
            scores.forEachIndexed { index, entry ->
                scoreListPanel.addView(createScoreRow(index + 1, entry))
            }
        }
        
        scrollView.addView(scoreListPanel)
        mainPanel.addView(scrollView)
        
        // 添加按钮区域
        val buttonPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        
        buttonPanel.addView(createBackButton())
        buttonPanel.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(10), 0)
        })
        buttonPanel.addView(createClearButton())
        
        mainPanel.addView(buttonPanel)
        
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
    
    private fun createHeaderRow(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(10), dp(12), dp(10), dp(12))
            setBackgroundColor(Color.rgb(40, 40, 40))
        }.apply {
            addView(createHeaderText("排名", 0.8f))
            addView(createHeaderText("分数", 1.2f))
            addView(createHeaderText("难度", 1.2f))
            addView(createHeaderText("时长", 1.0f))
            addView(createHeaderText("日期", 2.0f))
        }
    }
    
    private fun createHeaderText(text: String, weight: Float): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(Color.rgb(255, 165, 0))
            textSize = 14f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight)
        }
    }
    
    private fun createScoreRow(rank: Int, entry: com.example.aircraftwar.data.LeaderboardEntry): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setBackgroundColor(if (rank % 2 == 0) Color.rgb(30, 30, 30) else Color.rgb(35, 35, 35))
        }.apply {
            // 排名
            addView(
                createScoreText(
                    when (rank) {
                        1 -> "🥇 $rank"
                        2 -> "🥈 $rank"
                        3 -> "🥉 $rank"
                        else -> "$rank"
                    },
                    0.8f,
                    if (rank <= 3) Color.rgb(255, 215, 0) else Color.WHITE
                )
            )
            
            // 分数
            addView(createScoreText("${entry.score}", 1.2f, Color.WHITE))
            
            // 难度
            addView(createScoreText(entry.difficulty.name, 1.2f, Color.rgb(100, 200, 255)))
            
            // 时长
            addView(createScoreText(entry.getFormattedDuration(), 1.0f, Color.rgb(200, 200, 200)))
            
            // 日期
            addView(createScoreText(entry.getFormattedDate(), 2.0f, Color.rgb(180, 180, 180)).apply {
                textSize = 11f
            })
        }
    }
    
    private fun createScoreText(text: String, weight: Float, color: Int): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(color)
            textSize = 13f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight)
        }
    }
    
    private fun createBackButton(): View {
        return AppCompatButton(this).apply {
            text = "返回主菜单"
            isAllCaps = false
            gravity = Gravity.CENTER
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(56, 56, 56))
            setPadding(dp(18), dp(14), dp(18), dp(14))
            setOnClickListener {
                finish()
            }
            
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
    }
    
    private fun createClearButton(): View {
        return AppCompatButton(this).apply {
            text = "清空记录"
            isAllCaps = false
            gravity = Gravity.CENTER
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(180, 60, 60))
            setPadding(dp(18), dp(14), dp(18), dp(14))
            setOnClickListener {
                clearAllScores()
            }
            
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
    }
    
    private fun clearAllScores() {
        android.app.AlertDialog.Builder(this)
            .setTitle("确认清空")
            .setMessage("确定要清空所有排行榜记录吗？此操作不可恢复。")
            .setPositiveButton("确定") { _, _ ->
                database.clearAllScores()
                showLeaderboard() // 刷新界面
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
