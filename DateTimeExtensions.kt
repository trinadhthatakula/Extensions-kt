@file:Suppress("unused")

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaDayOfWeek
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toJavaMonth
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

/**
 * Adds [months] to the current Month, wrapping around the year.
 * Handles negative values naturally.
 */
operator fun Month.plus(months: Int): Month {
    val values = Month.entries
    // Standard modular arithmetic: (current + shift) mod 12
    // Added +12 before mod to handle negative shifts correctly
    val nextIndex = (this.ordinal + (months % 12) + 12) % 12
    return values[nextIndex]
}

/**
 * Subtracts [months] from the current Month, wrapping around the year.
 */
operator fun Month.minus(months: Int): Month = this.plus(-months)

/**
 * Increment operator for concise state updates.
 */
operator fun Month.inc(): Month = this.plus(1)

/**
 * Decrement operator for concise state updates.
 */
operator fun Month.dec(): Month = this.minus(1)

/**
 * Gets the display name of the month.
 *
 * @param style The style of the text (FULL, SHORT, NARROW). Default is FULL.
 * @param locale The locale to use. Default is ENGLISH.
 * @return The display name of the month.
 */
fun Month.getDisplayName(
    style: TextStyle = TextStyle.FULL,
    locale: Locale = Locale.ENGLISH
): String = this.toJavaMonth().getDisplayName(style, locale) ?: this.name

/**
 * Returns the length of the month in days.
 *
 * @param leapYear Whether it is a leap year.
 * @return The number of days in the month.
 */
fun Month.length(leapYear: Boolean): Int = this.toJavaMonth().length(leapYear)

/**
 * Gets the display name of the day of the week.
 *
 * @param style The style of the text (FULL, SHORT, NARROW). Default is FULL.
 * @param locale The locale to use. Default is ENGLISH.
 * @return The display name of the day of the week.
 */
fun DayOfWeek.getDisplayName(
    style: TextStyle = TextStyle.FULL,
    locale: Locale = Locale.ENGLISH
): String = this.toJavaDayOfWeek().getDisplayName(style, locale) ?: this.name

/**
 * Converts a Long timestamp (milliseconds) to LocalDateTime.
 *
 * @param timeZone The time zone to use. Default is system default.
 * @return The LocalDateTime corresponding to the timestamp.
 */
