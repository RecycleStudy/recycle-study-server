package com.recyclestudy.review.service.input;

import com.recyclestudy.member.domain.DeviceIdentifier;
import com.recyclestudy.review.domain.ReviewURL;

public record ReviewSaveInput(DeviceIdentifier identifier, ReviewURL url) {

    public static ReviewSaveInput of(final DeviceIdentifier identifier, final String url) {
        final ReviewURL reviewURL = ReviewURL.from(url);
        return new ReviewSaveInput(identifier, reviewURL);
    }
}
