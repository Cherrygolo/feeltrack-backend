package ld.feeltrack_backend.dto;

import java.time.LocalDate;

/**
 * DTO for representing the number of reviews of each type (positive, negative, neutral) for a specific date.
 * Used in the review timeline endpoint to provide temporal statistics.
 */

public class ReviewTimelineItemDTO {

    private final LocalDate startingPeriodDate;
    private long positive;
    private long negative;
    private long neutral;

    public ReviewTimelineItemDTO(LocalDate startingPeriodDate, long positive, long negative, long neutral) {
        this.startingPeriodDate = startingPeriodDate;
        this.positive = positive;
        this.negative = negative;
        this.neutral = neutral;
    }

    public LocalDate getStartingPeriodDate() {
        return startingPeriodDate;
    }

    public long getPositive() {
        return positive;
    }

    public long getNegative() {
        return negative;
    }

    public long getNeutral() {
        return neutral;
    }

    public void addPositive(long value) {
        this.positive += value;
    }

    public void addNegative(long value) {
        this.negative += value;
    }

    public void addNeutral(long value) {
        this.neutral += value;
    }
}
