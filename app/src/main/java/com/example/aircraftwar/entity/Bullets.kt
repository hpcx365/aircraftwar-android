package com.example.aircraftwar.entity

import com.example.aircraftwar.engine.GameConstants
import com.example.aircraftwar.engine.Vec

data class StraightHeroBullet(
    override val id: Int,
    override var position: Vec,
    override var velocity: Vec,
    override val power: Int = 1,
) : HeroBullet {
    
    override val type = EntityType.HERO_BULLET
    override val width: Float = GameConstants.BULLET_WIDTH
    override val height: Float = GameConstants.BULLET_HEIGHT
}

data class StraightEnemyBullet(
    override val id: Int,
    override var position: Vec,
    override var velocity: Vec,
    override val power: Int = 1,
) : EnemyBullet {
    
    override val type = EntityType.ENEMY_BULLET
    override val width: Float = GameConstants.BULLET_WIDTH
    override val height: Float = GameConstants.BULLET_HEIGHT
}
