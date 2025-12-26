
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel


data class SttState(
    val isRequested: Boolean = false,
    val isActuallyListening: Boolean = false,
    val spokenText: String = "",
    val error: SttError? = null
)

class SttViewModel(
    private val sttManager: SpeechToTextManager
) : ViewModel() {

    private val _localState = MutableStateFlow(SttState())

    val uiState: StateFlow<SttState> = combine(
        _localState,
        sttManager.isReadyForSpeech
    ) { state, isReady ->
        state.copy(isActuallyListening = isReady)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SttState()
    )

    val soundLevel: StateFlow<Float> = sttManager.soundLevel

    init {
        observeSttResults()
    }

    private fun observeSttResults() {
        viewModelScope.launch {
            sttManager.sttEvents.collect { result ->
                result.fold(
                    onSuccess = { text ->
                        _localState.update {
                            it.copy(
                                isRequested = false,
                                spokenText = text,
                                error = null
                            )
                        }
                    },
                    onFailure = { throwable ->
                        // Unwrap the SttException to get the clean Sealed Class
                        val error = (throwable as? SttException)?.error
                            ?: SttError.Unknown(throwable.message ?: "Unknown error")

                        _localState.update {
                            it.copy(
                                isRequested = false,
                                error = error
                            )
                        }
                    }
                )
            }
        }
    }

    fun startListening() {
        _localState.update { it.copy(isRequested = true, error = null) }
        sttManager.startListening()
    }

    fun stopListening() {
        sttManager.stopListening()
    }

    override fun onCleared() {
        super.onCleared()
        sttManager.shutdown()
    }
}