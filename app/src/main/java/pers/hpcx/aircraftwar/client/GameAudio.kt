package pers.hpcx.aircraftwar.client

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import pers.hpcx.aircraftwar.R
import pers.hpcx.aircraftwar.kernal.AudioEvent
import pers.hpcx.aircraftwar.kernal.FrameSnapshot

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
    var musicEnabled = true
    
    fun sync(frame: FrameSnapshot, audioEvents: List<AudioEvent>) {
        audioEvents.forEach(::playEvent)
        val targetMode = when {
            hostPaused || frame.gameOver -> BgmMode.NONE
            frame.hasBoss -> BgmMode.BOSS
            else -> BgmMode.NORMAL
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
        if (!musicEnabled) return
        when (event) {
            AudioEvent.HERO_SHOOT -> playOneShot(shootSoundId, volume = 0.65f)
            AudioEvent.BULLET_HIT -> playOneShot(bulletHitSoundId, volume = 0.72f)
            AudioEvent.PICKUP_PROP -> playOneShot(pickupSoundId, volume = 0.95f)
            AudioEvent.BOMB_TRIGGER -> playOneShot(bombSoundId, volume = 1.0f)
            AudioEvent.GAME_OVER -> {
                updateBgm(BgmMode.NONE)
                playOneShot(gameOverSoundId, volume = 1.0f)
            }
        }
    }
    
    private fun playOneShot(soundId: Int, volume: Float) {
        soundPool.play(soundId, volume, volume, 1, 0, 1f)
    }
    
    private fun updateBgm(targetMode: BgmMode) {
        if (currentBgmMode == targetMode) return
        when (currentBgmMode) {
            BgmMode.NORMAL -> normalBgm.stopSafely()
            BgmMode.BOSS -> bossBgm.stopSafely()
            BgmMode.NONE -> Unit
        }
        if (musicEnabled) {
            when (targetMode) {
                BgmMode.NORMAL -> normalBgm.startFromBeginning()
                BgmMode.BOSS -> bossBgm.startFromBeginning()
                BgmMode.NONE -> Unit
            }
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
