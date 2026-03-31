package com.example.aircraftwar.entity

import com.example.aircraftwar.engine.Vec
import com.example.aircraftwar.engine.degToVec

fun interface BulletFactory<B : Bullet> {
    
    fun create(position: Vec, velocity: Vec): B
}

interface ShootingPattern {
    
    fun <B : Bullet> spawnBullets(origin: Vec, factory: BulletFactory<B>): List<B>
}

data object Dumb : ShootingPattern {
    
    override fun <B : Bullet> spawnBullets(origin: Vec, factory: BulletFactory<B>): List<B> = emptyList()
}

data class Straight(
    val velocity: Float,
    val directionDeg: Float,
) : ShootingPattern {
    
    override fun <B : Bullet> spawnBullets(origin: Vec, factory: BulletFactory<B>): List<B> {
        return listOf(factory.create(origin, degToVec(directionDeg) * velocity))
    }
}

data class Fan(
    val count: Int,
    val velocity: Float,
    val directionDeg: Float,
    val fieldDeg: Float,
) : ShootingPattern {
    
    override fun <B : Bullet> spawnBullets(origin: Vec, factory: BulletFactory<B>): List<B> {
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

data class Radial(
    val count: Int,
    val velocity: Float,
    val directionDeg: Float
) : ShootingPattern {
    
    override fun <B : Bullet> spawnBullets(origin: Vec, factory: BulletFactory<B>): List<B> {
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
