package com.recyclestudy.review.controller.response;

import com.recyclestudy.review.service.output.ReviewSaveOutput;
import java.time.Instant;
import java.util.List;

public record ReviewSaveResponse(String url, List<Instant> scheduledAts) {

    public static ReviewSaveResponse from(final ReviewSaveOutput output) {
        return new ReviewSaveResponse(output.url().getValue(), output.scheduledAts());
    }
}
