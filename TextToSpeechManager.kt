
import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

// Holds and manages a TTS instance with lifecycle awareness
@Composable
fun rememberTextToSpeech(): TextToSpeechManager {
    val context = LocalContext.current
    val manager = remember { TextToSpeechManager(context) }
    DisposableEffect(Unit) {
        onDispose { manager.shutdown() }
    }
    return manager
}

class TextToSpeechManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false

    private val pending = ConcurrentHashMap<String, (Result<File>) -> Unit>()

    init {
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                // Set to device default or a specific locale
                val result = tts?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    ready = false
                }
            }
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone(utteranceId: String) {
                pending.remove(utteranceId)?.invoke(
                    Result.success(File(utteranceIdToPath(utteranceId)))
                )
            }

            override fun onError(utteranceId: String) {
                pending.remove(utteranceId)?.invoke(
                    Result.failure(IllegalStateException("TTS error"))
                )
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                pending.remove(utteranceId)?.invoke(
                    Result.failure(IllegalStateException("TTS error: $errorCode"))
                )
            }

            override fun onStart(utteranceId: String) {}

        })
    }

    private fun utteranceIdToPath(id: String): String =
        id.substringAfterLast("|path=")

    fun speak(text: String) {
        if (!ready || text.isBlank()) return
        tts?.stop()
        // QUEUE_FLUSH replaces any current utterance
        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "utterance-${System.currentTimeMillis()}"
        )
    }

    /**
     * Asynchronously synthesizes to file and completes with Result<File> when the file is fully written.
     */
    fun synthesizeToFileResult(text: String, outputFile: File, onResult: (Result<File>) -> Unit) {
        if (!ready) {
            onResult(Result.failure(IllegalStateException("TTS not ready")))
            return
        }
        if (text.isBlank()) {
            onResult(Result.failure(IllegalArgumentException("Text is blank")))
            return
        }
        outputFile.parentFile?.mkdirs()

        // Encode file path in utteranceId so we can retrieve it in onDone
        val utteranceId = "file-${System.currentTimeMillis()}|path=${outputFile.absolutePath}"

        pending[utteranceId] = onResult
        val bundle: Bundle? = null // keep null for modern engines; add params if needed
        val r = tts?.synthesizeToFile(text, bundle, outputFile, utteranceId)
        if (r != TextToSpeech.SUCCESS) {
            pending.remove(utteranceId)
            onResult(Result.failure(IllegalStateException("Synthesis request rejected")))
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        ready = false
    }

}

suspend fun TextToSpeechManager.synthesizeToFileResult(
    text: String,
    outputFile: File,
    onCancellation: (cause: Throwable) -> Unit
): Result<File> = suspendCancellableCoroutine { cont ->
    synthesizeToFileResult(text, outputFile) { result ->
        if (cont.isActive) cont.resume(result) { cause, _, _ -> onCancellation(cause) }
    }
}
