package com.recyclestudy.review.service.input;

import java.time.LocalDateTime;

public record ReviewSendInput(LocalDateTime scheduledAt) {

    public static ReviewSendInput from(final LocalDateTime targetDateTime) {
        return new ReviewSendInput(targetDateTime);
    }
}
