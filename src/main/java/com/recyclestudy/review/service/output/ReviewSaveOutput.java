package com.recyclestudy.review.service.output;

import com.recyclestudy.review.domain.ReviewURL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public record ReviewSaveOutput(ReviewURL url, List<Instant> scheduledAts) {

    public static ReviewSaveOutput of(ReviewURL url, List<LocalDateTime> scheduledAts) {
        return new ReviewSaveOutput(url,
                scheduledAts.stream().map(scheduledAt -> scheduledAt.toInstant(ZoneOffset.UTC)).toList());
    }
}
