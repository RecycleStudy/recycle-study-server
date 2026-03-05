package com.recyclestudy.review.controller.response;

import com.recyclestudy.review.service.output.NextReviewOutput;
import java.time.LocalDateTime;

public record NextReviewResponse(LocalDateTime scheduledAt, int count) {

    public static NextReviewResponse of(final NextReviewOutput output) {
        return new NextReviewResponse(output.scheduledAt(), output.count());
    }
}
