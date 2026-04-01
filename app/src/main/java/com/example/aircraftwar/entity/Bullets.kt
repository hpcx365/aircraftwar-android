package com.example.aircraftwar.entity

import com.example.aircraftwar.engine.Vec

data class HeroBullet(
    override val id: Int,
    override val width: Float,
    override val height: Float,
    override var position: Vec,
    override val velocity: Vec,
    override val power: Int,
) : Bullet

data class EnemyBullet(
    override val id: Int,
    override val width: Float,
    override val height: Float,
    override var position: Vec,
    override val velocity: Vec,
    override val power: Int,
) : Bullet
