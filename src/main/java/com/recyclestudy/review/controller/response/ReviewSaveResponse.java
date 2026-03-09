package com.recyclestudy.review.controller.response;

import com.recyclestudy.review.domain.ReviewURL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public record ReviewSaveResponse(String url, List<Instant> scheduledAts) {

    public static ReviewSaveResponse of(ReviewURL url, List<LocalDateTime> scheduledAts) {
        return new ReviewSaveResponse(url.getValue(),
                scheduledAts.stream().map(scheduledAt -> scheduledAt.toInstant(ZoneOffset.UTC)).toList());
    }
}
