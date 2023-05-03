import androidx.annotation.RawRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.airbnb.lottie.compose.*

///LazyGrid doesn't have a header while lazyRow has one so why not write our own header implementation
@LazyGridScopeMarker
fun LazyGridScope.header(
    content: @Composable LazyGridItemScope.() -> Unit
) {
    item(span = { GridItemSpan(this.maxLineSpan) }, content = content)
}

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

///Custom tab indicator pill 💊 style
@Composable
private fun CustomTabIndicator(tabPositions: List<TabPosition>, tabTitle: TabPage) {
    val transition = updateTransition(targetState = tabTitle, label = "Tab indicator")
    val indicatorLeft by transition.animateDp(
        transitionSpec = {
            if (TabPage.Address isTransitioningTo TabPage.Maps) {
                spring(stiffness = Spring.StiffnessLow)
            } else {
                spring(stiffness = Spring.StiffnessMedium)
            }
        }, label = "Indicator Left"
    ) { page ->
        tabPositions[page.ordinal].left
    }
    val indicatorRight by transition.animateDp(
        transitionSpec = {
            if (TabPage.Address isTransitioningTo TabPage.Maps) {
                spring(stiffness = Spring.StiffnessMedium)
            } else {
                spring(stiffness = Spring.StiffnessLow)
            }
        }, label = "Indicator Right"
    ) { page ->
        tabPositions[page.ordinal].right
    }
    val color = MaterialTheme.colorScheme.primary
    Box(
        Modifier
            .fillMaxHeight()
            .wrapContentSize(align = Alignment.BottomStart)
            .offset(x = indicatorLeft)
            .width(indicatorRight - indicatorLeft)
            .fillMaxHeight()
            .background(color = color, shape = RoundedCornerShape(50.dp))
            .zIndex(1f)
    )
}
