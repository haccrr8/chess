package com.example.chess.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.chess.R
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    var isMusicPlaying: Boolean = false
        private set

    var isSoundFxEnabled: Boolean = true
    var isHapticsEnabled: Boolean = true

    private var moveAudioTrack: AudioTrack? = null
    private var captureAudioTrack: AudioTrack? = null
    private var checkAudioTrack: AudioTrack? = null

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    init {
        try {
            initSynthesizedSounds()
        } catch (_: Exception) {}
    }

    private fun initSynthesizedSounds() {
        val sampleRate = 44100

        // Synthesize Move Sound: 320Hz -> 160Hz over 0.08s
        val moveDuration = 0.08
        val moveSamples = (sampleRate * moveDuration).toInt()
        val moveBuffer = ShortArray(moveSamples)
        var movePhase = 0.0
        for (i in 0 until moveSamples) {
            val t = i.toDouble() / sampleRate
            val progress = t / moveDuration
            val freq = 320.0 * Math.pow(160.0 / 320.0, progress)
            val envelope = 0.3 * exp(-3.0 * progress)
            movePhase += 2.0 * PI * freq / sampleRate
            moveBuffer[i] = (sin(movePhase) * envelope * Short.MAX_VALUE).toInt().toShort()
        }
        moveAudioTrack = createStaticTrack(moveBuffer, sampleRate)

        // Synthesize Capture Sound: 650Hz -> 220Hz over 0.12s
        val captureDuration = 0.12
        val captureSamples = (sampleRate * captureDuration).toInt()
        val captureBuffer = ShortArray(captureSamples)
        var capturePhase = 0.0
        for (i in 0 until captureSamples) {
            val t = i.toDouble() / sampleRate
            val progress = t / captureDuration
            val freq = 650.0 * Math.pow(220.0 / 650.0, progress)
            val envelope = 0.5 * exp(-3.5 * progress)
            capturePhase += 2.0 * PI * freq / sampleRate
            captureBuffer[i] = (sin(capturePhase) * envelope * Short.MAX_VALUE).toInt().toShort()
        }
        captureAudioTrack = createStaticTrack(captureBuffer, sampleRate)

        // Synthesize Check Alert Sound: 880Hz -> 1174Hz over 0.16s
        val checkDuration = 0.16
        val checkSamples = (sampleRate * checkDuration).toInt()
        val checkBuffer = ShortArray(checkSamples)
        var checkPhase = 0.0
        for (i in 0 until checkSamples) {
            val t = i.toDouble() / sampleRate
            val progress = t / checkDuration
            val freq = 880.0 + 294.0 * progress
            val envelope = 0.4 * exp(-2.5 * progress)
            checkPhase += 2.0 * PI * freq / sampleRate
            checkBuffer[i] = (sin(checkPhase) * envelope * Short.MAX_VALUE).toInt().toShort()
        }
        checkAudioTrack = createStaticTrack(checkBuffer, sampleRate)
    }

    private fun createStaticTrack(buffer: ShortArray, sampleRate: Int): AudioTrack {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(buffer, 0, buffer.size)
        return track
    }

    fun playMoveSound() {
        if (!isSoundFxEnabled) return
        try {
            moveAudioTrack?.let { track ->
                track.stop()
                track.reloadStaticData()
                track.play()
            }
        } catch (_: Exception) {}
    }

    fun playCaptureSound() {
        if (!isSoundFxEnabled) return
        try {
            captureAudioTrack?.let { track ->
                track.stop()
                track.reloadStaticData()
                track.play()
            }
        } catch (_: Exception) {}
    }

    fun playCheckSound() {
        if (!isSoundFxEnabled) return
        try {
            checkAudioTrack?.let { track ->
                track.stop()
                track.reloadStaticData()
                track.play()
            }
        } catch (_: Exception) {}
    }

    fun vibrateMove() {
        if (!isHapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(20)
            }
        } catch (_: Exception) {}
    }

    fun vibrateCapture() {
        if (!isHapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(45)
            }
        } catch (_: Exception) {}
    }

    fun vibrateCheck() {
        if (!isHapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 40, 50, 60),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(100)
            }
        } catch (_: Exception) {}
    }

    fun toggleRyukMusic(): Boolean {
        return try {
            if (isMusicPlaying) {
                mediaPlayer?.pause()
                isMusicPlaying = false
            } else {
                if (mediaPlayer == null) {
                    mediaPlayer = MediaPlayer.create(context, R.raw.ryuk_theme)?.apply {
                        isLooping = true
                    }
                }
                mediaPlayer?.start()
                isMusicPlaying = true
            }
            isMusicPlaying
        } catch (_: Exception) {
            false
        }
    }

    fun release() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            moveAudioTrack?.release()
            captureAudioTrack?.release()
            checkAudioTrack?.release()
        } catch (_: Exception) {}
    }
}
