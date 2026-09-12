package com.example.chess.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import com.example.chess.R
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    var isMusicPlaying: Boolean = false
        private set

    private var moveAudioTrack: AudioTrack? = null
    private var captureAudioTrack: AudioTrack? = null

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
        try {
            moveAudioTrack?.let { track ->
                track.stop()
                track.reloadStaticData()
                track.play()
            }
        } catch (_: Exception) {}
    }

    fun playCaptureSound() {
        try {
            captureAudioTrack?.let { track ->
                track.stop()
                track.reloadStaticData()
                track.play()
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
        } catch (_: Exception) {}
    }
}
