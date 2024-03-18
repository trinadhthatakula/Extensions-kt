///kotlinx-datetime = get LocalDateTime at zero hour 
fun LocalDateTime.atZeroHour(): LocalDateTime {
    return LocalDateTime(this.year, this.month, this.dayOfMonth, 0, 0)
}

///kotlinx-datetime = get Millis from localDateTime
fun LocalDateTime.millis(timeZone: TimeZone = TimeZone.currentSystemDefault()): Long {
    return this.toInstant(timeZone).toEpochMilliseconds()
}

///Millis to LocalDateTime
fun Long.toLocalDateTime(
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): LocalDateTime {
    return Instant.fromEpochMilliseconds(this).toLocalDateTime(timeZone)
}

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

///For Multi PLatform projects where we can't use java specific classes like Decimal formatter
///Add decimals to a Number includes int, float, double and long
fun Number.addDecimals(maxDecimals: Int = 2): String {
    return if (!this.toString().contains(".")) {
        var result = this.toInt().toString() + "."
        repeat(maxDecimals) {
            result += "0"
        }
        println("$this -> $result")
        result
    } else {
        val split = this.toString().split(".")
        val decimal = split[1]
        if (decimal.length < maxDecimals) {
            var result = this.toString()
            repeat(maxDecimals - decimal.length) {
                result += "0"
            }
            result
        } else {
            this.toString()
        }
    }
}

///For Multi PLatform projects where we can't use java specific classes like Decimal formatter
//adds prefix zeros to a number like when shoeing time 08 Jan 2026, 01:06 pm
fun Number.addPrefixZeros(maxDigits: Int = 2): String {
    return if (this.toString().length < maxDigits) {
        var result = this.toString()
        repeat(maxDigits - this.toString().length) {
            result = "0$result"
        }
        result
    } else {
        this.toString()
    }
}

