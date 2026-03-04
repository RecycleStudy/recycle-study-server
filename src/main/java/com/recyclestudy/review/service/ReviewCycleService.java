package com.recyclestudy.review.service;

import com.recyclestudy.review.domain.NotificationStatus;
import com.recyclestudy.review.domain.ReviewCycle;
import com.recyclestudy.review.repository.ReviewCycleRepository;
import com.recyclestudy.review.service.input.ReviewSendInput;
import com.recyclestudy.review.service.output.ReviewSendOutput;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewCycleService {

    private final ReviewCycleRepository reviewCycleRepository;

    @Transactional(readOnly = true)
    public ReviewSendOutput findTargetReviewCycle(final ReviewSendInput input) {
        final List<ReviewCycle> targetCycle = reviewCycleRepository.findAllByScheduledAt(
                input.scheduledAt(), NotificationStatus.PENDING);
        return ReviewSendOutput.from(targetCycle);
    }
}
