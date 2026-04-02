package com.example.aircraftwar.engine

import com.example.aircraftwar.entity.EntityType

enum class DestroyReason {
    KILLED,
    OUT_OF_WORLD,
    PICKED_UP,
    COLLISION,
    SYSTEM,
}

enum class DamageSourceType {
    HERO_BULLET,
    ENEMY_BULLET,
    COLLISION,
    BOMB,
    UNKNOWN,
}

enum class BuffType {
    ENHANCE,
    RAMPAGE,
}

sealed interface GameEvent {
    
    val timeSec: Float
}

data class EntitySpawned(
    override val timeSec: Float,
    val entityId: Int,
    val entityType: EntityType,
    val position: Vec,
    val ownerPlayerId: PlayerId? = null,
) : GameEvent

data class EntityDestroyed(
    override val timeSec: Float,
    val entityId: Int,
    val entityType: EntityType,
    val reason: DestroyReason,
) : GameEvent

data class DamageApplied(
    override val timeSec: Float,
    val targetEntityId: Int,
    val targetEntityType: EntityType,
    val damage: Int,
    val sourceType: DamageSourceType,
    val remainingHp: Int,
) : GameEvent

data class PropCollected(
    override val timeSec: Float,
    val playerId: PlayerId,
    val heroEntityId: Int,
    val propEntityId: Int,
    val propType: EntityType,
) : GameEvent

data class ScoreChanged(
    override val timeSec: Float,
    val newScore: Int,
    val delta: Int,
    val sourceEntityId: Int,
    val sourceEntityType: EntityType,
) : GameEvent

data class BuffStateChanged(
    override val timeSec: Float,
    val playerId: PlayerId,
    val buffType: BuffType,
    val active: Boolean,
    val remainingSec: Float,
) : GameEvent

data class PlayerEliminated(
    override val timeSec: Float,
    val playerId: PlayerId,
    val heroEntityId: Int,
) : GameEvent

data class GameEnded(
    override val timeSec: Float,
) : GameEvent
