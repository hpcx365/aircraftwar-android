package com.example.aircraftwar.entity

import com.example.aircraftwar.engine.PlayerId
import com.example.aircraftwar.engine.Vec

data class Hero(
    override val id: Int,
    val ownerPlayerId: PlayerId,
    override val width: Float,
    override val height: Float,
    override var position: Vec,
    override var maxHp: Int,
    override var hp: Int = maxHp,
    override var shootTimer: Float = 0f,
    override var shootPattern: ShootPattern?,
) : Aircraft {
    
    override val velocity: Vec = Vec(0f, 0f)
}

data class MobEnemy(
    override val id: Int,
    override val width: Float,
    override val height: Float,
    override var position: Vec,
    override val velocity: Vec,
    override val maxHp: Int,
    override var hp: Int = maxHp,
    override var shootTimer: Float = 0f,
    override val shootPattern: ShootPattern?,
) : Enemy

data class EliteEnemy(
    override val id: Int,
    override val width: Float,
    override val height: Float,
    override var position: Vec,
    override val velocity: Vec,
    override val maxHp: Int,
    override var hp: Int = maxHp,
    override var shootTimer: Float = 0f,
    override val shootPattern: ShootPattern?,
) : Enemy

data class SuperEnemy(
    override val id: Int,
    override val width: Float,
    override val height: Float,
    override var position: Vec,
    override var velocity: Vec,
    override val maxHp: Int,
    override var hp: Int = maxHp,
    override var shootTimer: Float = 0f,
    override val shootPattern: ShootPattern?,
) : Enemy

data class BossEnemy(
    override val id: Int,
    override val width: Float,
    override val height: Float,
    override var position: Vec,
    override var velocity: Vec,
    override val maxHp: Int,
    override var hp: Int = maxHp,
    override var shootTimer: Float = 0f,
    override val shootPattern: ShootPattern?,
) : Enemy
