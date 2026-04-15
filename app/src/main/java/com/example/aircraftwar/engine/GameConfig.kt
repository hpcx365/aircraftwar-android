package com.example.aircraftwar.engine

interface GameConfig {
    
    val worldWidth: Float
    val worldHeight: Float
    val heroWidth: Float
    val heroHeight: Float
    val mobEnemyWidth: Float
    val mobEnemyHeight: Float
    val eliteEnemyWidth: Float
    val eliteEnemyHeight: Float
    val superEnemyWidth: Float
    val superEnemyHeight: Float
    val bossEnemyWidth: Float
    val bossEnemyHeight: Float
    val heroBulletWidth: Float
    val heroBulletHeight: Float
    val enemyBulletWidth: Float
    val enemyBulletHeight: Float
    val propWidth: Float
    val propHeight: Float
    
    val enemySpawnY: Float
    val enemySpawnWidth: Float
    
    val heroHp: Int
    val mobEnemyHp: Int
    val eliteEnemyHp: Int
    val superEnemyHp: Int
    val bossEnemyHp: Int
    val heroPower: Int
    val enemyPower: Int
    
    val heroMaxSpeed: Float
    val enemyMinSpeed: Float
    val enemyMaxSpeed: Float
    val heroBulletSpeed: Float
    val enemyBulletSpeed: Float
    val propFallSpeed: Float
    val superEnemyLateralSpeed: Float
    val bossEntrySpeed: Float
    val bossLateralSpeed: Float
    val bossCruiseY: Float
    
    val heroFireInterval: Float
    val enemyFireInterval: Float
    val enemySpawnInterval: Float
    
    val healthPropHealRatio: Float
    val enhanceDuration: Float
    val rampageDuration: Float
    val enhanceBulletCount: Int
    val enhanceFieldDeg: Float
    val enhanceDamageMultiplier: Int
    val enhanceFireIntervalMultiplier: Float
    val rampageBulletCount: Int
    val rampageDamageMultiplier: Int
    val rampageFireIntervalMultiplier: Float
    val bombDamageRatio: Float
    
    val mobEnemyScore: Int
    val eliteEnemyScore: Int
    val superEnemyScore: Int
    val bossEnemyScore: Int
    
    val propWeightHealth: Int
    val propWeightEnhance: Int
    val propWeightRampage: Int
    val propWeightBomb: Int
}

abstract class BaseGameConfig : GameConfig {
    
    override val worldWidth = 12f
    override val worldHeight = 20f
    override val heroWidth = 1.00f
    override val heroHeight = 1.00f
    override val mobEnemyWidth = 1.00f
    override val mobEnemyHeight = 1.00f
    override val eliteEnemyWidth = 1.50f
    override val eliteEnemyHeight = 1.20f
    override val superEnemyWidth = 2.00f
    override val superEnemyHeight = 1.80f
    override val bossEnemyWidth = 3.00f
    override val bossEnemyHeight = 2.00f
    override val heroBulletWidth = 0.20f
    override val heroBulletHeight = 0.40f
    override val enemyBulletWidth = 0.20f
    override val enemyBulletHeight = 0.40f
    override val propWidth = 0.80f
    override val propHeight = 0.80f
    
    override val enemySpawnY = 24f
    override val enemySpawnWidth = 10f
    
    override val heroHp: Int = 10
    override val heroPower: Int = 1
    
    override val heroMaxSpeed = 18f
    override val heroBulletSpeed = 15f
    override val propFallSpeed = 3.2f
    override val superEnemyLateralSpeed = 1.2f
    override val bossEntrySpeed = 1.6f
    override val bossLateralSpeed = 2.4f
    override val bossCruiseY = worldHeight * 0.75f
    
    override val healthPropHealRatio = 0.5f
    override val enhanceDuration = 3f
    override val rampageDuration = 5f
    override val enhanceBulletCount = 3
    override val enhanceFieldDeg = 30f
    override val enhanceDamageMultiplier = 2
    override val enhanceFireIntervalMultiplier = 0.75f
    override val rampageBulletCount = 16
    override val rampageDamageMultiplier = 2
    override val rampageFireIntervalMultiplier = 0.50f
    override val bombDamageRatio = 0.5f
    
    override val mobEnemyScore = 10
    override val eliteEnemyScore = 20
    override val superEnemyScore = 50
    override val bossEnemyScore = 100
    
    override val propWeightHealth = 3
    override val propWeightEnhance = 3
    override val propWeightRampage = 2
    override val propWeightBomb = 1
}

object EasyGameConfig : BaseGameConfig() {
    
    override val mobEnemyHp: Int = 1
    override val eliteEnemyHp: Int = 4
    override val superEnemyHp: Int = 8
    override val bossEnemyHp: Int = 50
    override val enemyPower: Int = 1
    
    override val enemyMinSpeed = 0.8f
    override val enemyMaxSpeed = 1.2f
    override val enemyBulletSpeed = 4f
    
    override val heroFireInterval = 0.20f
    override val enemyFireInterval = 2.40f
    override val enemySpawnInterval = 1.20f
}

object NormalGameConfig : BaseGameConfig() {
    
    override val mobEnemyHp: Int = 3
    override val eliteEnemyHp: Int = 6
    override val superEnemyHp: Int = 10
    override val bossEnemyHp: Int = 100
    override val enemyPower: Int = 1
    
    override val enemyMinSpeed = 1.0f
    override val enemyMaxSpeed = 1.4f
    override val enemyBulletSpeed = 6f
    
    override val heroFireInterval = 0.20f
    override val enemyFireInterval = 2.20f
    override val enemySpawnInterval = 1.10f
}

object HardGameConfig : BaseGameConfig() {
    
    override val mobEnemyHp: Int = 5
    override val eliteEnemyHp: Int = 8
    override val superEnemyHp: Int = 12
    override val bossEnemyHp: Int = 200
    override val enemyPower: Int = 2
    
    override val enemyMinSpeed = 1.2f
    override val enemyMaxSpeed = 1.6f
    override val enemyBulletSpeed = 8f
    
    override val heroFireInterval = 0.20f
    override val enemyFireInterval = 2.00f
    override val enemySpawnInterval = 1.00f
}
