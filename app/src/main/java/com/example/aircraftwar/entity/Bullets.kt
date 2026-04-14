package com.example.aircraftwar.entity

import com.example.aircraftwar.engine.Vec

data class RedHeroBullet(
    override val id: Int,
    override val width: Float,
    override val height: Float,
    override var position: Vec,
    override val velocity: Vec,
    override val power: Int,
) : HeroBullet

data class BlueHeroBullet(
    override val id: Int,
    override val width: Float,
    override val height: Float,
    override var position: Vec,
    override val velocity: Vec,
    override val power: Int,
) : HeroBullet

data class EnemyBullet(
    override val id: Int,
    override val width: Float,
    override val height: Float,
    override var position: Vec,
    override val velocity: Vec,
    override val power: Int,
) : Bullet
