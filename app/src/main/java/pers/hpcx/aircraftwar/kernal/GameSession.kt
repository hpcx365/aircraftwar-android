package pers.hpcx.aircraftwar.kernal

import pers.hpcx.aircraftwar.kernal.entity.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.math.*
import kotlin.random.Random

class GameSession(
    val difficulty: GameDifficulty,
) {
    
    private val lock = ReentrantReadWriteLock()
    
    private var entityIdSeq = 0
    private val entities = linkedMapOf<Int, Entity>()
    
    private var redHeroJoined = false
    private var blueHeroJoined = false
    private var redHero: RedHero? = null
    private var blueHero: BlueHero? = null
    private var redScore: Int = 0
    private var blueScore: Int = 0
    
    private var started = false
    private val commandQueue = ArrayDeque<GameCommand>()
    
    private var enemySpawnTimer = 0f
    private var elapsedTimeSec = 0f
    private val audioEvents = ArrayList<AudioEvent>()
    
    fun isOver(): Boolean {
        lock.readLock().lock()
        try {
            return started && (!redHeroJoined || redHero == null) && (!blueHeroJoined || blueHero == null)
        } finally {
            lock.readLock().unlock()
        }
    }
    
    fun submitCommand(command: GameCommand) {
        lock.writeLock().lock()
        try {
            commandQueue.add(command)
        } finally {
            lock.writeLock().unlock()
        }
    }
    
    fun update(dt: Float) {
        lock.writeLock().lock()
        try {
            applyCommands()
            clearEvents()
            moveHero(redHero, dt)
            moveHero(blueHero, dt)
            if (started) {
                moveEntity(dt)
                updateHeroTimers(redHero, dt)
                updateHeroTimers(blueHero, dt)
                heroShoot(redHero)
                heroShoot(blueHero)
                enemyShoot(dt)
                spawnEnemy(dt)
                resolveCollisions()
                boundaryCleanup()
                elapsedTimeSec += dt
            }
            if (isOver()) {
                emitEvent(AudioEvent.GAME_OVER)
            }
        } finally {
            lock.writeLock().unlock()
        }
    }
    
    fun snapshot(): FrameSnapshot {
        lock.readLock().lock()
        try {
            return FrameSnapshot(
                worldWidth = config.worldWidth,
                worldHeight = config.worldHeight,
                difficulty = difficulty,
                scoreRed = redScore,
                scoreBlue = blueScore,
                elapsedTimeSec = elapsedTimeSec,
                hasBoss = entities.values.any { it is BossEnemy },
                gameOver = isOver(),
                events = ArrayList(audioEvents),
                entities = entities.values.map {
                    when (it) {
                        is Aircraft -> EntitySnapshot(
                            id = it.id,
                            type = it.type,
                            x = it.position.x,
                            y = it.position.y,
                            width = it.width,
                            height = it.height,
                            hp = it.hp,
                            maxHp = it.maxHp,
                        )
                        else -> EntitySnapshot(
                            id = it.id,
                            type = it.type,
                            x = it.position.x,
                            y = it.position.y,
                            width = it.width,
                            height = it.height,
                        )
                    }
                },
            )
        } finally {
            lock.readLock().unlock()
        }
    }
    
    private fun clearEvents() {
        audioEvents.clear()
    }
    
    private fun applyCommands() {
        var redPlayerCommand: PlayerCommand? = null
        var bluePlayerCommand: PlayerCommand? = null
        
        while (commandQueue.isNotEmpty()) {
            when (val command = commandQueue.removeFirst()) {
                is StartGameCommand -> {
                    started = true
                }
                is PlayerCommand -> when (command) {
                    is PlayerJoinRedCommand -> {
                        if (!redHeroJoined && redPlayerCommand == null) {
                            redPlayerCommand = command
                        }
                    }
                    is PlayerJoinBlueCommand -> {
                        if (!blueHeroJoined && bluePlayerCommand == null) {
                            bluePlayerCommand = command
                        }
                    }
                    else -> {
                        when (command.playerId) {
                            redHero?.playerId -> {
                                if (command.sequence > (redPlayerCommand?.sequence ?: -1)) {
                                    redPlayerCommand = command
                                }
                            }
                            blueHero?.playerId -> {
                                if (command.sequence > (bluePlayerCommand?.sequence ?: -1)) {
                                    bluePlayerCommand = command
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (redPlayerCommand != null) applyRedPlayerCommand(redPlayerCommand)
        if (bluePlayerCommand != null) applyBluePlayerCommand(bluePlayerCommand)
    }
    
    private fun applyRedPlayerCommand(command: PlayerCommand) {
        when (command) {
            is PlayerJoinRedCommand -> {
                if (redHeroJoined) throw IllegalStateException()
                redHeroJoined = true
                createRedHero(command.playerId)
            }
            is PlayerJoinBlueCommand -> {
                throw IllegalStateException()
            }
            is PlayerMoveCommand -> {
                val hero = redHero ?: return
                hero.targetPosition = command.targetPosition
            }
            is PlayerStopCommand -> {
                val hero = redHero ?: return
                hero.targetPosition = null
            }
            is PlayerLeaveCommand -> {
                val hero = redHero ?: return
                unregisterEntity(hero)
            }
        }
    }
    
    private fun applyBluePlayerCommand(command: PlayerCommand) {
        when (command) {
            is PlayerJoinRedCommand -> {
                throw IllegalStateException()
            }
            is PlayerJoinBlueCommand -> {
                if (blueHeroJoined) throw IllegalStateException()
                blueHeroJoined = true
                createBlueHero(command.playerId)
            }
            is PlayerMoveCommand -> {
                val hero = blueHero ?: return
                hero.targetPosition = command.targetPosition
            }
            is PlayerStopCommand -> {
                val hero = blueHero ?: return
                hero.targetPosition = null
            }
            is PlayerLeaveCommand -> {
                val hero = blueHero ?: return
                unregisterEntity(hero)
            }
        }
    }
    
    private fun createRedHero(playerId: String): RedHero {
        return registerEntity(
            RedHero(
                id = nextId(),
                playerId = playerId,
                width = config.heroWidth,
                height = config.heroHeight,
                position = Vec(config.worldWidth * 0.5f, config.worldHeight * 0.2f),
                maxHp = config.heroHp,
            )
        )
    }
    
    private fun createBlueHero(playerId: String): BlueHero {
        return registerEntity(
            BlueHero(
                id = nextId(),
                playerId = playerId,
                width = config.heroWidth,
                height = config.heroHeight,
                position = Vec(config.worldWidth * 0.5f, config.worldHeight * 0.2f),
                maxHp = config.heroHp,
            )
        )
    }
    
    private fun createMobEnemy(position: Vec, velocity: Vec): MobEnemy {
        return registerEntity(
            MobEnemy(
                id = nextId(),
                width = config.mobEnemyWidth,
                height = config.mobEnemyHeight,
                position = position,
                velocity = velocity,
                maxHp = config.mobEnemyHp,
                shootPattern = null,
            )
        )
    }
    
    private fun createEliteEnemy(position: Vec, velocity: Vec): EliteEnemy {
        return registerEntity(
            EliteEnemy(
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
        )
    }
    
    private fun createSuperEnemy(position: Vec, downwardSpeed: Float): SuperEnemy {
        return registerEntity(
            SuperEnemy(
                id = nextId(),
                width = config.superEnemyWidth,
                height = config.superEnemyHeight,
                position = position,
                velocity = Vec(
                    config.superEnemyLateralSpeed * if (Random.nextBoolean()) 1f else -1f,
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
        )
    }
    
    private fun createBossEnemy(): BossEnemy {
        return registerEntity(
            BossEnemy(
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
        )
    }
    
    private fun createRedHeroBullet(position: Vec, velocity: Vec, power: Int): RedHeroBullet {
        return registerEntity(
            RedHeroBullet(
                id = nextId(),
                width = config.heroBulletWidth,
                height = config.heroBulletHeight,
                position = position,
                velocity = velocity,
                power = power,
            )
        )
    }
    
    private fun createBlueHeroBullet(position: Vec, velocity: Vec, power: Int): BlueHeroBullet {
        return registerEntity(
            BlueHeroBullet(
                id = nextId(),
                width = config.heroBulletWidth,
                height = config.heroBulletHeight,
                position = position,
                velocity = velocity,
                power = power,
            )
        )
    }
    
    private fun createEnemyBullet(position: Vec, velocity: Vec): EnemyBullet {
        return registerEntity(
            EnemyBullet(
                id = nextId(),
                width = config.enemyBulletWidth,
                height = config.enemyBulletHeight,
                position = position,
                velocity = velocity,
                power = config.enemyPower,
            )
        )
    }
    
    private fun createHealthProp(position: Vec): HealthProp {
        return registerEntity(
            HealthProp(
                id = nextId(),
                width = config.propWidth,
                height = config.propHeight,
                position = position,
                velocity = Vec(0f, -config.propFallSpeed),
            )
        )
    }
    
    private fun createEnhanceProp(position: Vec): EnhanceProp {
        return registerEntity(
            EnhanceProp(
                id = nextId(),
                width = config.propWidth,
                height = config.propHeight,
                position = position,
                velocity = Vec(0f, -config.propFallSpeed),
            )
        )
    }
    
    private fun createRampageProp(position: Vec): RampageProp {
        return registerEntity(
            RampageProp(
                id = nextId(),
                width = config.propWidth,
                height = config.propHeight,
                position = position,
                velocity = Vec(0f, -config.propFallSpeed),
            )
        )
    }
    
    private fun createBombProp(position: Vec): BombProp {
        return registerEntity(
            BombProp(
                id = nextId(),
                width = config.propWidth,
                height = config.propHeight,
                position = position,
                velocity = Vec(0f, -config.propFallSpeed),
            )
        )
    }
    
    private fun moveHero(hero: Hero?, dt: Float) {
        if (hero == null) return
        val target = hero.targetPosition ?: return
        hero.position = hero.position.moveTowards(
            target = target,
            maxDistance = config.heroMaxSpeed * dt
        )
    }
    
    private fun moveEntity(dt: Float) {
        entities.values
            .filter { it !is Hero }
            .forEach {
                when (it) {
                    is SuperEnemy -> moveSuperEnemy(it, dt)
                    is BossEnemy -> moveBossEnemy(it, dt)
                    else -> it.move(dt)
                }
            }
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
            else -> Unit
        }
    }
    
    private fun updateHeroTimers(hero: Hero?, dt: Float) {
        if (hero == null) return
        hero.shootTimer = (hero.shootTimer - dt).coerceAtLeast(0f)
        hero.enhanceTimer = (hero.enhanceTimer - dt).coerceAtLeast(0f)
        hero.rampageTimer = (hero.rampageTimer - dt).coerceAtLeast(0f)
    }
    
    private fun heroShoot(hero: Hero?) {
        if (hero == null) return
        if (hero.shootTimer > 0f) return
        hero.shootTimer = currentHeroFireInterval(hero)
        currentHeroShootPattern(hero)
            .createBullets(hero.position) { position, velocity ->
                when (hero) {
                    is RedHero -> createRedHeroBullet(position, velocity, currentHeroBulletPower(hero))
                    is BlueHero -> createBlueHeroBullet(position, velocity, currentHeroBulletPower(hero))
                }
            }
        emitEvent(AudioEvent.HERO_SHOOT)
    }
    
    private fun currentHeroShootPattern(hero: Hero): ShootPattern {
        return when {
            hero.rampageTimer > 0f -> RadialPattern(
                count = config.rampageBulletCount,
                velocity = config.heroBulletSpeed,
                directionDeg = 90f + sin(hero.rampageTimer / config.rampageDuration * 2f * PI.toFloat()) * 90f,
            )
            hero.enhanceTimer > 0f -> FanPattern(
                count = config.enhanceBulletCount,
                velocity = config.heroBulletSpeed,
                directionDeg = 90f,
                fieldDeg = config.enhanceFieldDeg * (1f - cos(hero.enhanceTimer / config.enhanceDuration * 3f * PI.toFloat())),
            )
            else -> hero.shootPattern ?: StraightPattern(
                velocity = config.heroBulletSpeed,
                directionDeg = 90f,
            )
        }
    }
    
    private fun currentHeroBulletPower(hero: Hero): Int {
        return config.heroPower * when {
            hero.rampageTimer > 0f -> config.rampageDamageMultiplier
            hero.enhanceTimer > 0f -> config.enhanceDamageMultiplier
            else -> 1
        }
    }
    
    private fun currentHeroFireInterval(hero: Hero): Float {
        return config.heroFireInterval * when {
            hero.rampageTimer > 0f -> config.rampageFireIntervalMultiplier
            hero.enhanceTimer > 0f -> config.enhanceFireIntervalMultiplier
            else -> 1f
        }
    }
    
    private fun enemyShoot(dt: Float) {
        entities.values
            .filterIsInstance<Enemy>()
            .forEach {
                it.shootTimer -= dt
                if (it.shootTimer > 0f) return@forEach
                it.shootTimer = config.enemyFireInterval
                it.shootPattern?.createBullets(it.position, ::createEnemyBullet)
            }
    }
    
    private fun spawnEnemy(dt: Float) {
        enemySpawnTimer -= dt
        if (enemySpawnTimer > 0f) return
        enemySpawnTimer = config.enemySpawnInterval
        
        val x = 0.5f * config.worldWidth + randomIn(-1f, 1f) * 0.5f * config.enemySpawnWidth
        val y = config.enemySpawnY
        val vy = randomIn(config.enemyMinSpeed, config.enemyMaxSpeed)
        val roll = Random.nextFloat()
        val hasBoss = entities.values.any { it is BossEnemy }
        when {
            roll < 0.60f -> createMobEnemy(Vec(x, y), Vec(0f, -vy))
            roll < 0.82f -> createEliteEnemy(Vec(x, y), Vec(0f, -vy))
            roll < 0.95f -> createSuperEnemy(Vec(x, y), vy)
            !hasBoss -> createBossEnemy()
            else -> createSuperEnemy(Vec(x, y), vy)
        }
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
        
        for (hero in listOfNotNull(redHero, blueHero)) {
            enemyBullets.forEach {
                if (it.collides(hero)) resolveEnemyBulletHitHero(hero, it)
            }
            props.forEach {
                if (it.collides(hero)) resolvePropPickup(hero, it)
            }
            enemies.forEach {
                if (it.collides(hero)) resolveAircraftCrash(hero, it)
            }
        }
    }
    
    private fun resolveHeroBulletHitEnemy(bullet: HeroBullet, enemy: Enemy) {
        unregisterEntity(bullet)
        enemyTakeDamage(
            enemy, bullet.power, when (bullet) {
                is RedHeroBullet -> redHero
                is BlueHeroBullet -> blueHero
            }
        )
        emitEvent(AudioEvent.BULLET_HIT)
    }
    
    private fun resolveEnemyBulletHitHero(hero: Hero, bullet: EnemyBullet) {
        unregisterEntity(bullet)
        heroTakeDamage(hero, bullet.power)
        emitEvent(AudioEvent.BULLET_HIT)
    }
    
    private fun resolvePropPickup(hero: Hero, prop: Prop) {
        unregisterEntity(prop)
        when (prop) {
            is HealthProp -> {
                heroIncreaseHp(hero, (hero.maxHp * config.healthPropHealRatio).roundToInt().coerceAtLeast(1))
            }
            is EnhanceProp -> {
                hero.enhanceTimer = config.enhanceDuration
            }
            is RampageProp -> {
                hero.rampageTimer = config.rampageDuration
            }
            is BombProp -> {
                val bombDamage = (config.bossEnemyHp * config.bombDamageRatio).roundToInt().coerceAtLeast(1)
                entities.values
                    .filterIsInstance<Enemy>()
                    .forEach { enemyTakeDamage(it, bombDamage, hero) }
                entities.values
                    .filterIsInstance<EnemyBullet>()
                    .forEach { unregisterEntity(it) }
                emitEvent(AudioEvent.BOMB_TRIGGER)
            }
        }
        emitEvent(AudioEvent.PICKUP_PROP)
    }
    
    private fun resolveAircraftCrash(hero: Hero, enemy: Enemy) {
        heroTakeDamage(hero, enemy.hp)
        enemyTakeDamage(enemy, enemy.hp, hero)
    }
    
    private fun heroIncreaseHp(hero: Hero, amount: Int) {
        if (hero.hp <= 0) return
        hero.hp = (hero.hp + amount).coerceAtMost(hero.maxHp)
    }
    
    private fun heroTakeDamage(hero: Hero, amount: Int) {
        if (hero.hp <= 0) return
        hero.hp = (hero.hp - amount).coerceAtLeast(0)
        if (hero.hp <= 0) unregisterEntity(hero)
    }
    
    private fun enemyTakeDamage(enemy: Enemy, amount: Int, damageSource: Hero?) {
        if (enemy.hp <= 0) return
        enemy.hp = (enemy.hp - amount).coerceAtLeast(0)
        if (enemy.hp <= 0) {
            val score = scoreForEnemy(enemy)
            when (damageSource) {
                is RedHero -> redScore += score
                is BlueHero -> blueScore += score
                else -> {}
            }
            dropPropsForEnemy(enemy)
            unregisterEntity(enemy)
        }
    }
    
    private fun dropPropsForEnemy(enemy: Enemy) {
        when (enemy) {
            is EliteEnemy -> {
                registerEntity(createPropByKind(randomElitePropType(), propSpawnPosition(enemy)))
            }
            is SuperEnemy -> {
                repeat(Random.nextInt(1, 3)) {
                    registerEntity(createPropByKind(randomAnyPropType(), propSpawnPosition(enemy)))
                }
            }
            is BossEnemy -> {
                repeat(3) {
                    registerEntity(createPropByKind(randomAnyPropType(), propSpawnPosition(enemy)))
                }
            }
            else -> Unit
        }
    }
    
    private fun propSpawnPosition(enemy: Enemy): Vec {
        val x = (enemy.position.x + randomIn(-enemy.width * 0.35f, enemy.width * 0.35f))
            .coerceIn(config.propWidth * 0.5f, config.worldWidth - config.propWidth * 0.5f)
        val y = (enemy.position.y + randomIn(-enemy.height * 0.15f, enemy.height * 0.15f))
            .coerceAtLeast(config.propHeight * 0.5f)
        return Vec(x, y)
    }
    
    private fun randomElitePropType(): PropType {
        return weightedPropType(
            listOf(
                PropType.HEALTH to config.propWeightHealth,
                PropType.ENHANCE to config.propWeightEnhance,
            )
        )
    }
    
    private fun randomAnyPropType(): PropType {
        return weightedPropType(
            listOf(
                PropType.HEALTH to config.propWeightHealth,
                PropType.ENHANCE to config.propWeightEnhance,
                PropType.RAMPAGE to config.propWeightRampage,
                PropType.BOMB to config.propWeightBomb,
            )
        )
    }
    
    private fun weightedPropType(weightedKinds: List<Pair<PropType, Int>>): PropType {
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
    
    private fun scoreForEnemy(enemy: Enemy): Int {
        return when (enemy) {
            is MobEnemy -> config.mobEnemyScore
            is EliteEnemy -> config.eliteEnemyScore
            is SuperEnemy -> config.superEnemyScore
            is BossEnemy -> config.bossEnemyScore
        }
    }
    
    private fun createPropByKind(kind: PropType, position: Vec): Prop {
        return when (kind) {
            PropType.HEALTH -> createHealthProp(position)
            PropType.ENHANCE -> createEnhanceProp(position)
            PropType.RAMPAGE -> createRampageProp(position)
            PropType.BOMB -> createBombProp(position)
        }
    }
    
    private fun boundaryCleanup() {
        entities.values
            .filter { it.isOutOfWorld() }
            .forEach { unregisterEntity(it) }
    }
    
    private val config: GameConfig
        get() = when (difficulty) {
            GameDifficulty.EASY -> EasyGameConfig
            GameDifficulty.NORMAL -> NormalGameConfig
            GameDifficulty.HARD -> HardGameConfig
        }
    
    private fun nextId(): Int {
        return entityIdSeq++
    }
    
    private fun <T : Entity> registerEntity(obj: T): T {
        when (obj) {
            is RedHero -> redHero = obj
            is BlueHero -> blueHero = obj
            else -> {}
        }
        entities[obj.id] = obj
        return obj
    }
    
    private fun <T : Entity> unregisterEntity(obj: T) {
        when (obj) {
            is RedHero -> redHero = null
            is BlueHero -> blueHero = null
            else -> {}
        }
        entities.remove(obj.id)
    }
    
    private fun emitEvent(event: AudioEvent) {
        audioEvents += event
    }
    
    private fun Entity.move(dt: Float) {
        position += velocity * dt
    }
    
    private fun Entity.collides(that: Entity): Boolean = bounds.intersects(that.bounds)
    
    private fun Entity.isOutOfWorld(): Boolean {
        return when (this) {
            is Hero -> false
            is BossEnemy -> false
            is Enemy -> position.y < 0
            else -> !bounds.intersects(Rect(0.5f * config.worldWidth, 0.5f * config.worldHeight, config.worldWidth, config.worldHeight))
        }
    }
    
    private fun randomIn(min: Float, max: Float): Float {
        return Random.nextFloat() * (max - min) + min
    }
}
