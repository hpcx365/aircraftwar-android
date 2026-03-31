package com.example.aircraftwar.entity

import com.example.aircraftwar.engine.Vec

enum class EntityType {
    
    HERO,
    MOB_ENEMY,
    ELITE_ENEMY,
    SUPER_ENEMY,
    BOSS_ENEMY,
    HERO_BULLET,
    ENEMY_BULLET,
    HEALTH_PROP,
    BOMB_PROP,
    BULLET_PROP,
    SUPER_BULLET_PROP,
}

sealed interface Entity {
    
    val id: Int
    val type: EntityType
    val width: Float
    val height: Float
    var position: Vec
    var velocity: Vec
}

sealed interface Aircraft : Entity {
    
    var hp: Int
    var maxHp: Int
    val shootingPattern: ShootingPattern
}

sealed interface Enemy : Aircraft {
    
    val scoreValue: Int
    val touchDamage: Int
}

sealed interface Prop : Entity

sealed interface Bullet : Entity {
    
    val power: Int
}

sealed interface HeroBullet : Bullet

sealed interface EnemyBullet : Bullet