//Horizontal Calendar view -  shows date in current month or of any localDateTime passed, 
//shows a dialog for month and year selection
@Preview
@Composable
fun HorizontalCalendar(
    modifier: Modifier = Modifier.fillMaxWidth().background(color = MaterialTheme.colorScheme.primary),
    localDateTime: LocalDateTime = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()),
    onDateSelected: ((LocalDate) -> Unit)? = null
) {

    var selectedDate by remember {
        mutableStateOf<LocalDate?>(null)
    }
    Column(modifier = modifier) {
        val monthInitial = localDateTime.month
        val dayInitial = localDateTime.dayOfMonth
        val yearInitial = localDateTime.year
        var month by remember { mutableStateOf(localDateTime.month) }
        var year by remember { mutableIntStateOf(localDateTime.year) }
        var day by remember { mutableIntStateOf(localDateTime.dayOfMonth) }
        var maxDays by remember { mutableIntStateOf(month.length(year.isLeapYear())) }

        val scrollState = rememberLazyListState()

        var daysList by remember {
            mutableStateOf(emptyList<String>())
        }

        var tempList = emptyList<String>().toMutableList()
        repeat(month.length(year.isLeapYear())) {
            val tempDate = LocalDate(year, month, it + 1)
            tempList.add(tempDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
        }
        daysList = tempList

        LaunchedEffect(day, month, year) {
            if (scrollState.firstVisibleItemIndex != day) {
                scrollState.animateScrollToItem(if (day == 0) 0 else day - 1)
            }
            tempList = emptyList<String>().toMutableList()
            repeat(month.length(year.isLeapYear())) {
                val tempDate = LocalDate(year, month, it + 1)
                tempList.add(tempDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
            }
            daysList = tempList
        }

        Box(
            modifier = Modifier
                .padding(10.dp)
                .align(Alignment.CenterHorizontally)
                .background(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(50))
        ) {

            var showMonthView by remember {
                mutableStateOf(false)
            }

            ConstraintLayout(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        showMonthView = true
                    }
                    .padding(5.dp)
            ) {
                val (prev, next, txt) = createRefs()
                Icon(imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Left",
                    modifier = Modifier
                        .constrainAs(prev) {
                            linkTo(parent.top, parent.bottom)
                            start.linkTo(parent.start)
                        }
                        .padding(5.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (month != Month.JANUARY) {
                                month -= 1
                            } else {
                                month = Month.DECEMBER
                                year -= 1
                            }
                            maxDays = month.length(year.isLeapYear())
                            day = if (month == monthInitial) dayInitial else 0
                        },
                    tint = Color.White
                )
                Icon(imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Right",
                    modifier = Modifier
                        .constrainAs(next) {
                            linkTo(parent.top, parent.bottom)
                            end.linkTo(parent.end)
                        }
                        .padding(5.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (month != Month.DECEMBER) {
                                month += 1
                            } else {
                                month = Month.JANUARY
                                year += 1
                            }
                            maxDays = month.length(year.isLeapYear())
                            day = if (month == monthInitial) dayInitial else 0
                        },
                    tint = Color.White
                )
                Text(
                    text = "${month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} $year",
                    modifier = Modifier
                        .constrainAs(txt) {
                            linkTo(parent.top, parent.bottom)
                            linkTo(prev.end, next.start, startMargin = 10.dp, endMargin = 10.dp)
                            width = Dimension.wrapContent
                        },
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = dmSansFont,
                    color = Color.White
                )
            }

            if(showMonthView) {
                AlertDialog(
                    onDismissRequest = { showMonthView = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    MonthGrid(month, year, yearInitial, monthInitial) { monthSelected, yearSelected ->
                        month = monthSelected
                        year = yearSelected
                        maxDays = month.length(year.isLeapYear())
                        day = if (month == monthInitial) dayInitial else 0
                        showMonthView = false
                    }
                }
            }

        }

        if (daysList.isNotEmpty()) LazyRow(
            state = scrollState,
            modifier = Modifier.padding(top = 10.dp, bottom = 15.dp)
        ) {

            items(maxDays) {
                if (month != Month.FEBRUARY || it != 29 || year.isLeapYear()) {
                    val cDate = LocalDate(year, month, it + 1)
                    Column(modifier = Modifier
                        .padding(5.dp)
                        .width(75.dp)
                        .background(
                            color = if (selectedDate == cDate) Color(0xffFEBD2F) else Color.Transparent,
                            shape = RoundedCornerShape(15.dp)
                        )
                        .clip(RoundedCornerShape(15.dp))
                        .clickable {
                            selectedDate = cDate
                            onDateSelected?.invoke(cDate)
                        }) {

                        Text(
                            text = daysList[it],
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = dmSansFont,
                            modifier = Modifier
                                .padding(vertical = 5.dp, horizontal = 10.dp)
                                .align(Alignment.CenterHorizontally),
                            color = if (selectedDate == cDate) Color.Black else Color.White
                        )

                        Text(
                            text = (it + 1).toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = dmSansFont,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.CenterHorizontally),
                            textAlign = TextAlign.Center,
                            color = if (selectedDate == cDate) Color.Black else Color.White
                        )

                        Box(
                            modifier = Modifier
                                .padding(5.dp)
                                .size(5.dp)
                                .align(Alignment.CenterHorizontally)
                                .clip(
                                    CircleShape
                                )
                                .background(
                                    color = if (day == it + 1) if (selectedDate == cDate) Color.Black else Color(
                                        0xffFEBD2F
                                    )
                                    else Color.Transparent, shape = CircleShape
                                )
                        )
                    }

                }
            }
        }

    }

}


@Preview
@Composable
fun MonthGrid(
    month: Month = Month.JANUARY,
    year: Int = 2023,
    yearInitial: Int = 2023,
    monthInitial: Month = Month.JANUARY,
    onMonthSelected: ((Month, Int) -> Unit)? = null
) {
    var yearState by remember {
        mutableStateOf(year)
    }

    Box{

        var yearGrid by remember {
            mutableStateOf(false)
        }

        ConstraintLayout(
            modifier = Modifier
                .padding(25.dp)
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(5)
                )
                .padding(top = 10.dp)
                .align(Alignment.TopCenter)
        ) {
            val (
                yTxt,
                up,
                down,
                divider,
                monthGrid
            ) = createRefs()
            Text(
                text = "$yearState",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = dmSansFont,
                modifier = Modifier
                    .constrainAs(yTxt) {
                        top.linkTo(parent.top, 10.dp)
                        start.linkTo(parent.start, 25.dp)
                        end.linkTo(down.start)
                        width = Dimension.fillToConstraints
                    }
                    .clickable {
                        yearGrid = true
                    }
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = "",
                modifier = Modifier
                    .constrainAs(up) {
                        end.linkTo(parent.end, 10.dp)
                        top.linkTo(parent.top, 10.dp)
                    }
                    .clickable {
                        yearState++
                    })

            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "",
                modifier = Modifier
                    .constrainAs(down) {
                        end.linkTo(up.start, 10.dp)
                        top.linkTo(parent.top, 10.dp)
                    }
                    .clickable {
                        yearState--
                    })

            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .background(color = Color(0xffFB8C78))
                    .height(2.dp)
                    .fillMaxWidth()
                    .constrainAs(divider) {
                        linkTo(parent.start, parent.end)
                        top.linkTo(yTxt.bottom)
                    }
            )

            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.constrainAs(monthGrid) {
                top.linkTo(divider.bottom, 5.dp)
                bottom.linkTo(parent.bottom, 5.dp)
                linkTo(parent.start, parent.end, startMargin = 5.dp, endMargin = 5.dp)
                width = Dimension.fillToConstraints
            }) {
                items(java.time.Month.values()) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                            .height(40.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(30))
                            .clickable {
                                onMonthSelected?.invoke(it, yearState)
                            }
                            .background(
                                color = if (month == it) Color(0xfffebd2f) else Color.Transparent,
                                shape = RoundedCornerShape(30)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            it.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = dmSansFont,
                            color = if (monthInitial == it && yearInitial == yearState && month!=it) Color(0xfffebd2f) else  Color.Black ,
                        )
                    }
                }
            }


        }

        if(yearGrid){
            AlertDialog(
                onDismissRequest = { yearGrid = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                YearGrid(year){
                    yearState = it
                    yearGrid = false
                }
            }
        }

    }


}

