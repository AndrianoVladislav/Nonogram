package com.example.nonogram.data.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.example.nonogram.R

class SoundManager(context: Context) {
    private val soundPool: SoundPool

    private val correctSound: Int
    private val wrongSound: Int
    private val heartBreakSound: Int
    private val switchSound: Int

    private val loadedIds = mutableSetOf<Int>()

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedIds.add(sampleId)
                Log.d("SoundManager", "Sound loaded: $sampleId")
            } else {
                Log.e("SoundManager", "Error loading sound: $sampleId, status: $status")
            }
        }

        correctSound = soundPool.load(context, R.raw.correct, 1)
        wrongSound = soundPool.load(context, R.raw.wrong, 1)
        heartBreakSound = soundPool.load(context, R.raw.heart_break, 1)
        switchSound = soundPool.load(context, R.raw.sound_button, 1)
    }

    fun playCorrect() = play(correctSound)
    fun playWrong() = play(wrongSound)
    fun playHeartBreak() = play(heartBreakSound)
    fun playSwitch() = play(switchSound)

    private fun play(soundId: Int) {
        if (soundId != 0 && loadedIds.contains(soundId)) {
            soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
        } else {
            Log.w("SoundManager", "Sound $soundId not loaded yet")
        }
    }

    fun release() {
        soundPool.release()
    }
}
