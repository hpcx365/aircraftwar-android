package com.example.aircraftwar.engine

import com.example.aircraftwar.entity.*

val Entity.top: Float get() = position.x + height * 0.5f
val Entity.bottom: Float get() = position.x - height * 0.5f
val Entity.left: Float get() = position.x - width * 0.5f
val Entity.right: Float get() = position.x + width * 0.5f
val Entity.bounds: Rect get() = Rect(position.x, position.y, width, height)

fun Entity.move(dt: Float) {
    position += velocity * dt
}

fun Entity.collides(that: Entity): Boolean = bounds.intersects(that.bounds)

fun Entity.isOutOfWorld(): Boolean {
    return when (this) {
        is Hero        -> false
        is Enemy       -> top < 0f
        is Prop        -> top < 0f
        is HeroBullet  -> bottom > GameConstants.WORLD_HEIGHT
        is EnemyBullet -> top < 0f || right < 0f || left > GameConstants.WORLD_WIDTH
    }
}

fun Aircraft.takeDamage(amount: Int) {
    hp = (hp - amount).coerceAtLeast(0)
}

val Aircraft.isDead: Boolean get() = hp <= 0
