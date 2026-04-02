package com.example.aircraftwar.ui

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
import com.example.aircraftwar.engine.GameDifficulty

class MainActivity : AppCompatActivity() {
    
    private lateinit var rootContainer: FrameLayout
    private var gameView: GameView? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rootContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }
        setContentView(rootContainer)
        showDifficultyMenu()
    }
    
    private fun showDifficultyMenu() {
        gameView = null
        rootContainer.removeAllViews()
        rootContainer.addView(buildDifficultyMenu())
    }
    
    private fun startGame(difficulty: GameDifficulty) {
        val view = GameView(this, difficulty) {
            showPostDeathPage()
        }
        gameView = view
        rootContainer.removeAllViews()
        rootContainer.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        )
    }
    
    private fun showPostDeathPage() {
        gameView = null
        rootContainer.removeAllViews()
        rootContainer.addView(buildPostDeathPage())
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
    
    private fun buildPostDeathPage(): View {
        val backButton = AppCompatButton(this).apply {
            text = "回到主菜单"
            isAllCaps = false
            gravity = Gravity.CENTER
            textSize = 18f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(56, 56, 56))
            setPadding(dp(18), dp(14), dp(18), dp(14))
            setOnClickListener { showDifficultyMenu() }
        }
        
        return FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(
                backButton,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
                ).apply {
                    leftMargin = dp(24)
                    rightMargin = dp(24)
                    bottomMargin = dp(28)
                }
            )
        }
    }
    
    private fun createDifficultyButton(difficulty: GameDifficulty): View {
        return AppCompatButton(this).apply {
            text = difficulty.displayName
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
