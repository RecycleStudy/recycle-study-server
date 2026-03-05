package com.recyclestudy.review.service.output;

import java.time.LocalDateTime;

public record NextReviewOutput(LocalDateTime scheduledAt, int count) {

    public static NextReviewOutput empty() {
        return new NextReviewOutput(null, 0);
    }

    public static NextReviewOutput of(final LocalDateTime scheduledAt, final int count) {
        return new NextReviewOutput(scheduledAt, count);
    }
}
