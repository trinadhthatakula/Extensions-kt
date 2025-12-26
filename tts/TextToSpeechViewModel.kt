
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import java.io.File

data class TtsState(
    val isReady: Boolean = false,
    val isSpeaking: Boolean = false,
    val errorMessage: String? = null,
    val isSavingToFile: Boolean = false,
    val fileSavedPath: String? = null
)

class TextToSpeechViewModel(
    private val ttsManager: TextToSpeechManager
) : ViewModel() {

    // Internal state only tracks transient UI things
    private val _uiState = MutableStateFlow(TtsState())

    // Public state combines Singleton source of truth with local UI state
    val ttsState = combine(
        _uiState,
        ttsManager.isReady
    ) { state, isReady ->
        state.copy(isReady = isReady)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TtsState())

    init {
        // Listen to real engine events
        viewModelScope.launch {
            ttsManager.events.collect { event ->
                when (event) {
                    is TtsEvent.Speaking -> {
                        _uiState.update { it.copy(isSpeaking = true) }
                    }
                    is TtsEvent.Idle -> {
                        _uiState.update { it.copy(isSpeaking = false) }
                    }
                }
            }
        }
    }

    fun readText(text: String) {
        if (!ttsManager.isReady.value) {
            _uiState.update { it.copy(errorMessage = "Engine not ready") }
            return
        }
        ttsManager.speak(text)
    }

    fun stopReading() {
        ttsManager.stop()
    }

    fun saveAudio(text: String, path: String) {
        if (!ttsManager.isReady.value) {
            _uiState.update { it.copy(errorMessage = "Text-to-Speech engine is not ready") }
            return
        }

        viewModelScope.launch {
            // 1. Set Loading State
            _uiState.update { it.copy(isSavingToFile = true, errorMessage = null, fileSavedPath = null) }

            val file = File(path)
            val result = ttsManager.synthesizeToFile(text, file)

            result.onSuccess { savedFile ->
                // 2. Handle Success
                _uiState.update {
                    it.copy(
                        isSavingToFile = false,
                        fileSavedPath = savedFile.absolutePath
                    )
                }
            }.onFailure { error ->
                // 3. Handle Failure
                _uiState.update {
                    it.copy(
                        isSavingToFile = false,
                        errorMessage = error.message ?: "Unknown error saving audio"
                    )
                }
            }
        }
    }

}