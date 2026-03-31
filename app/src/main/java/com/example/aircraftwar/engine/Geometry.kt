package com.example.aircraftwar.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

fun degToVec(deg: Float) = radToVec(deg / 180f * PI.toFloat())
fun radToVec(rad: Float) = Vec(cos(rad), sin(rad))

data class Vec(
    val x: Float,
    val y: Float
) {
    
    operator fun plus(that: Float): Vec = Vec(x + that, y + that)
    operator fun minus(that: Float): Vec = Vec(x - that, y - that)
    operator fun times(that: Float): Vec = Vec(x * that, y * that)
    operator fun div(that: Float): Vec = Vec(x / that, y / that)
    
    operator fun plus(that: Vec): Vec = Vec(x + that.x, y + that.y)
    operator fun minus(that: Vec): Vec = Vec(x - that.x, y - that.y)
    operator fun times(that: Vec): Vec = Vec(x * that.x, y * that.y)
    operator fun div(that: Vec): Vec = Vec(x / that.x, y / that.y)
    
    fun rotateDegree(angle: Float): Vec = rotateRadian(angle * PI.toFloat() / 180f)
    fun rotateRadian(angle: Float): Vec {
        val cos = cos(angle)
        val sin = sin(angle)
        return Vec(
            x * cos - y * sin,
            x * sin + y * cos
        )
    }
    
    fun moveTowards(target: Vec, maxDistance: Float): Vec {
        val dx = target.x - x
        val dy = target.y - y
        val dist = sqrt(dx * dx + dy * dy)
        if (dist <= maxDistance) return target
        val ratio = maxDistance / dist
        return Vec(
            x + dx * ratio,
            y + dy * ratio
        )
    }
}

data class Rect(
    val centerX: Float,
    val centerY: Float,
    val width: Float,
    val height: Float
) {
    
    val left: Float get() = centerX - width * 0.5f
    val right: Float get() = centerX + width * 0.5f
    val bottom: Float get() = centerY - height * 0.5f
    val top: Float get() = centerY + height * 0.5f
    
    fun intersects(that: Rect): Boolean = left < that.right && right > that.left && bottom < that.top && top > that.bottom
}
