package com.mypainter.app

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Child-friendly Traditional Chinese narration.
 *
 * A description spoken while Android TTS is initializing must not disappear.
 * Keep only the most recently selected item, preventing a late engine startup
 * from announcing the previous picture.
 */
class AudioGuide(private val store: ArtworkStore, context: Context) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var ready = false
    private var closed = false
    private var pendingSpeech: String? = null
    private val tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 55) }.getOrNull()

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            // TTS may invoke its callback before constructor assignment,
            // so configure it on the main event loop only after allocation.
            mainHandler.post {
                if (closed) return@post
                if (status == TextToSpeech.SUCCESS) {
                    val tts = engine ?: return@post
                    val language = tts.setLanguage(Locale.TAIWAN)
                    val result = if (
                        language == TextToSpeech.LANG_NOT_SUPPORTED ||
                        language == TextToSpeech.LANG_MISSING_DATA
                    ) tts.setLanguage(Locale.CHINESE) else language
                    if (
                        result != TextToSpeech.LANG_NOT_SUPPORTED &&
                        result != TextToSpeech.LANG_MISSING_DATA
                    ) {
                        tts.setSpeechRate(.88f)
                        ready = true
                        val pending = pendingSpeech
                        pendingSpeech = null
                        if (voiceEnabled && !pending.isNullOrBlank()) {
                            tts.speak(pending, TextToSpeech.QUEUE_FLUSH, null, "painter")
                        }
                    }
                }
            }
        }
    }

    var voiceEnabled: Boolean
        get() = store.preferences.getBoolean("voice", true)
        set(value) {
            store.preferences.edit().putBoolean("voice", value).apply()
            if (!value) {
                pendingSpeech = null
                engine?.stop()
            }
        }

    var soundEnabled: Boolean
        get() = store.preferences.getBoolean("sound", true)
        set(value) { store.preferences.edit().putBoolean("sound", value).apply() }

    var easyMode: Boolean
        get() = store.preferences.getBoolean("easy", true)
        set(value) { store.preferences.edit().putBoolean("easy", value).apply() }

    fun speakItem(item: DrawingItem) = speak(ItemSpeech.intro(item))

    fun speak(text: String) {
        if (closed || !voiceEnabled || text.isBlank()) return
        if (!ready) {
            pendingSpeech = text
            return
        }
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "painter")
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
        closed = true
        pendingSpeech = null
        engine?.stop()
        engine?.shutdown()
        tone?.release()
    }
}
