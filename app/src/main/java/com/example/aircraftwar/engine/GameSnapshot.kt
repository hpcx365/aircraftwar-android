package com.example.aircraftwar.engine

import com.example.aircraftwar.entity.EntityType

data class EntitySnapshot(
    val id: Int,
    val type: EntityType,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val hp: Int? = null,
    val maxHp: Int? = null,
    val ownerPlayerId: PlayerId? = null,
)

data class PlayerSnapshot(
    val playerId: PlayerId,
    val heroEntityId: Int?,
    val alive: Boolean,
    val moveTarget: Vec?,
)

data class BuffSnapshot(
    val playerId: PlayerId,
    val buffType: BuffType,
    val remainingSec: Float,
)

data class GameStateSnapshot(
    val worldWidth: Float,
    val worldHeight: Float,
    val elapsedTimeSec: Float,
    val score: Int,
    val hasBoss: Boolean,
    val gameOver: Boolean,
    val entities: List<EntitySnapshot>,
    val players: List<PlayerSnapshot>,
    val activeBuffs: List<BuffSnapshot>,
)
