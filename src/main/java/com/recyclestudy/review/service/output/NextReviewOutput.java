package com.recyclestudy.review.service.output;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public record NextReviewOutput(Instant scheduledAt, int count) {

    public static NextReviewOutput empty() {
        return new NextReviewOutput(null, 0);
    }

    public static NextReviewOutput of(final LocalDateTime scheduledAt, final int count) {
        return new NextReviewOutput(scheduledAt.toInstant(ZoneOffset.UTC), count);
    }
}
