package com.example.aircraftwar.ui

import com.example.aircraftwar.entity.EntityType

data class Drawable(
    val id: Int,
    val type: EntityType,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val hp: Int? = null,
    val maxHp: Int? = null,
)

data class FrameSnapshot(
    val drawables: List<Drawable>,
    val score: Int,
    val elapsedTimeSec: Float,
    val hasBoss: Boolean,
    val gameOver: Boolean,
)