fun Long.toLocalDateTime(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDateTime =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(timeZone)

/**
 * Converts a Long timestamp (milliseconds) to LocalDate.
 *
 * @param timeZone The time zone to use. Default is system default.
 * @return The LocalDate corresponding to the timestamp.
 */
fun Long.toLocalDate(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(timeZone).date

/**
 * Converts a Long timestamp (milliseconds) to LocalTime.
 *
 * @param timeZone The time zone to use. Default is system default.
 * @return The LocalTime corresponding to the timestamp.
 */
fun Long.toLocalTime(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalTime =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(timeZone).time

/**
 * Converts LocalDateTime to milliseconds (epoch) from the start of 1970-01-01.
 *
 * @param timeZone The time zone to use. Default is system default.
 * @return The timestamp in milliseconds.
 */
fun LocalDateTime.millis(timeZone: TimeZone = TimeZone.currentSystemDefault()): Long =
    toInstant(timeZone).toEpochMilliseconds()

/**
 * Converts LocalDate to LocalDateTime at the start of the day (00:00).
 *
 * @return The LocalDateTime at 00:00 of the date.
 */
fun LocalDate.asLocalDateTime() = LocalDateTime(year, month, day, 0, 0)

/**
 * Returns the timestamp in milliseconds for the start of the day (00:00).
 *
 * @param timeZone The time zone to use. Default is system default.
 * @return The timestamp in milliseconds.
 */
fun LocalDate.millisAtZeroHour(timeZone: TimeZone = TimeZone.currentSystemDefault()): Long =
    asLocalDateTime().millis(timeZone)

/**
 * Converts LocalTime to milliseconds from the start of the day.
 *
 * @return The total milliseconds since 00:00:00.
 */
fun LocalTime.millis(): Long =
    (hour.toDuration(DurationUnit.HOURS) + minute.toDuration(DurationUnit.MINUTES) + second.toDuration(
        DurationUnit.SECONDS
    )).toLong(DurationUnit.MILLISECONDS)

/**
 * Converts a Long duration in milliseconds to LocalTime.
 *
 * @return The LocalTime representing the duration.
 */
fun Long.toLocalTime(): LocalTime {
    val duration = this.toDuration(DurationUnit.MILLISECONDS)
    val hours = duration.inWholeHours
    val minutes = duration.minus(hours.toDuration(DurationUnit.HOURS)).inWholeMinutes
    val seconds = duration.minus(hours.toDuration(DurationUnit.HOURS))
        .minus(minutes.toDuration(DurationUnit.MINUTES)).inWholeSeconds
    return LocalTime(hours.toInt(), minutes.toInt(), seconds.toInt())
}

/**
 * Returns a new LocalDateTime set to the start of the day (00:00:00).
 *
 * @return The LocalDateTime at the start of the day.
 */
fun LocalDateTime.atZeroHours(): LocalDateTime =
    LocalDateTime(year, month, day, 0, 0)

/**
 * Returns a new LocalDateTime set to the end of the day (23:59:59).
 *
 * @return The LocalDateTime at the end of the day.
 */
fun LocalDateTime.at24Hours(): LocalDateTime =
    LocalDateTime(year, month, day, 23, 59, 59)

/**
 * Formats the LocalDateTime using the specified pattern.
 *
 * @param pattern The date/time pattern to use. The default is "dd MMM yyyy, hh:mm a".
 * @return The formatted string.
 */
fun LocalDateTime.format(pattern: String = "dd MMM yyyy, hh:mm a"): String {
    return DateTimeFormatter
        .ofPattern(pattern, Locale.getDefault())
        .format(toJavaLocalDateTime())
}

/**
 * Formats the LocalDate using the specified pattern.
 *
 * @param pattern The date pattern to use. The default is "dd MMM yyyy".
 * @return The formatted string.
 */
fun LocalDate.format(pattern: String = "dd MMM yyyy"): String {
    return DateTimeFormatter
        .ofPattern(pattern, Locale.getDefault())
        .format(toJavaLocalDate())
}

/**
 * Formats the LocalTime using the specified pattern.
 *
 * @param pattern The time pattern to use. The default is "hh:mm a".
 * @return The formatted string.
 */
fun LocalTime.format(pattern: String = "hh:mm a"): String {
    return DateTimeFormatter
        .ofPattern(pattern, Locale.getDefault())
        .format(toJavaLocalTime())
}

/**
 * Enum representing various date and time format patterns.
 *
 * @property pattern The string pattern.
 */
@Suppress("SpellCheckingInspection")
enum class DatePatterns(val pattern: String) {
    HH_MM_A("hh:mm a"),
    MMMM_YYYY("MMMM yyyy"),
    DD_MMMM_YYYY("dd MMMM yyyy"),
    DD_MMM_YYYY("dd MMM yyyy"),
    DD_MM_YYYY("dd/MM/yyyy"),
    DD_MM_YY("dd/MM/yy"),
    MMM_DD_YYYY("MMM dd, yyyy"),
    MMM_DD_YY("MMM dd, yy"),
    YYYY_MM_DD("yyyy-MM-dd"),
    YYYY_MM_DD_HH_MM("yyyy-MM-dd HH:mm"),
    YYYY_MM_DD_HH_MM_SS("yyyy-MM-dd HH:mm:ss"),
    DD_MMM_YYYY_HH_MM("dd MMM yyyy HH:mm"),
    DD_MMM_YYYY_HH_MM_SS("dd MMM yyyy HH:mm:ss"),
    DD_MM_YYYY_HH_MM("dd/MM/yyyy HH:mm"),
    DD_MM_YYYY_HH_MM_SS("dd/MM/yyyy HH:mm:ss"),
    MMM_DD_YYYY_HH_MM("MMM dd, yyyy HH:mm"),
    MMM_DD_YYYY_HH_MM_SS("MMM dd, yyyy HH:mm:ss"),
    YYYY_MM_DD_T_HH_MM_SS("yyyy-MM-dd'T'HH:mm:ss"),
    DD_MMM_YYYY_T_HH_MM_SS("dd MMM yyyy'T'HH:mm:ss"),
    DD_MM_YYYY_T_HH_MM_SS("dd/MM/yyyy'T'HH:mm:ss"),
    MMM_DD_YYYY_T_HH_MM_SS("MMM dd, yyyy'T'HH:mm:ss"),
    YYYY_MM_DD_T_HH_MM_SS_SSS("yyyy-MM-dd'T'HH:mm:ss.SSS"),
    DD_MMM_YYYY_T_HH_MM_SS_SSS("dd MMM yyyy'T'HH:mm:ss.SSS"),
    DD_MM_YYYY_T_HH_MM_SS_SSS("dd/MM/yyyy'T'HH:mm:ss.SSS"),
    MMM_DD_YYYY_T_HH_MM_SS_SSS("MMM dd, yyyy'T'HH:mm:ss.SSS"),
}

/**
 * Formats the number as a string with a specified number of decimal places.
 *
 * @param maxDecimals The number of decimal places. Default is 2.
 * @return The formatted string.
 */
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
            var result = split[0] + "."
            repeat(maxDecimals) {
                result += decimal[it]
            }
            result
        }
    }
}

/**
 * Formats the number as a string, padding with leading zeros to reach the specified number of digits.
 *
 * @param maxDigits The target number of digits. Default is 2.
 * @return The formatted string with leading zeros if needed.
 */
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

/**
 * Returns the absolute value of the integer.
 *
 * @return The positive value of the integer.
 */
fun Int.makeItPositive() = if (this.toDouble() < 0) -1 * this else this
