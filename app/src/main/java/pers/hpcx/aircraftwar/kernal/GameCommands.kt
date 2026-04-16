package pers.hpcx.aircraftwar.kernal

import org.json.JSONObject

sealed interface GameCommand

object StartGameCommand : GameCommand

sealed interface PlayerCommand : GameCommand {
    
    val playerId: String
    val sequence: Int
}

data class PlayerJoinRedCommand(
    override val playerId: String,
    override val sequence: Int,
) : PlayerCommand {
    
    fun toJson(): String = JSONObject().apply {
        put("playerId", playerId)
        put("sequence", sequence)
    }.toString()
    
    companion object {
        
        fun fromJson(json: String): PlayerJoinRedCommand = JSONObject(json).let {
            PlayerJoinRedCommand(
                playerId = it.getString("playerId"),
                sequence = it.getInt("sequence"),
            )
        }
    }
}

data class PlayerJoinBlueCommand(
    override val playerId: String,
    override val sequence: Int,
) : PlayerCommand {
    
    fun toJson(): String = JSONObject().apply {
        put("playerId", playerId)
        put("sequence", sequence)
    }.toString()
    
    companion object {
        
        fun fromJson(json: String): PlayerJoinBlueCommand = JSONObject(json).let {
            PlayerJoinBlueCommand(
                playerId = it.getString("playerId"),
                sequence = it.getInt("sequence"),
            )
        }
    }
}

data class PlayerMoveCommand(
    override val playerId: String,
    override val sequence: Int,
    val targetPosition: Vec,
) : PlayerCommand {
    
    fun toJson(): String = JSONObject().apply {
        put("playerId", playerId)
        put("sequence", sequence)
        put("targetPosition", targetPosition.toJson())
    }.toString()
    
    companion object {
        
        fun fromJson(json: String): PlayerMoveCommand = JSONObject(json).let {
            PlayerMoveCommand(
                playerId = it.getString("playerId"),
                sequence = it.getInt("sequence"),
                targetPosition = Vec.fromJson(it.getString("targetPosition")),
            )
        }
    }
}

data class PlayerStopCommand(
    override val playerId: String,
    override val sequence: Int,
) : PlayerCommand {
    
    fun toJson(): String = JSONObject().apply {
        put("playerId", playerId)
        put("sequence", sequence)
    }.toString()
    
    companion object {
        
        fun fromJson(json: String): PlayerStopCommand = JSONObject(json).let {
            PlayerStopCommand(
                playerId = it.getString("playerId"),
                sequence = it.getInt("sequence"),
            )
        }
    }
}

data class PlayerLeaveCommand(
    override val playerId: String,
    override val sequence: Int,
) : PlayerCommand {
    
    fun toJson(): String = JSONObject().apply {
        put("playerId", playerId)
        put("sequence", sequence)
    }.toString()
    
    companion object {
        
        fun fromJson(json: String): PlayerLeaveCommand = JSONObject(json).let {
            PlayerLeaveCommand(
                playerId = it.getString("playerId"),
                sequence = it.getInt("sequence"),
            )
        }
    }
}
