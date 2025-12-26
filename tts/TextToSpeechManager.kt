@file:Suppress("unused")

import android.app.Application
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

class TextToSpeechManager(application: Application) {

    private var tts: TextToSpeech? = null

    // Scope for internal coroutines (emitting events from binder threads)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    // Events for the UI (Speaking/Idle)
    private val _events = MutableSharedFlow<TtsEvent>()
    val events: SharedFlow<TtsEvent> = _events.asSharedFlow()

    // Map request ID to a continuation (for Files only)
    private val pendingFileRequests = ConcurrentHashMap<String, (Result<File>) -> Unit>()

    init {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported")
                    _isReady.value = false
                } else {
                    _isReady.value = true
                }
            } else {
                Log.e("TTS", "Initialization failed")
                _isReady.value = false
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                // If it's NOT a file request, it's a Speak request -> Notify UI
                if (!pendingFileRequests.containsKey(utteranceId)) {
                    scope.launch { _events.emit(TtsEvent.Speaking) }
                }
            }

            override fun onDone(utteranceId: String) {
                handleResult(utteranceId, true)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                handleResult(utteranceId, false)
            }

            override fun onError(utteranceId: String, errorCode: Int) {
                handleResult(utteranceId, false, "Error code: $errorCode")
            }
        })
    }

    private fun handleResult(utteranceId: String, isSuccess: Boolean, errorMsg: String = "Unknown error") {
        // Check if this was a file request
        val fileCallback = pendingFileRequests.remove(utteranceId)

        if (fileCallback != null) {
            // It was a File Synthesis -> Resume Coroutine
            if (isSuccess) {
                // We assume the file path was known by the caller, passing dummy file or tracking it is optional
                fileCallback(Result.success(File("")))
            } else {
                fileCallback(Result.failure(IllegalStateException(errorMsg)))
            }
        } else {
            // It was a Speak request -> Notify UI we are done
            scope.launch { _events.emit(TtsEvent.Idle) }
        }
    }

    fun speak(text: String) {
        if (!_isReady.value) return

        // Stop previous speech to keep state clean
        tts?.stop()

        // Use a distinct prefix if you want to debug, but checking map existence is enough
        val id = "speak_${UUID.randomUUID()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    suspend fun synthesizeToFile(text: String, outputFile: File): Result<File> = suspendCancellableCoroutine { cont ->
        if (!_isReady.value) {
            cont.resume(Result.failure(IllegalStateException("TTS not ready")))
            return@suspendCancellableCoroutine
        }

        outputFile.parentFile?.mkdirs()
        val utteranceId = "file_${UUID.randomUUID()}"

        pendingFileRequests[utteranceId] = { result ->
            if (cont.isActive) {
                if (result.isSuccess) cont.resume(Result.success(outputFile))
                else cont.resume(Result.failure(result.exceptionOrNull()!!))
            }
        }

        val result = tts?.synthesizeToFile(text, null, outputFile, utteranceId)

        if (result != TextToSpeech.SUCCESS) {
            pendingFileRequests.remove(utteranceId)
            cont.resume(Result.failure(IllegalStateException("Failed to queue synthesis")))
        }

        cont.invokeOnCancellation {
            pendingFileRequests.remove(utteranceId)
            // Optional: Stop TTS if needed, but risky if user is also listening to speech
        }
    }

    fun stop() {
        tts?.stop()
        // Force Idle state just in case onDone isn't called immediately
        scope.launch { _events.emit(TtsEvent.Idle) }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }

}

sealed interface TtsEvent {
    data object Speaking : TtsEvent
    data object Idle : TtsEvent
}
