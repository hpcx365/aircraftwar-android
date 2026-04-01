package com.example.aircraftwar.entity

import com.example.aircraftwar.engine.Vec
import com.example.aircraftwar.engine.degToVec

fun interface BulletFactory<B : Bullet> {
    
    fun create(position: Vec, velocity: Vec): B
}

interface ShootPattern {
    
    fun <B : Bullet> createBullets(origin: Vec, factory: BulletFactory<B>): List<B>
}

data class StraightPattern(
    val velocity: Float,
    val directionDeg: Float,
) : ShootPattern {
    
    override fun <B : Bullet> createBullets(origin: Vec, factory: BulletFactory<B>): List<B> {
        return listOf(factory.create(origin, degToVec(directionDeg) * velocity))
    }
}

data class FanPattern(
    val count: Int,
    val velocity: Float,
    val directionDeg: Float,
    val fieldDeg: Float,
) : ShootPattern {
    
    override fun <B : Bullet> createBullets(origin: Vec, factory: BulletFactory<B>): List<B> {
        return (0..<count)
            .asSequence()
            .map { it.toFloat() }
            .map { it / (count - 1) - 0.5f }
            .map { it * fieldDeg + directionDeg }
            .map { degToVec(it) * velocity }
            .map { factory.create(origin, it) }
            .toList()
    }
}

data class RadialPattern(
    val count: Int,
    val velocity: Float,
    val directionDeg: Float
) : ShootPattern {
    
    override fun <B : Bullet> createBullets(origin: Vec, factory: BulletFactory<B>): List<B> {
        return (0..<count)
            .asSequence()
            .map { it.toFloat() }
            .map { it / count }
            .map { it * 360f + directionDeg }
            .map { degToVec(it) * velocity }
            .map { factory.create(origin, it) }
            .toList()
    }
}
