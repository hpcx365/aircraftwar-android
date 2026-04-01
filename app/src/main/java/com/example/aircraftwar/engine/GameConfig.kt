package com.example.aircraftwar.engine

interface GameConfig {
    
    val worldWidth: Float
    val worldHeight: Float
    val heroWidth: Float
    val heroHeight: Float
    val enemyWidth: Float
    val enemyHeight: Float
    val bulletWidth: Float
    val bulletHeight: Float
    val propWidth: Float
    val propHeight: Float
    
    val heroHp: Int
    val enemyHp: Int
    val heroPower: Int
    val enemyPower: Int
    
    val heroMaxSpeed: Float
    val enemyMinSpeed: Float
    val enemyMaxSpeed: Float
    val heroBulletSpeed: Float
    val enemyBulletSpeed: Float
    
    val heroFireInterval: Float
    val enemyFireInterval: Float
    val enemySpawnInterval: Float
}

object NormalGameConfig : GameConfig {
    
    override val worldWidth: Float = 12f
    override val worldHeight: Float = 20f
    override val heroWidth: Float = 0.72f
    override val heroHeight: Float = 0.72f
    override val enemyWidth: Float = 0.6f
    override val enemyHeight: Float = 0.6f
    override val bulletWidth: Float = 0.2f
    override val bulletHeight: Float = 0.4f
    override val propWidth: Float = 0.3f
    override val propHeight: Float = 0.3f
    
    override val heroHp: Int = 10
    override val enemyHp: Int = 3
    override val heroPower: Int = 1
    override val enemyPower: Int = 1
    
    override val heroMaxSpeed: Float = 18f
    override val enemyMinSpeed: Float = 1.0f
    override val enemyMaxSpeed: Float = 2.0f
    override val heroBulletSpeed: Float = 15f
    override val enemyBulletSpeed: Float = 10f
    
    override val heroFireInterval: Float = 0.25f
    override val enemyFireInterval: Float = 1.2f
    override val enemySpawnInterval: Float = 1.0f
}
