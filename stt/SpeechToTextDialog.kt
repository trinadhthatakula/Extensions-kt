
import android.Manifest
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import org.koin.androidx.compose.koinViewModel

/**
 * A robust Speech-to-Text dialog with optional Permission handling and Language selection.
 *
 * NOTE: Requires `androidx.activity:activity-compose:1.10.0` or higher for LocalActivity.
 *
 * @param onDismissRequest Called when the user cancels or clicks outside.
 * @param onResult Called with the final text when the user clicks "Done".
 * @param onPickLanguage Optional. If null, the globe icon is hidden.
 * @param onPermissionRequired Optional. If null, the "Grant Permission" button is hidden.
 * @param onOpenSettings Optional. If null, the "Open Settings" button is hidden.
 * @param viewModel Injected automatically via Koin.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeechToTextDialog(
    onDismissRequest: () -> Unit,
    onResult: (String) -> Unit,
    onPickLanguage: (() -> Unit)? = null,
    onPermissionRequired: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    viewModel: SttViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val soundLevel by viewModel.soundLevel.collectAsState()

    // The modern way to get Activity in Compose (Activity Compose 1.10.0+)
    val activity = LocalActivity.current

    // Check if rationale is needed
    val showRationale = remember(state.error, activity) {
        if (activity != null && state.error is SttError.Permission) {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
        } else {
            false
        }
    }

    val isPermissionError = state.error is SttError.Permission

    val animatedScale by animateFloatAsState(
        targetValue = if (state.isActuallyListening) 1f + (soundLevel * 0.6f) else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "voice_pulse"
    )

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- Header with Language Picker ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Voice Input",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Only show language picker if the callback is provided
                    if (onPickLanguage != null) {
                        IconButton(onClick = onPickLanguage) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Change Language",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // --- Live Preview Area ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isPermissionError -> {
                            Text(
                                text = if (showRationale) "Permission Required" else "Access Denied",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // If we are actively listening or requested, show listening/initializing state, otherwise hint
                        state.spokenText.isBlank() && !state.isActuallyListening && !state.isRequested -> {
                            Text(
                                text = "Tap the mic to speak...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {
                            Text(
                                text = state.spokenText.ifBlank { if (state.isActuallyListening) "Listening..." else "..." },
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = if (state.isActuallyListening && state.spokenText.isBlank())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // --- Content Switching: Error vs Mic ---
                if (isPermissionError) {
                    // PERMISSION ERROR UI
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )

                        Text(
                            text = if (showRationale)
                                "We need microphone access to record your journal entry. Please grant permission."
                            else
                                "Microphone access is missing. If you permanently denied it, please open settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Optional Primary Action: Grant
                        if (onPermissionRequired != null) {
                            Button(
                                onClick = onPermissionRequired,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Grant Permission")
                            }
                        }

                        // Optional Secondary Action: Settings
                        if (onOpenSettings != null) {
                            OutlinedButton(
                                onClick = onOpenSettings,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Open Settings")
                            }
                        }
                    }
                } else {
                    // NORMAL MIC UI
                    // FIX: Only show error if we are idle (not listening and not loading)
                    // This prevents stale errors from showing while the user is actively recording
                    if (state.error != null && !state.isActuallyListening && !state.isRequested) {
                        Text(
                            text = state.error!!.message,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(contentAlignment = Alignment.Center) {
                        // Pulsing Circle
                        if (state.isActuallyListening) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .scale(animatedScale)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            )
                        }

                        // Main Button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        state.isActuallyListening -> MaterialTheme.colorScheme.errorContainer
                                        state.isRequested -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> MaterialTheme.colorScheme.primaryContainer
                                    }
                                )
                                .clickable {
                                    if (state.isActuallyListening || state.isRequested) {
                                        viewModel.stopListening()
                                    } else {
                                        viewModel.startListening()
                                    }
                                }
                        ) {
                            if (state.isRequested && !state.isActuallyListening) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (state.isActuallyListening) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = if (state.isActuallyListening) "Stop" else "Start",
                                    tint = if (state.isActuallyListening) MaterialTheme.colorScheme.onErrorContainer
                                    else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = when {
                            state.isActuallyListening -> "Listening..."
                            state.isRequested -> "Initializing..."
                            else -> "Tap to Speak"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // --- Action Buttons (Bottom) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            viewModel.stopListening()
                            onResult(state.spokenText)
                            onDismissRequest()
                        },
                        // Disable "Done" if we have no text, or we are in a permission deadlock
                        enabled = !isPermissionError && state.spokenText.isNotBlank()
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}