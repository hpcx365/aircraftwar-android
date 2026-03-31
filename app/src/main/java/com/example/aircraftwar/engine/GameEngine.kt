package com.example.aircraftwar.engine

import kotlin.math.max
import kotlin.random.Random
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.World

class GameEngine {
    private val world = World(Vec2(0f, 0f))
    private val entities = mutableMapOf<Int, GameEntity>()
    private var idSeq = 1

    private val pendingDamage = mutableMapOf<Int, Int>()
    private val pendingDestroy = mutableSetOf<Int>()

    private var hero: GameEntity
    private var enemySpawnTimer = 0f
    private var heroShootTimer = 0f
    private val enemyShootTimers = mutableMapOf<Int, Float>()

    init {
        world.setContactListener(GameCollisionListener(::handleContact))
        hero = createHero()
    }

    fun moveHero(targetX: Float) {
        val half = hero.width / 2
        val clamped = targetX.coerceIn(half, GameConstants.WORLD_WIDTH - half)
        hero.body.setTransform(Vec2(clamped, hero.body.position.y), 0f)
    }

    fun tick(dt: Float) {
        enemySpawnTimer += dt
        heroShootTimer += dt

        if (enemySpawnTimer >= GameConstants.ENEMY_SPAWN_INTERVAL_SEC) {
            spawnEnemy()
            enemySpawnTimer = 0f
        }

        if (heroShootTimer >= GameConstants.HERO_FIRE_INTERVAL_SEC) {
            shootHeroBullet()
            heroShootTimer = 0f
        }

        entities.values
            .filter { it.type == EntityType.ENEMY }
            .forEach { enemy ->
                val timer = (enemyShootTimers[enemy.id] ?: 0f) + dt
                if (timer >= GameConstants.ENEMY_FIRE_INTERVAL_SEC) {
                    shootEnemyBullet(enemy)
                    enemyShootTimers[enemy.id] = 0f
                } else {
                    enemyShootTimers[enemy.id] = timer
                }
                enemy.body.linearVelocity = Vec2(0f, -2f)
            }

        world.step(dt, 6, 2)
        handleBoundaryCleanup()
        applyCombatResults()
    }

    fun snapshot(): List<EntityState> {
        return entities.values.map {
            EntityState(
                id = it.id,
                type = it.type,
                x = it.body.position.x,
                y = it.body.position.y,
                width = it.width,
                height = it.height,
                hp = it.hp
            )
        }
    }

    private fun createHero(): GameEntity {
        return createEntity(
            type = EntityType.HERO,
            x = GameConstants.WORLD_WIDTH / 2,
            y = 2f,
            width = GameConstants.HERO_WIDTH,
            height = GameConstants.HERO_HEIGHT,
            hp = GameConstants.HERO_HP,
            damage = 1,
            isBullet = false,
            bodyType = BodyType.KINEMATIC
        )
    }

    private fun spawnEnemy() {
        createEntity(
            type = EntityType.ENEMY,
            x = Random.nextFloat() * (GameConstants.WORLD_WIDTH - 1f) + 0.5f,
            y = GameConstants.WORLD_HEIGHT - 1f,
            width = GameConstants.ENEMY_WIDTH,
            height = GameConstants.ENEMY_HEIGHT,
            hp = GameConstants.ENEMY_HP,
            damage = 1,
            isBullet = false,
            bodyType = BodyType.DYNAMIC
        )
    }

    private fun shootHeroBullet() {
        createBullet(
            type = EntityType.HERO_BULLET,
            owner = EntityType.HERO,
            x = hero.body.position.x,
            y = hero.body.position.y + hero.height,
            vy = GameConstants.HERO_BULLET_SPEED,
            damage = 1
        )
    }

    private fun shootEnemyBullet(enemy: GameEntity) {
        createBullet(
            type = EntityType.ENEMY_BULLET,
            owner = EntityType.ENEMY,
            x = enemy.body.position.x,
            y = enemy.body.position.y - enemy.height,
            vy = -GameConstants.ENEMY_BULLET_SPEED,
            damage = 1
        )
    }

