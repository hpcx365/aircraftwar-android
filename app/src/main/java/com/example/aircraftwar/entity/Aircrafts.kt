package com.example.aircraftwar.entity

import com.example.aircraftwar.engine.GameConstants
import com.example.aircraftwar.engine.Vec

data class Hero(
    override val id: Int,
    override var position: Vec,
    override var velocity: Vec = Vec(0f, 0f),
    override var hp: Int = GameConstants.HERO_HP,
    override var maxHp: Int = GameConstants.HERO_HP,
) : Aircraft {
    
    override val type = EntityType.HERO
    override val width = GameConstants.HERO_WIDTH
    override val height = GameConstants.HERO_HEIGHT
    override val shootingPattern = Straight(
        velocity = GameConstants.HERO_BULLET_SPEED,
        directionDeg = 90f,
    )
    
    fun increaseHp(amount: Int) {
        hp = (hp + amount).coerceAtMost(maxHp)
    }
}

data class MobEnemy(
    override val id: Int,
    override var position: Vec,
    override var velocity: Vec = Vec(0f, -1.2f),
    override var hp: Int = GameConstants.ENEMY_HP,
    override var maxHp: Int = GameConstants.ENEMY_HP,
    override val touchDamage: Int = 1,
) : Enemy {
    
    override val type = EntityType.MOB_ENEMY
    override val width = GameConstants.ENEMY_WIDTH
    override val height = GameConstants.ENEMY_HEIGHT
    override val scoreValue = 10
    override val shootingPattern = Dumb
}

data class EliteEnemy(
    override val id: Int,
    override var position: Vec,
    override var velocity: Vec = Vec(0f, -1.4f),
    override var hp: Int = GameConstants.ENEMY_HP * 2,
    override var maxHp: Int = GameConstants.ENEMY_HP * 2,
    override val touchDamage: Int = 1,
) : Enemy {
    
    override val type = EntityType.ELITE_ENEMY
    override val width = GameConstants.ENEMY_WIDTH
    override val height = GameConstants.ENEMY_HEIGHT
    override val scoreValue = 20
    override val shootingPattern = Straight(
        velocity = GameConstants.ENEMY_BULLET_SPEED,
        directionDeg = -90f,
    )
}

data class SuperEliteEnemy(
    override val id: Int,
    override var position: Vec,
    override var velocity: Vec = Vec(0f, -1.6f),
    override var hp: Int = GameConstants.ENEMY_HP * 3,
    override var maxHp: Int = GameConstants.ENEMY_HP * 3,
    override val touchDamage: Int = 1,
) : Enemy {
    
    override val type = EntityType.SUPER_ENEMY
    override val width = GameConstants.ENEMY_WIDTH
    override val height = GameConstants.ENEMY_HEIGHT
    override val scoreValue = 30
    override val shootingPattern = Fan(
        count = 4,
        velocity = GameConstants.ENEMY_BULLET_SPEED,
        directionDeg = -90f,
        fieldDeg = 30f,
    )
}

data class BossEnemy(
    override val id: Int,
    override var position: Vec,
    override var velocity: Vec = Vec(0f, -0.8f),
    override var hp: Int = GameConstants.ENEMY_HP * 20,
    override var maxHp: Int = GameConstants.ENEMY_HP * 20,
    override val touchDamage: Int = 2,
    var hovering: Boolean = false,
) : Enemy {
    
    override val type = EntityType.BOSS_ENEMY
    override val width = GameConstants.ENEMY_WIDTH * 2.2f
    override val height = GameConstants.ENEMY_HEIGHT * 2.2f
    override val scoreValue = 100
    override val shootingPattern = Radial(
        count = 8,
        velocity = GameConstants.ENEMY_BULLET_SPEED,
        directionDeg = -90f,
    )
}
