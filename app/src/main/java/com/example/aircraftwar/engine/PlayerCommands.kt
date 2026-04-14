package com.example.aircraftwar.engine

enum class PlayerCommandType {
    JOIN_RED,
    JOIN_BLUE,
    MOVE,
    STOP,
    LEAVE,
}

val PlayerCommand.type
    get() = when (this) {
        is PlayerJoinRedCommand -> PlayerCommandType.JOIN_RED
        is PlayerJoinBlueCommand -> PlayerCommandType.JOIN_BLUE
        is PlayerMoveCommand -> PlayerCommandType.MOVE
        is PlayerStopCommand -> PlayerCommandType.STOP
        is PlayerLeaveCommand -> PlayerCommandType.LEAVE
    }

sealed interface PlayerCommand {
    
    val playerId: String
    val sequence: Int
}

data class PlayerJoinRedCommand(
    override val playerId: String,
    override val sequence: Int,
) : PlayerCommand

data class PlayerJoinBlueCommand(
    override val playerId: String,
    override val sequence: Int,
) : PlayerCommand

data class PlayerMoveCommand(
    override val playerId: String,
    override val sequence: Int,
    val targetPosition: Vec,
) : PlayerCommand

data class PlayerStopCommand(
    override val playerId: String,
    override val sequence: Int,
) : PlayerCommand

data class PlayerLeaveCommand(
    override val playerId: String,
    override val sequence: Int,
) : PlayerCommand
