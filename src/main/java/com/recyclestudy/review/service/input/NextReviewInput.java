package com.recyclestudy.review.service.input;

import com.recyclestudy.member.domain.DeviceIdentifier;

public record NextReviewInput(DeviceIdentifier identifier) {

    public static NextReviewInput from(final DeviceIdentifier identifier) {
        return new NextReviewInput(identifier);
    }
}