    private fun createBullet(
        type: EntityType,
        owner: EntityType,
        x: Float,
        y: Float,
        vy: Float,
        damage: Int
    ) {
        val bullet = createEntity(
            type = type,
            x = x,
            y = y,
            width = GameConstants.BULLET_WIDTH,
            height = GameConstants.BULLET_HEIGHT,
            hp = 1,
            damage = damage,
            isBullet = true,
            owner = owner,
            bodyType = BodyType.KINEMATIC
        )
        bullet.body.linearVelocity = Vec2(0f, vy)
    }

    private fun createEntity(
        type: EntityType,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        hp: Int,
        damage: Int,
        isBullet: Boolean,
        owner: EntityType? = null,
        bodyType: BodyType
    ): GameEntity {
        val bodyDef = BodyDef().apply {
            this.type = bodyType
            position = Vec2(x, y)
            bullet = isBullet
        }
        val body = world.createBody(bodyDef)

        val shape = PolygonShape().apply {
            setAsBox(width / 2, height / 2)
        }
        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            density = 1f
            friction = 0f
            restitution = 0f
            isSensor = true
        }
        body.createFixture(fixtureDef)

        val entity = GameEntity(
            id = idSeq++,
            type = type,
            body = body,
            width = width,
            height = height,
            hp = hp,
            damage = damage,
            isBullet = isBullet,
            owner = owner
        )
        body.userData = entity
        entities[entity.id] = entity
        if (type == EntityType.ENEMY) {
            enemyShootTimers[entity.id] = 0f
        }
        return entity
    }

    private fun handleContact(a: GameEntity, b: GameEntity) {
        if (!canCollide(a, b)) return
        markDamage(a, b)
        markDamage(b, a)
        if (a.isBullet) pendingDestroy += a.id
        if (b.isBullet) pendingDestroy += b.id
    }

    private fun canCollide(a: GameEntity, b: GameEntity): Boolean {
        if (a.type == b.type) return false
        if (a.owner == b.type || b.owner == a.type) return false

        return when {
            a.type == EntityType.HERO_BULLET && b.type == EntityType.ENEMY -> true
            b.type == EntityType.HERO_BULLET && a.type == EntityType.ENEMY -> true
            a.type == EntityType.ENEMY_BULLET && b.type == EntityType.HERO -> true
            b.type == EntityType.ENEMY_BULLET && a.type == EntityType.HERO -> true
            a.type == EntityType.HERO && b.type == EntityType.ENEMY -> true
            b.type == EntityType.HERO && a.type == EntityType.ENEMY -> true
            else -> false
        }
    }

    private fun markDamage(target: GameEntity, source: GameEntity) {
        pendingDamage[target.id] = (pendingDamage[target.id] ?: 0) + source.damage
    }

    private fun applyCombatResults() {
        pendingDamage.forEach { (id, damage) ->
            val entity = entities[id] ?: return@forEach
            entity.hp = max(0, entity.hp - damage)
            if (entity.hp <= 0) pendingDestroy += id
        }
        pendingDamage.clear()

        pendingDestroy.toList().forEach { id ->
            val entity = entities.remove(id) ?: return@forEach
            world.destroyBody(entity.body)
            enemyShootTimers.remove(id)
            if (entity.type == EntityType.HERO) {
                hero = createHero()
            }
        }
        pendingDestroy.clear()
    }

    private fun handleBoundaryCleanup() {
        entities.values.forEach { entity ->
            val p = entity.body.position
            if (entity.type == EntityType.HERO_BULLET && p.y > GameConstants.WORLD_HEIGHT + 1) {
                pendingDestroy += entity.id
            }
            if (entity.type == EntityType.ENEMY_BULLET && p.y < -1) {
                pendingDestroy += entity.id
            }
            if (entity.type == EntityType.ENEMY && p.y < -1) {
                pendingDestroy += entity.id
            }
        }
    }
}
