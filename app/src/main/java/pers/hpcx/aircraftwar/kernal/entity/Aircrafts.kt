package pers.hpcx.aircraftwar.kernal.entity

import pers.hpcx.aircraftwar.kernal.Vec

data class RedHero(
    override val id: Int,
    override val playerId: String,
    override val width: Float,
    override val height: Float,
    override var position: Vec,
    override var maxHp: Int,
    override var hp: Int = maxHp,
    override var shootTimer: Float = 0f,
    override var enhanceTimer: Float = 0f,
    override var rampageTimer: Float = 0f,
    override var targetPosition: Vec? = null,
) : Hero {
    
    override val velocity: Vec = Vec(0f, 0f)
    override val shootPattern: ShootPattern? = null
}

data class BlueHero(
    override val id: Int,
    override val playerId: String,
    override val width: Float,
    override val height: Float,
    override var position: Vec,
    override var maxHp: Int,
    override var hp: Int = maxHp,
    override var shootTimer: Float = 0f,
    override var enhanceTimer: Float = 0f,
    override var rampageTimer: Float = 0f,
    override var targetPosition: Vec? = null,
) : Hero {
    
    override val velocity: Vec = Vec(0f, 0f)
    override val shootPattern: ShootPattern? = null
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
