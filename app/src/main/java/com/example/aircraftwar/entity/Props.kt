package com.example.aircraftwar.entity

import com.example.aircraftwar.engine.GameConstants
import com.example.aircraftwar.engine.Vec

data class HealthProp(
    override val id: Int,
    override var position: Vec,
    override var velocity: Vec = Vec(0f, -1.5f),
) : Prop {
    
    override val type = EntityType.HEALTH_PROP
    override val width: Float = GameConstants.PROP_WIDTH
    override val height: Float = GameConstants.PROP_HEIGHT
}

data class BulletProp(
    override val id: Int,
    override var position: Vec,
    override var velocity: Vec = Vec(0f, -1.5f),
) : Prop {
    
    override val type = EntityType.BULLET_PROP
    override val width: Float = GameConstants.PROP_WIDTH
    override val height: Float = GameConstants.PROP_HEIGHT
}

data class SuperBulletProp(
    override val id: Int,
    override var position: Vec,
    override var velocity: Vec = Vec(0f, -1.5f),
) : Prop {
    
    override val type = EntityType.SUPER_BULLET_PROP
    override val width: Float = GameConstants.PROP_WIDTH
    override val height: Float = GameConstants.PROP_HEIGHT
}

data class BombProp(
    override val id: Int,
    override var position: Vec,
    override var velocity: Vec = Vec(0f, -1.5f),
) : Prop {
    
    override val type = EntityType.BOMB_PROP
    override val width: Float = GameConstants.PROP_WIDTH
    override val height: Float = GameConstants.PROP_HEIGHT
}
