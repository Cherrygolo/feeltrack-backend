package ld.feeltrack_backend.dto;

import java.time.LocalDate;

/**
 * DTO for representing the number of reviews of each type (positive, negative, neutral) for a specific date.
 * Used in the review timeline endpoint to provide temporal statistics.
 */

public class ReviewTimelineDTO {

    private LocalDate createdDate;
    private long positive;
    private long negative;
    private long neutral;

    public ReviewTimelineDTO(LocalDate createdDate, long positive, long negative, long neutral) {
        this.createdDate = createdDate;
        this.positive = positive;
        this.negative = negative;
        this.neutral = neutral;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
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
