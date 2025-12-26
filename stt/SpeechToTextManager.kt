
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
import org.koin.core.annotation.Single
import java.util.Locale

/**
 * Robust error handling hierarchy.
 */
sealed interface SttError {
    val message: String

    data object Permission : SttError { override val message = "Microphone permission required" }
    data object Network : SttError { override val message = "Network connection failed" }
    data object NoMatch : SttError { override val message = "No speech detected" }
    data object ServiceBusy : SttError { override val message = "Recognition service is busy" }
    data class Unknown(override val message: String) : SttError
}

/**
 * Wrapper to pass SttError through Kotlin Result failure (which expects Throwable).
 */
class SttException(val error: SttError) : Exception(error.message)

class SpeechToTextManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _sttEvents = MutableSharedFlow<Result<String>>()
    val sttEvents: SharedFlow<Result<String>> = _sttEvents.asSharedFlow()

    private val _isReadyForSpeech = MutableStateFlow(false)
    val isReadyForSpeech: StateFlow<Boolean> = _isReadyForSpeech.asStateFlow()

    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            mainScope.launch { _isReadyForSpeech.value = true }
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            mainScope.launch { _soundLevel.value = normalized }
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            mainScope.launch {
                _isReadyForSpeech.value = false
                _soundLevel.value = 0f
            }
        }

        override fun onError(error: Int) {
            mainScope.launch {
                _isReadyForSpeech.value = false
                _soundLevel.value = 0f
                val sttError = mapErrorToSealedClass(error)
                // Wrap the typed error in an exception to fit the Result contract
                _sttEvents.emit(Result.failure(SttException(sttError)))
            }
        }

        override fun onResults(results: Bundle?) {
            mainScope.launch {
                _isReadyForSpeech.value = false
                _soundLevel.value = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    _sttEvents.emit(Result.success(text))
                }
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun startListening(language: Locale = Locale.getDefault()) {
        mainScope.launch {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _sttEvents.emit(Result.failure(SttException(SttError.Unknown("Speech recognition not available"))))
                return@launch
            }

            _isReadyForSpeech.value = false
            _soundLevel.value = 0f
            speechRecognizer?.destroy()

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(recognitionListener)
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.toString())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            try {
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                _sttEvents.emit(Result.failure(SttException(SttError.Unknown(e.message ?: "Start failed"))))
            }
        }
    }

    fun stopListening() {
        mainScope.launch {
            _isReadyForSpeech.value = false
            _soundLevel.value = 0f
            speechRecognizer?.stopListening()
        }
    }

    fun shutdown() {
        mainScope.launch {
            _isReadyForSpeech.value = false
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    private fun mapErrorToSealedClass(errorCode: Int): SttError {
        return when (errorCode) {
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SttError.Permission
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SttError.Network
            SpeechRecognizer.ERROR_NO_MATCH -> SttError.NoMatch
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> SttError.ServiceBusy
            SpeechRecognizer.ERROR_AUDIO -> SttError.Unknown("Audio recording error")
            SpeechRecognizer.ERROR_CLIENT -> SttError.Unknown("Client side error")
            SpeechRecognizer.ERROR_SERVER -> SttError.Unknown("Server error")
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SttError.NoMatch // Treat timeout as no match
            else -> SttError.Unknown("Error code: $errorCode")
        }
    }
}