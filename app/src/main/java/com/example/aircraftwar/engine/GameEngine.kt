package com.example.aircraftwar.engine

import com.example.aircraftwar.entity.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

class GameEngine(
    val config: GameConfig = NormalGameConfig
) {
    
    private enum class PropKind {
        HEALTH,
        ENHANCE,
        RAMPAGE,
        BOMB,
    }
    
    private data class PendingDamage(
        var totalDamage: Int = 0,
        var sourceType: DamageSourceType = DamageSourceType.UNKNOWN,
    )
    
    private data class PlayerRuntimeState(
        var heroId: Int? = null,
        var lastSequence: Long = Long.MIN_VALUE,
        var intent: PlayerIntent = PlayerIntent(),
    )
    
    private var entityIdSeq = 0
    private val entities = linkedMapOf<Int, Entity>()
    private val players = linkedMapOf<PlayerId, PlayerRuntimeState>()
    
    private val pendingDamage = mutableMapOf<Int, PendingDamage>()
    private val pendingDestroyReasons = mutableMapOf<Int, DestroyReason>()
    private val pendingEvents = mutableListOf<GameEvent>()
    private val recentlyDestroyedEntityTypes = mutableMapOf<Int, EntityType>()
    
    private val commandLock = Any()
    private val commandQueue = ArrayDeque<GameCommand>()
    
    private var enemySpawnTimer = 0f
    private var heroEnhanceTimer = 0f
    private var heroRampageTimer = 0f
    private var score = 0
    private var elapsedTimeSec = 0f
    
    private var lastEnhanceActive = false
    private var lastRampageActive = false
    
    @Volatile
    private var gameOver = false
    
    private var hero: Hero?
    
    init {
        val hero = createHero(PlayerId.LOCAL)
        registerEntity(hero)
        this.hero = hero
    }
    
    fun submitCommand(command: GameCommand) {
        synchronized(commandLock) {
            commandQueue.add(command)
        }
    }
    
    fun tick(dt: Float) {
        if (gameOver) return
        
        val safeDt = dt.coerceAtLeast(0f)
        elapsedTimeSec += safeDt
        applyPendingCommands()
        updateHeroBuffs(safeDt)
        entityMove(safeDt)
        aircraftShoot(safeDt)
        enemySpawn(safeDt)
        resolveCollisions()
        handleBoundaryCleanup()
        applyCombatResults()
        emitBuffStateChanges()
    }
    
    fun captureState(): GameStateSnapshot {
        val hero = hero
        return GameStateSnapshot(
            worldWidth = config.worldWidth,
            worldHeight = config.worldHeight,
            elapsedTimeSec = elapsedTimeSec,
            score = score,
            hasBoss = entities.values.any { it is BossEnemy && it.id !in pendingDestroyReasons },
            gameOver = hero == null || gameOver,
            entities = entities.values.map { entity ->
                val aircraft = entity as? Aircraft
                EntitySnapshot(
                    id = entity.id,
                    type = entity.type,
                    x = entity.position.x,
                    y = entity.position.y,
                    width = entity.width,
                    height = entity.height,
                    hp = aircraft?.hp,
                    maxHp = aircraft?.maxHp,
                    ownerPlayerId = (entity as? Hero)?.ownerPlayerId,
                )
            },
            players = players.map { (playerId, runtime) ->
                PlayerSnapshot(
                    playerId = playerId,
                    heroEntityId = runtime.heroId,
                    alive = runtime.heroId != null && entities.containsKey(runtime.heroId),
                    moveTarget = runtime.intent.moveTarget,
                )
            },
            activeBuffs = buildList {
                if (heroEnhanceTimer > 0f) {
                    add(BuffSnapshot(PlayerId.LOCAL, BuffType.ENHANCE, heroEnhanceTimer))
                }
                if (heroRampageTimer > 0f) {
                    add(BuffSnapshot(PlayerId.LOCAL, BuffType.RAMPAGE, heroRampageTimer))
                }
            }
        )
    }
    
    fun pollEvents(): List<GameEvent> {
        if (pendingEvents.isEmpty()) return emptyList()
        val copy = pendingEvents.toList()
        pendingEvents.clear()
        return copy
    }
    
    fun isGameOver(): Boolean = gameOver
    
    private fun applyPendingCommands() {
        val drained = mutableListOf<GameCommand>()
        synchronized(commandLock) {
            while (commandQueue.isNotEmpty()) {
                drained += commandQueue.removeFirst()
            }
        }
        drained.forEach { command ->
            val runtime = players.getOrPut(command.playerId) { PlayerRuntimeState() }
            if (command.sequence <= runtime.lastSequence) return@forEach
            runtime.lastSequence = command.sequence
            when (command) {
                is UpdatePlayerIntent -> runtime.intent = command.intent
            }
        }
    }
    
    private fun createHero(playerId: PlayerId): Hero {
        return Hero(
            id = nextId(),
            ownerPlayerId = playerId,
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
            width = config.mobEnemyWidth,
            height = config.mobEnemyHeight,
            position = position,
            velocity = velocity,
            maxHp = config.mobEnemyHp,
            shootPattern = null,
        )
    }
    
    private fun createEliteEnemy(position: Vec, velocity: Vec): EliteEnemy {
        return EliteEnemy(
            id = nextId(),
            width = config.eliteEnemyWidth,
            height = config.eliteEnemyHeight,
            position = position,
            velocity = velocity,
            maxHp = config.eliteEnemyHp,
            shootPattern = StraightPattern(
                velocity = config.enemyBulletSpeed,
                directionDeg = -90f,
            ),
        )
    }
    
    private fun createSuperEnemy(position: Vec, downwardSpeed: Float): SuperEnemy {
        val horizontalDirection = if (Random.nextBoolean()) 1f else -1f
        return SuperEnemy(
            id = nextId(),
            width = config.superEnemyWidth,
            height = config.superEnemyHeight,
            position = position,
            velocity = Vec(
                config.superEnemyLateralSpeed * horizontalDirection,
                -downwardSpeed,
            ),
            maxHp = config.superEnemyHp,
            shootPattern = FanPattern(
                count = 4,
                velocity = config.enemyBulletSpeed,
                directionDeg = -90f,
                fieldDeg = 45f,
            ),
        )
    }
    
    private fun createBossEnemy(): BossEnemy {
        return BossEnemy(
            id = nextId(),
            width = config.bossEnemyWidth,
            height = config.bossEnemyHeight,
            position = Vec(config.worldWidth * 0.5f, config.enemySpawnY),
            velocity = Vec(0f, -config.bossEntrySpeed),
            maxHp = config.bossEnemyHp,
            shootPattern = RadialPattern(
                count = 8,
                velocity = config.enemyBulletSpeed,
                directionDeg = -90f,
            )
        )
    }
    
    private fun createHeroBullet(position: Vec, velocity: Vec, power: Int): HeroBullet {
        return HeroBullet(
            id = nextId(),
            width = config.heroBulletWidth,
            height = config.heroBulletHeight,
            position = position,
            velocity = velocity,
            power = power,
        )
    }
    
    private fun createEnemyBullet(position: Vec, velocity: Vec): EnemyBullet {
        return EnemyBullet(
            id = nextId(),
            width = config.enemyBulletWidth,
            height = config.enemyBulletHeight,
            position = position,
            velocity = velocity,
            power = config.enemyPower,
        )
    }
    
    private fun createHealthProp(position: Vec): HealthProp {
        return HealthProp(
            id = nextId(),
            width = config.propWidth,
            height = config.propHeight,
            position = position,
            velocity = Vec(0f, -config.propFallSpeed),
        )
    }
    
    private fun createEnhanceProp(position: Vec): EnhanceProp {
        return EnhanceProp(
            id = nextId(),
            width = config.propWidth,
            height = config.propHeight,
            position = position,
            velocity = Vec(0f, -config.propFallSpeed),
        )
    }
    
    private fun createRampageProp(position: Vec): RampageProp {
        return RampageProp(
            id = nextId(),
            width = config.propWidth,
            height = config.propHeight,
            position = position,
            velocity = Vec(0f, -config.propFallSpeed),
        )
    }
    
    private fun createBombProp(position: Vec): BombProp {
        return BombProp(
            id = nextId(),
            width = config.propWidth,
            height = config.propHeight,
            position = position,
            velocity = Vec(0f, -config.propFallSpeed),
        )
    }
    
    private fun moveSuperEnemy(enemy: SuperEnemy, dt: Float) {
        enemy.move(dt)
        bounceEnemyOnHorizontalBounds(enemy)
    }
    
    private fun moveBossEnemy(enemy: BossEnemy, dt: Float) {
        val centerX = config.worldWidth * 0.5f
        val cruiseY = config.bossCruiseY
        if (enemy.velocity.y < 0f) {
            enemy.position = Vec(centerX, enemy.position.y)
            enemy.move(dt)
            if (enemy.position.y <= cruiseY) {
                enemy.position = Vec(centerX, cruiseY)
                enemy.velocity = Vec(config.bossLateralSpeed, 0f)
            } else {
                enemy.position = Vec(centerX, enemy.position.y)
            }
            return
        }
        
        enemy.position = Vec(enemy.position.x, cruiseY)
        enemy.move(dt)
        bounceEnemyOnHorizontalBounds(enemy)
        enemy.position = Vec(enemy.position.x, cruiseY)
        if (enemy.velocity.x == 0f) {
            enemy.velocity = Vec(config.bossLateralSpeed, 0f)
        }
    }
    
    private fun bounceEnemyOnHorizontalBounds(enemy: Enemy) {
        val minX = enemy.width * 0.5f
        val maxX = config.worldWidth - enemy.width * 0.5f
        val x = enemy.position.x
        when {
            x < minX -> {
                val reflectedX = minX + (minX - x)
                val nextVelocityX = abs(enemy.velocity.x).coerceAtLeast(0.01f)
                setEnemyPositionAndVelocity(enemy, reflectedX, enemy.position.y, nextVelocityX, enemy.velocity.y)
            }
            
            x > maxX -> {
                val reflectedX = maxX - (x - maxX)
                val nextVelocityX = -abs(enemy.velocity.x).coerceAtLeast(0.01f)
                setEnemyPositionAndVelocity(enemy, reflectedX, enemy.position.y, nextVelocityX, enemy.velocity.y)
            }
        }
    }
    
    private fun setEnemyPositionAndVelocity(enemy: Enemy, x: Float, y: Float, vx: Float, vy: Float) {
        when (enemy) {
            is SuperEnemy -> {
                enemy.position = Vec(x, y)
                enemy.velocity = Vec(vx, vy)
            }
            
            is BossEnemy -> {
                enemy.position = Vec(x, y)
                enemy.velocity = Vec(vx, vy)
            }
            
            else         -> Unit
        }
    }
    
    private fun entityMove(dt: Float) {
        val hero = hero
        if (hero != null) {
            val runtime = players[hero.ownerPlayerId]
            val target = runtime?.intent?.moveTarget ?: hero.position
            val clampedTarget = Vec(
                target.x.coerceIn(hero.width * 0.5f, config.worldWidth - hero.width * 0.5f),
                target.y.coerceIn(hero.height * 0.5f, config.worldHeight - hero.height * 0.5f)
            )
            hero.position = hero.position.moveTowards(
                target = clampedTarget,
                maxDistance = config.heroMaxSpeed * dt
            )
        }
        
        entities.values
            .filterNot { it is Hero }
            .forEach { entity ->
                when (entity) {
                    is SuperEnemy -> moveSuperEnemy(entity, dt)
                    is BossEnemy  -> moveBossEnemy(entity, dt)
                    else          -> entity.move(dt)
                }
            }
    }
    
    private fun aircraftShoot(dt: Float) {
        val hero = hero
        if (hero != null) {
            val runtime = players[hero.ownerPlayerId]
            if (runtime?.intent?.primaryFirePressed != false) {
                val interval = currentHeroFireInterval().coerceAtLeast(0.01f)
                hero.shootTimer += dt
                while (hero.shootTimer >= interval) {
                    hero.shootTimer -= interval
                    currentHeroShootPattern(hero)
                        .createBullets(hero.position) { position, velocity ->
                            createHeroBullet(position, velocity, currentHeroBulletPower())
                        }
                        .forEach { registerEntity(it) }
                }
            }
        }
        
        entities.values
            .filterIsInstance<Enemy>()
            .forEach { enemy ->
                enemy.shootTimer += dt
                while (enemy.shootTimer >= config.enemyFireInterval) {
                    enemy.shootTimer -= config.enemyFireInterval
                    enemy.shootPattern
                        ?.createBullets(enemy.position, ::createEnemyBullet)
                        ?.forEach { registerEntity(it) }
                }
            }
    }
    
    private fun currentHeroShootPattern(hero: Hero): ShootPattern {
        return when {
            heroRampageTimer > 0f -> RadialPattern(
                count = config.rampageBulletCount,
                velocity = config.heroBulletSpeed,
                directionDeg = 90f,
            )
            
            heroEnhanceTimer > 0f -> FanPattern(
                count = config.enhanceBulletCount,
                velocity = config.heroBulletSpeed,
                directionDeg = 90f,
                fieldDeg = config.enhanceFieldDeg,
            )
            
            else -> hero.shootPattern ?: StraightPattern(
                velocity = config.heroBulletSpeed,
                directionDeg = 90f,
            )
        }
    }
    
    private fun currentHeroBulletPower(): Int {
        val multiplier = when {
            heroRampageTimer > 0f -> config.rampageDamageMultiplier
            heroEnhanceTimer > 0f -> config.enhanceDamageMultiplier
            else -> 1
        }
        return (config.heroPower * multiplier).coerceAtLeast(1)
    }
    
    private fun currentHeroFireInterval(): Float {
        return if (heroRampageTimer > 0f) {
            config.heroFireInterval * config.rampageFireIntervalMultiplier
        } else {
            config.heroFireInterval
        }
    }
    
    private fun updateHeroBuffs(dt: Float) {
        heroEnhanceTimer = (heroEnhanceTimer - dt).coerceAtLeast(0f)
        heroRampageTimer = (heroRampageTimer - dt).coerceAtLeast(0f)
    }
    
    private fun enemySpawn(dt: Float) {
        enemySpawnTimer += dt
        if (enemySpawnTimer < config.enemySpawnInterval) return
        enemySpawnTimer = 0f
        
        val x = 0.5f * config.worldWidth + (Random.nextFloat() * 2f - 1f) * 0.5f * config.enemySpawnWidth
        val y = config.enemySpawnY
        val vy = randomIn(config.enemyMinSpeed, config.enemyMaxSpeed)
        val roll = Random.nextFloat()
        val hasBoss = entities.values.any { it is BossEnemy && it.id !in pendingDestroyReasons }
        registerEntity(
            when {
                roll < 0.60f -> createMobEnemy(Vec(x, y), Vec(0f, -vy))
                roll < 0.82f -> createEliteEnemy(Vec(x, y), Vec(0f, -vy))
                roll < 0.95f -> createSuperEnemy(Vec(x, y), vy)
                !hasBoss     -> createBossEnemy()
                else         -> createSuperEnemy(Vec(x, y), vy)
            }
        )
    }
    
    private fun resolveCollisions() {
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
        
        val hero = hero
        if (hero != null) {
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
    }
    
    private fun resolveHeroBulletHitEnemy(bullet: Bullet, enemy: Enemy) {
        if (bullet.id in pendingDestroyReasons) return
        if (enemy.id in pendingDestroyReasons) return
        
        scheduleDamage(enemy, bullet.power, DamageSourceType.HERO_BULLET)
        scheduleDestroy(bullet, DestroyReason.COLLISION)
    }
    
    private fun resolveEnemyBulletHitHero(bullet: Bullet, hero: Hero) {
        if (bullet.id in pendingDestroyReasons) return
        if (hero.id in pendingDestroyReasons) return
        
        scheduleDamage(hero, bullet.power, DamageSourceType.ENEMY_BULLET)
        scheduleDestroy(bullet, DestroyReason.COLLISION)
    }
    
    private fun resolvePropPickup(prop: Prop, hero: Hero) {
        if (prop.id in pendingDestroyReasons) return
        if (hero.id in pendingDestroyReasons) return
        
        emitEvent(
            PropCollected(
                timeSec = elapsedTimeSec,
                playerId = hero.ownerPlayerId,
                heroEntityId = hero.id,
                propEntityId = prop.id,
                propType = prop.type,
            )
        )
        
        when (prop) {
            is HealthProp -> {
                val heal = (hero.maxHp * config.healthPropHealRatio).roundToInt().coerceAtLeast(1)
                hero.increaseHp(heal)
            }
            
            is EnhanceProp -> {
                heroEnhanceTimer = config.enhanceDuration
            }
            
            is RampageProp -> {
                heroRampageTimer = config.rampageDuration
            }
            
            is BombProp   -> {
                val bombDamage = (config.bossEnemyHp * config.bombDamageRatio).roundToInt().coerceAtLeast(1)
                entities.values
                    .filterIsInstance<Enemy>()
                    .forEach { scheduleDamage(it, bombDamage, DamageSourceType.BOMB) }
                entities.values
                    .filterIsInstance<EnemyBullet>()
                    .forEach { scheduleDestroy(it, DestroyReason.SYSTEM) }
            }
        }
        scheduleDestroy(prop, DestroyReason.PICKED_UP)
    }
    
    private fun resolveAircraftCrash(hero: Hero, enemy: Enemy) {
        if (hero.id in pendingDestroyReasons) return
        if (enemy.id in pendingDestroyReasons) return
        
        scheduleDamage(hero, enemy.hp, DamageSourceType.COLLISION)
        scheduleDamage(enemy, enemy.hp, DamageSourceType.COLLISION)
    }
    
    private fun applyCombatResults() {
        val deadEnemies = mutableListOf<Enemy>()
        pendingDamage.forEach { (id, damageInfo) ->
            val aircraft = entities[id] as? Aircraft ?: return@forEach
            aircraft.takeDamage(damageInfo.totalDamage)
            emitEvent(
                DamageApplied(
                    timeSec = elapsedTimeSec,
                    targetEntityId = aircraft.id,
                    targetEntityType = aircraft.type,
                    damage = damageInfo.totalDamage,
                    sourceType = damageInfo.sourceType,
                    remainingHp = aircraft.hp,
                )
            )
            if (aircraft.isDead) {
                if (aircraft is Enemy) {
                    deadEnemies += aircraft
                }
                scheduleDestroy(aircraft, DestroyReason.KILLED)
            }
        }
        pendingDamage.clear()
        
        deadEnemies.forEach { enemy ->
            val delta = scoreForEnemy(enemy)
            score += delta
            emitEvent(
                ScoreChanged(
                    timeSec = elapsedTimeSec,
                    newScore = score,
                    delta = delta,
                    sourceEntityId = enemy.id,
                    sourceEntityType = enemy.type,
                )
            )
            dropPropsForEnemy(enemy)
        }
        
        val destroyed = pendingDestroyReasons.toMap()
        pendingDestroyReasons.keys.forEach { entities.remove(it) }
        pendingDestroyReasons.clear()
        
        destroyed.forEach { (entityId, reason) ->
            val entityType = heroEntityTypeFallback(entityId) ?: return@forEach
            emitEvent(
                EntityDestroyed(
                    timeSec = elapsedTimeSec,
                    entityId = entityId,
                    entityType = entityType,
                    reason = reason,
                )
            )
        }
        
        val hero = hero
        if (hero != null && !entities.containsKey(hero.id)) {
            this.hero = null
            players[hero.ownerPlayerId]?.heroId = null
            heroEnhanceTimer = 0f
            heroRampageTimer = 0f
            emitEvent(PlayerEliminated(elapsedTimeSec, hero.ownerPlayerId, hero.id))
            if (!gameOver) {
                gameOver = true
                emitEvent(GameEnded(elapsedTimeSec))
            }
        }
    }
    
    private fun heroEntityTypeFallback(entityId: Int): EntityType? {
        return when {
            entityId == hero?.id -> EntityType.HERO
            else                 -> recentlyDestroyedEntityTypes.remove(entityId)
        }
    }
    
    private fun dropPropsForEnemy(enemy: Enemy) {
        when (enemy) {
            is EliteEnemy -> {
                registerEntity(createPropByKind(randomElitePropKind(), propSpawnPosition(enemy)))
            }
            
            is SuperEnemy -> {
                repeat(Random.nextInt(1, 3)) {
                    registerEntity(createPropByKind(randomAnyPropKind(), propSpawnPosition(enemy)))
                }
            }
            
            is BossEnemy -> {
                repeat(3) {
                    registerEntity(createPropByKind(randomAnyPropKind(), propSpawnPosition(enemy)))
                }
            }
            
            else         -> Unit
        }
    }
    
    private fun propSpawnPosition(enemy: Enemy): Vec {
        val x = (enemy.position.x + randomIn(-enemy.width * 0.35f, enemy.width * 0.35f))
            .coerceIn(config.propWidth * 0.5f, config.worldWidth - config.propWidth * 0.5f)
        val y = (enemy.position.y + randomIn(-enemy.height * 0.15f, enemy.height * 0.15f))
            .coerceAtLeast(config.propHeight * 0.5f)
        return Vec(x, y)
    }
    
    private fun randomElitePropKind(): PropKind {
        return weightedPropKind(
            listOf(
                PropKind.HEALTH to config.propWeightHealth,
                PropKind.ENHANCE to config.propWeightEnhance,
            )
        )
    }
    
    private fun randomAnyPropKind(): PropKind {
        return weightedPropKind(
            listOf(
                PropKind.HEALTH to config.propWeightHealth,
                PropKind.ENHANCE to config.propWeightEnhance,
                PropKind.RAMPAGE to config.propWeightRampage,
                PropKind.BOMB to config.propWeightBomb,
            )
        )
    }
    
    private fun weightedPropKind(weightedKinds: List<Pair<PropKind, Int>>): PropKind {
        val normalized = weightedKinds.filter { it.second > 0 }
        val totalWeight = normalized.sumOf { it.second }
        require(totalWeight > 0) { "prop weight must be positive" }
        var roll = Random.nextInt(totalWeight)
        normalized.forEach { (kind, weight) ->
            roll -= weight
            if (roll < 0) {
                return kind
            }
        }
        return normalized.last().first
    }
    
    private fun createPropByKind(kind: PropKind, position: Vec): Prop {
        return when (kind) {
            PropKind.HEALTH -> createHealthProp(position)
            PropKind.ENHANCE -> createEnhanceProp(position)
            PropKind.RAMPAGE -> createRampageProp(position)
            PropKind.BOMB   -> createBombProp(position)
        }
    }
    
    private fun scoreForEnemy(enemy: Enemy): Int {
        return when (enemy) {
            is MobEnemy  -> config.mobEnemyScore
            is EliteEnemy -> config.eliteEnemyScore
            is SuperEnemy -> config.superEnemyScore
            is BossEnemy -> config.bossEnemyScore
        }
    }
    
    private fun handleBoundaryCleanup() {
        entities.values
            .filter { it.isOutOfWorld(config.worldWidth, config.worldHeight) }
            .forEach { scheduleDestroy(it, DestroyReason.OUT_OF_WORLD) }
    }
    
    private fun nextId(): Int = entityIdSeq++
    
    private fun <T : Entity> registerEntity(obj: T) {
        entities[obj.id] = obj
        recentlyDestroyedEntityTypes[obj.id] = obj.type
        if (obj is Hero) {
            players.getOrPut(obj.ownerPlayerId) { PlayerRuntimeState() }.heroId = obj.id
        }
        emitEvent(
            EntitySpawned(
                timeSec = elapsedTimeSec,
                entityId = obj.id,
                entityType = obj.type,
                position = obj.position,
                ownerPlayerId = (obj as? Hero)?.ownerPlayerId,
            )
        )
    }
    
    private fun scheduleDamage(target: Aircraft, damage: Int, sourceType: DamageSourceType) {
        val bucket = pendingDamage.getOrPut(target.id) { PendingDamage() }
        bucket.totalDamage += damage
        bucket.sourceType = sourceType
    }
    
    private fun scheduleDestroy(obj: Entity, reason: DestroyReason) {
        val current = pendingDestroyReasons[obj.id]
        if (current == null || destroyPriorityOf(reason) >= destroyPriorityOf(current)) {
            pendingDestroyReasons[obj.id] = reason
        }
        recentlyDestroyedEntityTypes[obj.id] = obj.type
    }
    
    private fun emitBuffStateChanges() {
        val enhanceActive = heroEnhanceTimer > 0f && hero != null
        val rampageActive = heroRampageTimer > 0f && hero != null
        
        if (enhanceActive != lastEnhanceActive) {
            emitEvent(
                BuffStateChanged(
                    timeSec = elapsedTimeSec,
                    playerId = PlayerId.LOCAL,
                    buffType = BuffType.ENHANCE,
                    active = enhanceActive,
                    remainingSec = heroEnhanceTimer,
                )
            )
            lastEnhanceActive = enhanceActive
        }
        
        if (rampageActive != lastRampageActive) {
            emitEvent(
                BuffStateChanged(
                    timeSec = elapsedTimeSec,
                    playerId = PlayerId.LOCAL,
                    buffType = BuffType.RAMPAGE,
                    active = rampageActive,
                    remainingSec = heroRampageTimer,
                )
            )
            lastRampageActive = rampageActive
        }
    }
    
    private fun emitEvent(event: GameEvent) {
        pendingEvents += event
    }
    
    private fun destroyPriorityOf(reason: DestroyReason): Int {
        return when (reason) {
            DestroyReason.KILLED       -> 4
            DestroyReason.PICKED_UP    -> 3
            DestroyReason.COLLISION    -> 2
            DestroyReason.OUT_OF_WORLD -> 1
            DestroyReason.SYSTEM       -> 0
        }
    }
    
    private fun randomIn(min: Float, max: Float): Float {
        return Random.nextFloat() * (max - min) + min
    }
}
