package ld.feeltrack_backend.analytics;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for aggregating timelines based on different granularities (day, week, month).
 * Used for analytics and dashboard purposes to group data by time intervals.
 */

public class TimeSeriesGranularity {

    /* 
     * Normalize a date according to a given granularity
     * @param date the date to normalize
     * @param granularity the granularity to apply (DAY, WEEK, MONTH)
     * @return the normalized date
    */
    public static LocalDate normalize(LocalDate date, Granularity granularity) {

        return switch (granularity) {

            case DAY -> date;

            case WEEK ->
                date.with(DayOfWeek.MONDAY); // début semaine

            case MONTH ->
                date.withDayOfMonth(1); // début mois
        };
    }

    /* 
     * Increment a date according to a given granularity
     * @param date the date to increment
     * @param granularity the granularity to apply (DAY, WEEK, MONTH)
     * @return the incremented date
    */
    public static LocalDate increment(LocalDate date, Granularity granularity) {

        return switch (granularity) {

            case DAY -> date.plusDays(1);
            case WEEK -> date.plusWeeks(1);
            case MONTH -> date.plusMonths(1);
        };
    }

    /* 
     * Build a timeline between two dates according to a given granularity
     * @param from the start date
     * @param to the end date
     * @param granularity the granularity to apply (DAY, WEEK, MONTH)
     * @return the list of dates in the timeline
    */
    public static List<LocalDate> buildTimeline(
            LocalDate from,
            LocalDate to,
            Granularity granularity
    ) {

        List<LocalDate> timeline = new ArrayList<>();

        LocalDate current = normalize(from, granularity);
        LocalDate end = normalize(to, granularity);

        while (!current.isAfter(end)) {
            timeline.add(current);
            current = increment(current, granularity);
        }

        return timeline;
    }
}
