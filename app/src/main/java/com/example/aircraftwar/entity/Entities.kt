package com.example.aircraftwar.entity

import com.example.aircraftwar.engine.Rect
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
    ENHANCE_PROP,
    RAMPAGE_PROP,
    BOMB_PROP,
}

val Entity.type: EntityType
    get() = when (this) {
        is Hero        -> EntityType.HERO
        is MobEnemy    -> EntityType.MOB_ENEMY
        is EliteEnemy  -> EntityType.ELITE_ENEMY
        is SuperEnemy  -> EntityType.SUPER_ENEMY
        is BossEnemy   -> EntityType.BOSS_ENEMY
        is HeroBullet  -> EntityType.HERO_BULLET
        is EnemyBullet -> EntityType.ENEMY_BULLET
        is HealthProp  -> EntityType.HEALTH_PROP
        is EnhanceProp -> EntityType.ENHANCE_PROP
        is RampageProp -> EntityType.RAMPAGE_PROP
        is BombProp    -> EntityType.BOMB_PROP
    }

sealed interface Entity {
    
    val id: Int
    val width: Float
    val height: Float
    var position: Vec
    val velocity: Vec
    
    val top: Float get() = position.y + height * 0.5f
    val bottom: Float get() = position.y - height * 0.5f
    val left: Float get() = position.x - width * 0.5f
    val right: Float get() = position.x + width * 0.5f
    val bounds: Rect get() = Rect(position.x, position.y, width, height)
    
    fun move(dt: Float) {
        position += velocity * dt
    }
    
    fun collides(that: Entity): Boolean = bounds.intersects(that.bounds)
    
    fun isOutOfWorld(worldWidth: Float, worldHeight: Float): Boolean {
        return when (this) {
            is Hero        -> false
            is Enemy       -> top < 0f
            is Prop        -> top < 0f
            is HeroBullet  -> bottom > worldHeight
            is EnemyBullet -> top < 0f || right < 0f || left > worldWidth
        }
    }
}

sealed interface Aircraft : Entity {
    
    var hp: Int
    val maxHp: Int
    var shootTimer: Float
    val shootPattern: ShootPattern?
    
    fun takeDamage(amount: Int) {
        hp = (hp - amount).coerceAtLeast(0)
    }
    
    fun increaseHp(amount: Int) {
        hp = (hp + amount).coerceAtMost(maxHp)
    }
    
    val isDead: Boolean get() = hp <= 0
}

sealed interface Enemy : Aircraft

sealed interface Bullet : Entity {
    
    val power: Int
}

sealed interface Prop : Entity
