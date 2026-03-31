package com.example.aircraftwar.engine

import com.example.aircraftwar.entity.*
import com.example.aircraftwar.ui.Renderable
import kotlin.random.Random

class GameEngine {
    
    private var entityIdSeq = 0
    private val entities = mutableMapOf<Int, Entity>()
    
    private var enemySpawnTimer = 0f
    private var heroShootTimer = 0f
    private val enemyShootTimers = mutableMapOf<Int, Float>()
    
    private val pendingDamage = mutableMapOf<Int, Int>()
    private val pendingDestroy = mutableSetOf<Int>()
    private val pendingSpawn = mutableListOf<Entity>()
    
    private var hero = createHero()
    private var heroTarget = hero.position
    
    fun heroTarget(x: Float, y: Float) {
        heroTarget = Vec(
            x.coerceIn(hero.width * 0.5f, GameConstants.WORLD_WIDTH - hero.width * 0.5f),
            y.coerceIn(hero.height * 0.5f, GameConstants.WORLD_HEIGHT - hero.height * 0.5f)
        )
    }
    
    fun tick(dt: Float) {
        updateHeroMotion(dt)
        updateEntityMotion(dt)
        updateHeroShooting(dt)
        updateEnemyShooting(dt)
        updateEnemySpawn(dt)
        resolveCollisions()
        handleBoundaryCleanup()
        applyCombatResults()
        flushPendingSpawn()
    }
    
    fun capture(): List<Renderable> {
        return entities.values.map {
            Renderable(
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
        val hero = Hero(
            id = nextId(),
            position = Vec(GameConstants.WORLD_WIDTH * 0.5f, GameConstants.WORLD_HEIGHT * 0.2f)
        )
        return registerEntity(hero)
    }
    
    private fun updateHeroMotion(dt: Float) {
        val next = hero.position.moveTowards(
            target = heroTarget,
            maxDistance = GameConstants.HERO_MOVE_SPEED * dt
        )
        val safeDt = dt.coerceAtLeast(1e-4f)
        hero.velocity = Vec(
            x = (next.x - hero.position.x) / safeDt,
            y = (next.y - hero.position.y) / safeDt
        )
        hero.position = next
    }
    
    private fun updateEntityMotion(dt: Float) {
        entities.values
            .filterNot { it is Hero }
            .forEach { it.move(dt) }
    }
    
    private fun updateHeroShooting(dt: Float) {
        heroShootTimer += dt
        if (heroShootTimer < GameConstants.HERO_FIRE_INTERVAL_SEC) return
        heroShootTimer = 0f
        
        hero.shootingPattern
            .spawnBullets(hero.position) { position, velocity ->
                StraightHeroBullet(
                    id = nextId(),
                    position = position,
                    velocity = velocity,
                    power = 1,
                )
            }
            .forEach { scheduleSpawn(it) }
    }
    
    private fun updateEnemyShooting(dt: Float) {
        entities.values
            .filterIsInstance<Enemy>()
            .forEach { enemy ->
                val timer = (enemyShootTimers[enemy.id] ?: 0f) + dt
                if (timer < GameConstants.ENEMY_FIRE_INTERVAL_SEC) {
                    enemyShootTimers[enemy.id] = timer
                    return@forEach
                }
                enemyShootTimers[enemy.id] = 0f
                enemy.shootingPattern
                    .spawnBullets(enemy.position) { position, velocity ->
                        StraightEnemyBullet(
                            id = nextId(),
                            position = position,
                            velocity = velocity,
                            power = 1,
                        )
                    }
                    .forEach { scheduleSpawn(it) }
            }
    }
    
    private fun updateEnemySpawn(dt: Float) {
        enemySpawnTimer += dt
        if (enemySpawnTimer < GameConstants.ENEMY_SPAWN_INTERVAL_SEC) return
        enemySpawnTimer = 0f
        
        val x = Random.nextFloat() * (GameConstants.WORLD_WIDTH - GameConstants.ENEMY_WIDTH) + GameConstants.ENEMY_WIDTH * 0.5f
        val y = GameConstants.WORLD_HEIGHT + GameConstants.ENEMY_HEIGHT * 0.5f
        val roll = Random.nextFloat()
        val enemy: Enemy = when {
            roll < 0.60f -> {
                val speedY = randomIn(GameConstants.ENEMY_MIN_SPEED, GameConstants.ENEMY_MAX_SPEED)
                MobEnemy(
                    id = nextId(),
                    position = Vec(x, y),
                    velocity = Vec(0f, -speedY)
                )
            }
            
            roll < 0.82f -> {
                val speedY = randomIn(
                    GameConstants.ENEMY_MIN_SPEED * 0.9f,
                    GameConstants.ENEMY_MAX_SPEED * 0.95f
                )
                EliteEnemy(
                    id = nextId(),
                    position = Vec(x, y),
                    velocity = Vec(0f, -speedY)
                )
            }
            
            roll < 0.95f -> {
                val speedY = randomIn(
                    GameConstants.ENEMY_MIN_SPEED * 0.85f,
                    GameConstants.ENEMY_MAX_SPEED * 0.9f
                )
                SuperEliteEnemy(
                    id = nextId(),
                    position = Vec(x, y),
                    velocity = Vec(0f, -speedY)
                )
            }
            
            else -> {
                BossEnemy(
                    id = nextId(),
                    position = Vec(x, y),
                    velocity = Vec(0f, -0.8f)
                )
            }
        }
        registerEntity(enemy)
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
            is BulletProp      -> {}
            is SuperBulletProp -> {}
            is BombProp        -> {
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
        
        scheduleDamage(hero, enemy.touchDamage)
        scheduleDestroy(enemy)
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
        
        pendingDestroy.toList().forEach {
            val removed = entities.remove(it) ?: return@forEach
            enemyShootTimers.remove(it)
            
            if (removed is Hero) hero = createHero()
        }
        pendingDestroy.clear()
    }
    
    private fun handleBoundaryCleanup() {
        entities.values
            .filter { it.isOutOfWorld() }
            .forEach { scheduleDestroy(it) }
    }
    
    private fun flushPendingSpawn() {
        if (pendingSpawn.isEmpty()) return
        pendingSpawn.forEach { registerEntity(it) }
        pendingSpawn.clear()
    }
    
    private fun nextId(): Int = entityIdSeq++
    
    private fun <T : Entity> registerEntity(obj: T): T {
        entities[obj.id] = obj
        return obj
    }
    
    private fun scheduleDamage(target: Aircraft, damage: Int) {
        pendingDamage[target.id] = (pendingDamage[target.id] ?: 0) + damage
    }
    
    private fun scheduleDestroy(obj: Entity) {
        pendingDestroy += obj.id
    }
    
    private fun scheduleSpawn(obj: Entity) {
        pendingSpawn += obj
    }
    
    private fun randomIn(min: Float, max: Float): Float {
        return Random.nextFloat() * (max - min) + min
    }
}
