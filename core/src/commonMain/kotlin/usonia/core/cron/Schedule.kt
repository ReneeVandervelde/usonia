package usonia.core.cron

/**
 * Collection of instants that can be filtered down to form a schedule.
 */
data class Schedule(
    val minutes: Set<Int> = (0..59).toSet(),
    val hours: Set<Int> = (0..23).toSet(),
    val days: Set<Int> = (0..31).toSet(),
    val months: Set<Int> = (1..12).toSet(),
) {
    inline fun withMinutes(matching: (Int) -> Boolean) = copy(
        minutes = minutes.filter(matching).toSet()
    )

    inline fun withHours(matching: (Int) -> Boolean) = copy(
        hours = hours.filter(matching).toSet()
    )

    inline fun withDays(matching: (Int) -> Boolean) = copy(
        hours = hours.filter(matching).toSet()
    )

    inline fun withMonths(matching: (Int) -> Boolean) = copy(
        hours = hours.filter(matching).toSet()
    )
}
