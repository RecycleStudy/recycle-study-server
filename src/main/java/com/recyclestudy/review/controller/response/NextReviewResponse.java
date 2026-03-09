package com.recyclestudy.review.controller.response;

import com.recyclestudy.review.service.output.NextReviewOutput;
import java.time.Instant;
import java.time.ZoneOffset;

public record NextReviewResponse(Instant scheduledAt, int count) {

    public static NextReviewResponse of(final NextReviewOutput output) {
        final Instant scheduledAt = output.scheduledAt() != null
                ? output.scheduledAt().toInstant(ZoneOffset.UTC)
                : null;
        return new NextReviewResponse(scheduledAt, output.count());
    }
}
