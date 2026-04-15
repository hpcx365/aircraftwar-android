package com.example.aircraftwar.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.example.aircraftwar.R
import com.example.aircraftwar.engine.AudioEvent
import com.example.aircraftwar.engine.FrameSnapshot

class GameAudio(context: Context) {
    
    private enum class BgmMode {
        NONE,
        NORMAL,
        BOSS,
    }
    
    private val appContext = context.applicationContext
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    
    private val normalBgm: MediaPlayer? = MediaPlayer.create(appContext, R.raw.bgm)?.apply {
        isLooping = true
        setVolume(0.55f, 0.55f)
    }
    private val bossBgm: MediaPlayer? = MediaPlayer.create(appContext, R.raw.bgm_boss)?.apply {
        isLooping = true
        setVolume(0.62f, 0.62f)
    }
    
    private val shootSoundId = soundPool.load(appContext, R.raw.bullet, 1)
    private val bulletHitSoundId = soundPool.load(appContext, R.raw.bullet_hit, 1)
    private val pickupSoundId = soundPool.load(appContext, R.raw.get_supply, 1)
    private val bombSoundId = soundPool.load(appContext, R.raw.bomb_explosion, 1)
    private val gameOverSoundId = soundPool.load(appContext, R.raw.game_over, 1)
    
    private var currentBgmMode = BgmMode.NONE
    private var hostPaused = false
    private var lastShootSoundAtMs = 0L
    private var lastHitSoundAtMs = 0L
    
    fun sync(frame: FrameSnapshot, audioEvents: List<AudioEvent>) {
        audioEvents.forEach(::playEvent)
        val targetMode = when {
            hostPaused || frame.gameOver -> BgmMode.NONE
            frame.hasBoss                -> BgmMode.BOSS
            else                         -> BgmMode.NORMAL
        }
        updateBgm(targetMode)
    }
    
    fun onHostPause() {
        hostPaused = true
        updateBgm(BgmMode.NONE)
    }
    
    fun onHostResume() {
        hostPaused = false
    }
    
    fun release() {
        updateBgm(BgmMode.NONE)
        normalBgm?.release()
        bossBgm?.release()
        soundPool.release()
    }
    
    private fun playEvent(event: AudioEvent) {
        when (event) {
            AudioEvent.HERO_SHOOT -> playShootSound()
            AudioEvent.BULLET_HIT -> playHitSound()
            AudioEvent.PICKUP_PROP -> playOneShot(pickupSoundId, volume = 0.95f)
            AudioEvent.BOMB_TRIGGER -> playOneShot(bombSoundId, volume = 1.0f)
            AudioEvent.GAME_OVER -> {
                updateBgm(BgmMode.NONE)
                playOneShot(gameOverSoundId, volume = 1.0f)
            }
        }
    }
    
    private fun playShootSound() {
        val now = System.currentTimeMillis()
        if (now - lastShootSoundAtMs < 60L) return
        lastShootSoundAtMs = now
        playOneShot(shootSoundId, volume = 0.65f)
    }
    
    private fun playHitSound() {
        val now = System.currentTimeMillis()
        if (now - lastHitSoundAtMs < 35L) return
        lastHitSoundAtMs = now
        playOneShot(bulletHitSoundId, volume = 0.72f)
    }
    
    private fun playOneShot(soundId: Int, volume: Float) {
        soundPool.play(soundId, volume, volume, 1, 0, 1f)
    }
    
    private fun updateBgm(targetMode: BgmMode) {
        if (currentBgmMode == targetMode) return
        when (currentBgmMode) {
            BgmMode.NORMAL -> normalBgm.stopSafely()
            BgmMode.BOSS   -> bossBgm.stopSafely()
            BgmMode.NONE   -> Unit
        }
        when (targetMode) {
            BgmMode.NORMAL -> normalBgm.startFromBeginning()
            BgmMode.BOSS   -> bossBgm.startFromBeginning()
            BgmMode.NONE   -> Unit
        }
        currentBgmMode = targetMode
    }
    
    private fun MediaPlayer?.startFromBeginning() {
        val player = this ?: return
        player.seekTo(0)
        player.start()
    }
    
    private fun MediaPlayer?.stopSafely() {
        val player = this ?: return
        if (player.isPlaying) {
            player.pause()
        }
        player.seekTo(0)
    }
}
