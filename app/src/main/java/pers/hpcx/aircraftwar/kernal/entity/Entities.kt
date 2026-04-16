package pers.hpcx.aircraftwar.kernal.entity

import pers.hpcx.aircraftwar.kernal.EntityType
import pers.hpcx.aircraftwar.kernal.Rect
import pers.hpcx.aircraftwar.kernal.Vec

val Entity.type: EntityType
    get() = when (this) {
        is RedHero -> EntityType.RED_HERO
        is BlueHero -> EntityType.BLUE_HERO
        is MobEnemy -> EntityType.MOB_ENEMY
        is EliteEnemy -> EntityType.ELITE_ENEMY
        is SuperEnemy -> EntityType.SUPER_ENEMY
        is BossEnemy -> EntityType.BOSS_ENEMY
        is RedHeroBullet -> EntityType.RED_HERO_BULLET
        is BlueHeroBullet -> EntityType.BLUE_HERO_BULLET
        is EnemyBullet -> EntityType.ENEMY_BULLET
        is HealthProp -> EntityType.HEALTH_PROP
        is EnhanceProp -> EntityType.ENHANCE_PROP
        is RampageProp -> EntityType.RAMPAGE_PROP
        is BombProp -> EntityType.BOMB_PROP
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
}

sealed interface Aircraft : Entity {
    
    var hp: Int
    val maxHp: Int
    var shootTimer: Float
    val shootPattern: ShootPattern?
}

sealed interface Hero : Aircraft {
    
    val playerId: String
    var enhanceTimer: Float
    var rampageTimer: Float
    var targetPosition: Vec?
}

sealed interface Enemy : Aircraft

sealed interface Bullet : Entity {
    
    val power: Int
}

sealed interface HeroBullet : Bullet

sealed interface Prop : Entity
