package com.example.aircraftwar.engine

import com.example.aircraftwar.entity.*
import com.example.aircraftwar.ui.Drawable
import kotlin.random.Random

class GameEngine(
    val config: GameConfig = NormalGameConfig
) {
    
    private var entityIdSeq = 0
    private val entities = mutableMapOf<Int, Entity>()
    
    private val pendingDamage = mutableMapOf<Int, Int>()
    private val pendingDestroy = mutableSetOf<Int>()
    
    private var enemySpawnTimer = 0f
    
    private var hero: Hero
    
    init {
        hero = createHero()
        registerEntity(hero)
    }
    
    fun heroTarget(x: Float, y: Float) {
        hero.targetPosition = Vec(
            x.coerceIn(hero.width * 0.5f, config.worldWidth - hero.width * 0.5f),
            y.coerceIn(hero.height * 0.5f, config.worldHeight - hero.height * 0.5f)
        )
    }
    
    fun tick(dt: Float) {
        heroMove(dt)
        enemyMove(dt)
        heroShoot(dt)
        enemyShoot(dt)
        enemySpawn(dt)
        resolveCollisions()
        handleBoundaryCleanup()
        applyCombatResults()
    }
    
    fun capture(): List<Drawable> {
        return entities.values.map {
            Drawable(
                id = it.id,
                type = it.type,
                x = it.position.x,
                y = it.position.y,
                width = it.width,
                height = it.height,
            )
        }
    }
    
    private fun createHero(): Hero {
        return Hero(
            id = nextId(),
            width = config.heroWidth,
            height = config.heroHeight,
            position = Vec(config.worldWidth * 0.5f, config.worldHeight * 0.2f),
            maxHp = config.heroHp,
            shootPattern = StraightPattern(
                velocity = config.heroBulletSpeed,
                directionDeg = 90f,
            ),
        )
    }
    
    private fun createMobEnemy(position: Vec, velocity: Vec): MobEnemy {
        return MobEnemy(
            id = nextId(),
            width = config.enemyWidth,
            height = config.enemyHeight,
            position = position,
            velocity = velocity,
            maxHp = config.enemyHp,
            shootPattern = null,
        )
    }
    
    private fun createEliteEnemy(position: Vec, velocity: Vec): EliteEnemy {
        return EliteEnemy(
            id = nextId(),
            width = config.enemyWidth,
            height = config.enemyHeight,
            position = position,
            velocity = velocity,
            maxHp = config.enemyHp * 2,
            shootPattern = StraightPattern(
                velocity = config.enemyBulletSpeed,
                directionDeg = -90f,
            ),
        )
    }
    
    private fun createSuperEnemy(position: Vec, velocity: Vec): SuperEnemy {
        return SuperEnemy(
            id = nextId(),
            width = config.enemyWidth,
            height = config.enemyHeight,
            position = position,
            velocity = velocity,
            maxHp = config.enemyHp * 5,
            shootPattern = FanPattern(
                count = 4,
                velocity = config.enemyBulletSpeed,
                directionDeg = -90f,
                fieldDeg = 45f,
            ),
        )
    }
    
    private fun createBossEnemy(position: Vec, velocity: Vec): BossEnemy {
        return BossEnemy(
            id = nextId(),
            width = config.enemyWidth,
            height = config.enemyHeight,
            position = position,
            velocity = velocity,
            maxHp = config.enemyHp * 10,
            shootPattern = RadialPattern(
                count = 8,
                velocity = config.enemyBulletSpeed,
                directionDeg = -90f,
            )
        )
    }
    
    private fun createStraightHeroBullet(position: Vec, velocity: Vec): HeroBullet {
        return HeroBullet(
            id = nextId(),
            width = config.bulletWidth,
            height = config.bulletHeight,
            position = position,
            velocity = velocity,
            power = config.heroPower,
        )
    }
    
    private fun createStraightEnemyBullet(position: Vec, velocity: Vec): EnemyBullet {
        return EnemyBullet(
            id = nextId(),
            width = config.bulletWidth,
            height = config.bulletHeight,
            position = position,
            velocity = velocity,
            power = config.enemyPower,
        )
    }
    
    private fun heroMove(dt: Float) {
        if (hero.isDead) return
        hero.position = hero.position.moveTowards(
            target = hero.targetPosition,
            maxDistance = config.heroMaxSpeed * dt
        )
    }
    
    private fun enemyMove(dt: Float) {
        entities.values
            .filterNot { it is Hero }
            .forEach { it.move(dt) }
    }
    
    private fun heroShoot(dt: Float) {
        if (hero.isDead) return
        
        hero.shootTimer += dt
        if (hero.shootTimer < config.heroFireInterval) return
        hero.shootTimer = 0f
        
        hero.shootPattern
            ?.createBullets(hero.position, ::createStraightHeroBullet)
            ?.forEach { registerEntity(it) }
    }
    
    private fun enemyShoot(dt: Float) {
        entities.values
            .filterIsInstance<Enemy>()
            .forEach { enemy ->
                enemy.shootTimer += dt
                if (enemy.shootTimer < config.enemyFireInterval) return@forEach
                enemy.shootTimer = 0f
                
                enemy.shootPattern
                    ?.createBullets(enemy.position, ::createStraightEnemyBullet)
                    ?.forEach { registerEntity(it) }
            }
    }
    
    private fun enemySpawn(dt: Float) {
        enemySpawnTimer += dt
        if (enemySpawnTimer < config.enemySpawnInterval) return
        enemySpawnTimer = 0f
        
        val x = Random.nextFloat() * (config.worldWidth - config.enemyWidth) + config.enemyWidth * 0.5f
        val y = config.worldHeight + config.enemyHeight * 0.5f
        val vy = randomIn(config.enemyMinSpeed, config.enemyMaxSpeed)
        val roll = Random.nextFloat()
        registerEntity(
            when {
                roll < 0.60f -> createMobEnemy(Vec(x, y), Vec(0f, -vy))
                roll < 0.82f -> createEliteEnemy(Vec(x, y), Vec(0f, -vy))
                roll < 0.95f -> createSuperEnemy(Vec(x, y), Vec(0f, -vy))
                else         -> createBossEnemy(Vec(x, y), Vec(0f, -vy))
            }
        )
    }
    
    private fun resolveCollisions() {
        val hero = hero
        val enemies = entities.values.filterIsInstance<Enemy>()
        val heroBullets = entities.values.filterIsInstance<HeroBullet>()
        val enemyBullets = entities.values.filterIsInstance<EnemyBullet>()
        val props = entities.values.filterIsInstance<Prop>()
        
        heroBullets.forEach { bullet ->
            enemies.forEach { enemy ->
                if (bullet.collides(enemy)) {
                    resolveHeroBulletHitEnemy(bullet, enemy)
                }
            }
        }
        
        enemyBullets.forEach { bullet ->
            if (bullet.collides(hero)) {
                resolveEnemyBulletHitHero(bullet, hero)
            }
        }
        
        props.forEach { prop ->
            if (prop.collides(hero)) {
                resolvePropPickup(prop, hero)
            }
        }
        
        enemies.forEach { enemy ->
            if (enemy.collides(hero)) {
                resolveAircraftCrash(hero, enemy)
            }
        }
    }
    
    private fun resolveHeroBulletHitEnemy(bullet: Bullet, enemy: Enemy) {
        if (bullet.id in pendingDestroy) return
        if (enemy.id in pendingDestroy) return
        
        scheduleDamage(enemy, bullet.power)
        scheduleDestroy(bullet)
    }
    
    private fun resolveEnemyBulletHitHero(bullet: Bullet, hero: Hero) {
        if (bullet.id in pendingDestroy) return
        if (hero.id in pendingDestroy) return
        
        scheduleDamage(hero, bullet.power)
        scheduleDestroy(bullet)
    }
    
    private fun resolvePropPickup(prop: Prop, hero: Hero) {
        if (prop.id in pendingDestroy) return
        if (hero.id in pendingDestroy) return
        
        when (prop) {
            is HealthProp      -> hero.increaseHp(3)
            is EnhanceProp -> {}
            is RampageProp -> {}
            is BombProp    -> {
                entities.values
                    .filterIsInstance<Enemy>()
                    .forEach { scheduleDestroy(it) }
                entities.values
                    .filterIsInstance<EnemyBullet>()
                    .forEach { scheduleDestroy(it) }
            }
        }
        scheduleDestroy(prop)
    }
    
    private fun resolveAircraftCrash(hero: Hero, enemy: Enemy) {
        if (hero.id in pendingDestroy) return
        if (enemy.id in pendingDestroy) return
        
        scheduleDamage(hero, enemy.hp)
        scheduleDamage(enemy, enemy.hp)
    }
    
    private fun applyCombatResults() {
        pendingDamage.forEach { (id, damage) ->
            val aircraft = entities[id] as? Aircraft ?: return@forEach
            aircraft.takeDamage(damage)
            if (aircraft.isDead) {
                scheduleDestroy(aircraft)
            }
        }
        pendingDamage.clear()
        
        pendingDestroy.forEach { entities.remove(it) }
        pendingDestroy.clear()
    }
    
    private fun handleBoundaryCleanup() {
        entities.values
            .filter { it.isOutOfWorld(config.worldWidth, config.worldHeight) }
            .forEach { scheduleDestroy(it) }
    }
    
    private fun nextId(): Int = entityIdSeq++
    
    private fun <T : Entity> registerEntity(obj: T) {
        entities[obj.id] = obj
    }
    
    private fun scheduleDamage(target: Aircraft, damage: Int) {
        pendingDamage[target.id] = (pendingDamage[target.id] ?: 0) + damage
    }
    
    private fun scheduleDestroy(obj: Entity) {
        pendingDestroy += obj.id
    }
    
    private fun randomIn(min: Float, max: Float): Float {
        return Random.nextFloat() * (max - min) + min
    }
}
