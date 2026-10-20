package com.nyx.chess.util

import android.media.AudioManager
import android.media.ToneGenerator
import com.nyx.chess.presentation.GameEvent

/** Efeitos discretos sem assets externos (ToneGenerator). Troque por SoundPool + .ogg se quiser sons de madeira. */
class SoundPlayer {
    private val tone: ToneGenerator? = try { ToneGenerator(AudioManager.STREAM_MUSIC, 45) } catch (e: Exception) { null }

    fun play(e: GameEvent) {
        try {
            when (e) {
                GameEvent.Select -> tone?.startTone(ToneGenerator.TONE_DTMF_1, 25)
                GameEvent.Move -> tone?.startTone(ToneGenerator.TONE_DTMF_5, 45)
                GameEvent.Capture -> tone?.startTone(ToneGenerator.TONE_DTMF_9, 90)
                GameEvent.Check -> tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 160)
                GameEvent.GameOver -> tone?.startTone(ToneGenerator.TONE_PROP_ACK, 350)
            }
        } catch (e: Exception) { }
    }

    fun release() { tone?.release() }
}
