package com.example.aircraftwar.entity

import com.example.aircraftwar.engine.Vec

data class HealthProp(
    override val id: Int,
    override val width: Float,
    override val height: Float,
    override var position: Vec,
    override var velocity: Vec,
) : Prop

data class EnhanceProp(
    override val id: Int,
    override val width: Float,
    override val height: Float,
    override var position: Vec,
    override var velocity: Vec,
) : Prop

data class RampageProp(
    override val id: Int,
    override val width: Float,
    override val height: Float,
    override var position: Vec,
    override var velocity: Vec,
) : Prop

data class BombProp(
    override val id: Int,
    override val width: Float,
    override val height: Float,
    override var position: Vec,
    override var velocity: Vec,
) : Prop
