package com.example.aircraftwar.ui

import com.example.aircraftwar.engine.GameEvent
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
    val scoreRed: Int?,
    val scoreBlue: Int?,
    val elapsedTimeSec: Float,
    val hasBoss: Boolean,
    val gameOver: Boolean,
    val events: List<GameEvent>,
    val drawables: List<Drawable>,
)
