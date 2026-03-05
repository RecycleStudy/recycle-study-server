package com.recyclestudy.review.controller;

import com.recyclestudy.common.annotation.AuthDevice;
import com.recyclestudy.member.domain.DeviceIdentifier;
import com.recyclestudy.review.controller.request.ReviewSaveRequest;
import com.recyclestudy.review.controller.response.NextReviewResponse;
import com.recyclestudy.review.controller.response.ReviewSaveResponse;
import com.recyclestudy.review.service.ReviewCycleService;
import com.recyclestudy.review.service.ReviewService;
import com.recyclestudy.review.service.input.NextReviewInput;
import com.recyclestudy.review.service.input.ReviewSaveInput;
import com.recyclestudy.review.service.output.NextReviewOutput;
import com.recyclestudy.review.service.output.ReviewSaveOutput;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewCycleService reviewCycleService;

    @PostMapping
    public ResponseEntity<ReviewSaveResponse> saveReview(
            @AuthDevice final DeviceIdentifier identifier,
            @RequestBody ReviewSaveRequest request
    ) {
        final ReviewSaveInput input = ReviewSaveInput.of(identifier, request.targetUrl(), request.cycle());
        final ReviewSaveOutput output = reviewService.saveReview(input);
        ReviewSaveResponse response = ReviewSaveResponse.of(output.url(), output.scheduledAts());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/next")
    public ResponseEntity<NextReviewResponse> findNextReview(
            @AuthDevice final DeviceIdentifier identifier
    ) {
        final NextReviewOutput output = reviewCycleService.findNextReview(NextReviewInput.from(identifier));
        return ResponseEntity.ok(NextReviewResponse.of(output));
    }
}
