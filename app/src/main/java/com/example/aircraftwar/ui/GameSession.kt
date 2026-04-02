package com.example.aircraftwar.ui

import com.example.aircraftwar.engine.*
import com.example.aircraftwar.entity.EntityType

data class SessionFrame(
    val renderFrame: FrameSnapshot,
    val audioEvents: List<GameAudio.Event>,
)

class GameSession(
    difficulty: GameDifficulty = GameDifficulty.NORMAL,
) {
    
    private val engine = GameEngine(difficulty.config)
    
    fun submitCommand(command: GameCommand) {
        engine.submitCommand(command)
    }
    
    fun tick(dt: Float): SessionFrame {
        engine.tick(dt)
        return buildFrame()
    }
    
    fun currentFrame(): SessionFrame {
        return buildFrame()
    }
    
    private fun buildFrame(): SessionFrame {
        val state = engine.captureState()
        val events = engine.pollEvents()
        return SessionFrame(
            renderFrame = state.toRenderFrame(),
            audioEvents = events.toAudioEvents(),
        )
    }
}

private fun GameStateSnapshot.toRenderFrame(): FrameSnapshot {
    return FrameSnapshot(
        drawables = entities.map { entity ->
            Drawable(
                id = entity.id,
                type = entity.type,
                x = entity.x,
                y = entity.y,
                width = entity.width,
                height = entity.height,
                hp = entity.hp,
                maxHp = entity.maxHp,
            )
        },
        score = score,
        elapsedTimeSec = elapsedTimeSec,
        hasBoss = hasBoss,
        gameOver = gameOver,
    )
}

private fun List<GameEvent>.toAudioEvents(): List<GameAudio.Event> {
    var heroShoot = false
    var bulletHit = false
    var pickupSupply = false
    var bombExplosion = false
    var gameOver = false
    
    for (event in this) {
        when (event) {
            is EntitySpawned -> {
                if (event.entityType == EntityType.HERO_BULLET) {
                    heroShoot = true
                }
            }
            
            is DamageApplied -> {
                bulletHit = true
            }
            
            is PropCollected -> {
                pickupSupply = true
                if (event.propType == EntityType.BOMB_PROP) {
                    bombExplosion = true
                }
            }
            
            is GameEnded     -> {
                gameOver = true
            }
            
            else             -> Unit
        }
    }
    
    return buildList {
        if (heroShoot) add(GameAudio.Event.HERO_SHOOT)
        if (bulletHit) add(GameAudio.Event.BULLET_HIT)
        if (pickupSupply) add(GameAudio.Event.PICKUP_SUPPLY)
        if (bombExplosion) add(GameAudio.Event.BOMB_EXPLOSION)
        if (gameOver) add(GameAudio.Event.GAME_OVER)
    }
}
