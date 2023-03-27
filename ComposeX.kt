import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.airbnb.lottie.compose.*

///Compose function to animate Lottie Raw file easily
@Composable
fun AnimateLottieRaw(
    modifier: Modifier = Modifier,
    @RawRes resId: Int,
    shouldLoop: Boolean = false,
    repeatCount: Int = LottieConstants.IterateForever
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resId))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = if (shouldLoop) repeatCount else 1
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier,
    )
}
