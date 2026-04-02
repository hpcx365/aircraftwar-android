package com.example.aircraftwar.engine

@JvmInline
value class PlayerId(val value: String) {
    
    companion object {
        
        val LOCAL = PlayerId("local")
    }
    
    override fun toString(): String = value
}

data class PlayerIntent(
    val moveTarget: Vec? = null,
    val primaryFirePressed: Boolean = true,
)

sealed interface GameCommand {
    
    val playerId: PlayerId
    val sequence: Long
}

data class UpdatePlayerIntent(
    override val playerId: PlayerId,
    override val sequence: Long,
    val intent: PlayerIntent,
) : GameCommand
