package pers.hpcx.aircraftwar.leaderboard

import org.json.JSONObject

data class UploadLeaderboardRecordResponse(
    val updated: Boolean,
    val message: String?,
) {
    
    fun toJson(): JSONObject = JSONObject().apply {
        put("updated", updated)
        if (message != null) put("message", message)
    }
    
    companion object {
        
        fun fromJson(json: JSONObject) = UploadLeaderboardRecordResponse(
            updated = json.getBoolean("updated"),
            message = if (json.has("message")) json.getString("message") else null,
        )
    }
}
