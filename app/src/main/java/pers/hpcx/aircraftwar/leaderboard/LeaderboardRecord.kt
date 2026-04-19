package pers.hpcx.aircraftwar.leaderboard

import android.annotation.SuppressLint
import org.json.JSONObject
import pers.hpcx.aircraftwar.kernal.GameDifficulty
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LeaderboardRecord(
    val id: Int,
    val redPlayerId: String?,
    val bluePlayerId: String?,
    val difficulty: GameDifficulty,
    val totalScore: Int,
    val duration: Float,
    val date: Long,
    val uploaded: Boolean,
) {
    
    init {
        require(redPlayerId != null || bluePlayerId != null) {
            "Record must have at least one player id"
        }
    }
    
    @SuppressLint("DefaultLocale")
    fun getFormattedDuration(): String = duration.toInt().let { String.format("%02d:%02d", it / 60, it % 60) }
    
    fun getFormattedDate(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(date))
    
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        if (redPlayerId != null) put("red_player_id", redPlayerId)
        if (bluePlayerId != null) put("blue_player_id", bluePlayerId)
        put("difficulty", difficulty.name)
        put("total_score", totalScore)
        put("duration", duration)
        put("date", date)
        put("uploaded", uploaded)
    }
    
    companion object {
        
        fun fromJson(json: JSONObject): LeaderboardRecord = LeaderboardRecord(
            id = json.getInt("id"),
            redPlayerId = if (json.has("red_player_id")) json.getString("red_player_id") else null,
            bluePlayerId = if (json.has("blue_player_id")) json.getString("blue_player_id") else null,
            difficulty = GameDifficulty.valueOf(json.getString("difficulty")),
            totalScore = json.getInt("total_score"),
            duration = json.getDouble("duration").toFloat(),
            date = json.getLong("date"),
            uploaded = json.getBoolean("uploaded"),
        )
    }
}
