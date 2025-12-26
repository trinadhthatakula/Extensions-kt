import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.constraintlayout.compose.ConstraintLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean = false,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = is24Hour
    )
    var showingPicker by remember { mutableStateOf(true) }
    val configuration = LocalWindowInfo.current.containerSize
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10)
                )
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "Pick a time",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth()
            )

            if (showingPicker && configuration.height > 400)
                TimePicker(state = state, modifier = Modifier.padding(10.dp))
            else TimeInput(state = state, modifier = Modifier.padding(10.dp))

            ConstraintLayout(modifier = Modifier.fillMaxWidth()) {
                val (positive, negative, toggle) = createRefs()
                if (configuration.height > 400)
                    IconButton(onClick = { showingPicker = !showingPicker },
                        modifier = Modifier.constrainAs(toggle) {
                            start.linkTo(parent.start, 10.dp)
                            bottom.linkTo(parent.bottom, 10.dp)
                        }) {
                        val icon = if (showingPicker) {
                            Icons.Outlined.Keyboard
                        } else {
                            Icons.Outlined.Schedule
                        }
                        Icon(
                            icon, contentDescription = if (showingPicker) {
                                "Switch to Text Input"
                            } else {
                                "Switch to Touch Input"
                            }
                        )
                    }
                TextButton(
                    onClick = {
                        onTimeSelected(state.hour, state.minute)
                    }, modifier = Modifier.constrainAs(positive) {
                        end.linkTo(parent.end, 10.dp)
                        bottom.linkTo(parent.bottom, 10.dp)
                    }) {
                    Text(text = " Ok ")
                }
                TextButton(
                    onClick = {
                        onDismissRequest()
                    }, modifier = Modifier.constrainAs(negative) {
                        end.linkTo(positive.start, 10.dp)
                        bottom.linkTo(parent.bottom, 10.dp)
                    }) {
                    Text(text = "Cancel")
                }
            }
        }
    }
}