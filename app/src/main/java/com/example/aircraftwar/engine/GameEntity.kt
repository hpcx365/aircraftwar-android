package com.example.aircraftwar.engine

import org.jbox2d.dynamics.Body

enum class EntityType {
    HERO,
    ENEMY,
    HERO_BULLET,
    ENEMY_BULLET
}

data class EntityState(
    val id: Int,
    val type: EntityType,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val hp: Int
)

class GameEntity(
    val id: Int,
    val type: EntityType,
    val body: Body,
    val width: Float,
    val height: Float,
    var hp: Int,
    val damage: Int,
    val isBullet: Boolean,
    val owner: EntityType? = null
)