@Preview
@Composable
fun YearGrid(
    year: Int = 2023,
    onYearSelected: ((Int)->Unit)? = null
) {

    var yearState by remember {
        mutableStateOf(year)
    }
    var yearStateStart by remember {
        mutableStateOf(year - 12)
    }

    ConstraintLayout(
        modifier = Modifier
            .padding(25.dp)
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(5)
            )
            .padding(top = 10.dp)
    ) {
        val (
            yTxt,
            up,
            down,
            divider,
            monthGrid
        ) = createRefs()
        Text(
            text = "${yearState-12} - $yearState",
            style = MaterialTheme.typography.titleLarge,
            fontFamily = dmSansFont,
            modifier = Modifier
                .constrainAs(yTxt) {
                    top.linkTo(parent.top, 10.dp)
                    start.linkTo(parent.start, 25.dp)
                    end.linkTo(down.start)
                    width = Dimension.fillToConstraints
                }
        )
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowUp,
            contentDescription = "",
            modifier = Modifier
                .constrainAs(up) {
                    end.linkTo(parent.end, 10.dp)
                    top.linkTo(parent.top, 10.dp)
                }
                .clickable {
                    yearState += 12
                    yearStateStart = yearState - 12
                })

        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = "",
            modifier = Modifier
                .constrainAs(down) {
                    end.linkTo(up.start, 10.dp)
                    top.linkTo(parent.top, 10.dp)
                }
                .clickable {
                    yearState -= 12
                    yearStateStart = yearState - 12
                })

        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .background(color = Color(0xffFB8C78))
                .height(2.dp)
                .fillMaxWidth()
                .constrainAs(divider) {
                    linkTo(parent.start, parent.end)
                    top.linkTo(yTxt.bottom)
                }
        )
        LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.constrainAs(monthGrid) {
            top.linkTo(divider.bottom, 5.dp)
            bottom.linkTo(parent.bottom, 5.dp)
            linkTo(parent.start, parent.end, startMargin = 5.dp, endMargin = 5.dp)
            width = Dimension.fillToConstraints
        }) {
            items(12){
                Box(
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                        .height(40.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(30))
                        .clickable {
                            onYearSelected?.invoke(yearStateStart + it)
                        }
                        .background(
                            color = if (year == it) Color(0xfffebd2f) else Color.Transparent,
                            shape = RoundedCornerShape(30)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${yearStateStart+it}",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = dmSansFont,
                        color = Color.Black ,
                    )
                }
            }
        }
    }
}


val dmSansFont = FontFamily(
    Font(R.font.dmsans_regular, weight = FontWeight.Normal, style = FontStyle.Normal),
    Font(R.font.dmsans_italic, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(R.font.dmsans_black, weight = FontWeight.Black, style = FontStyle.Normal),
    Font(R.font.dmsans_blackitalic, weight = FontWeight.Black, style = FontStyle.Italic),
    Font(R.font.dmsans_medium, weight = FontWeight.Medium, style = FontStyle.Normal),
    Font(R.font.dmsans_mediumitalic, weight = FontWeight.Medium, style = FontStyle.Italic),
    Font(R.font.dmsans_lightitalic, weight = FontWeight.Light, style = FontStyle.Italic),
    Font(R.font.dmsans_light, weight = FontWeight.Light, style = FontStyle.Normal),
    Font(R.font.dmsans_thinitalic, weight = FontWeight.Thin, style = FontStyle.Italic),
    Font(R.font.dmsans_thin, weight = FontWeight.Thin, style = FontStyle.Normal),
    Font(R.font.dmsans_extralightitalic, weight = FontWeight.ExtraLight, style = FontStyle.Italic),
    Font(R.font.dmsans_extralight, weight = FontWeight.ExtraLight, style = FontStyle.Normal),
    Font(R.font.dmsans_bold, weight = FontWeight.Bold, style = FontStyle.Normal),
    Font(R.font.dmsans_bolditalic, weight = FontWeight.Bold, style = FontStyle.Italic),
    Font(R.font.dmsans_semibold, weight = FontWeight.SemiBold, style = FontStyle.Normal),
    Font(R.font.dmsans_semibolditalic, weight = FontWeight.SemiBold, style = FontStyle.Italic),
    Font(R.font.dmsans_extrabold, weight = FontWeight.ExtraBold, style = FontStyle.Normal),
    Font(R.font.dmsans_extrabolditalic, weight = FontWeight.ExtraBold, style = FontStyle.Italic),
)





