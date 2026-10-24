package com.mypainter.app

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import java.util.Locale

class AudioGuide(private val store: ArtworkStore, context: Context) {
    private var engine: TextToSpeech? = null
    private var ready = false
    private val tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 55) }.getOrNull()

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                val result = engine?.setLanguage(Locale.TAIWAN)
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine?.setLanguage(Locale.CHINESE)
                }
                engine?.setSpeechRate(.90f)
            }
        }
    }

    var voiceEnabled: Boolean
        get() = store.preferences.getBoolean("voice", true)
        set(value) { store.preferences.edit().putBoolean("voice", value).apply() }
    var soundEnabled: Boolean
        get() = store.preferences.getBoolean("sound", true)
        set(value) { store.preferences.edit().putBoolean("sound", value).apply() }
    var easyMode: Boolean
        get() = store.preferences.getBoolean("easy", true)
        set(value) { store.preferences.edit().putBoolean("easy", value).apply() }

    fun speak(text: String) {
        if (voiceEnabled && ready) engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "painter")
    }
    fun click() {
        if (soundEnabled) tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 65)
    }
    fun success() {
        if (soundEnabled) tone?.startTone(ToneGenerator.TONE_PROP_ACK, 190)
    }
    fun retry() {
        if (soundEnabled) tone?.startTone(ToneGenerator.TONE_PROP_NACK, 150)
    }
    fun close() {
        engine?.stop()
        engine?.shutdown()
        tone?.release()
    }
}
