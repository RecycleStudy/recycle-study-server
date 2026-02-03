package com.recyclestudy.email;

import com.recyclestudy.member.domain.Member;
import com.recyclestudy.review.domain.NotificationStatus;
import com.recyclestudy.review.domain.ReviewCycle;
import com.recyclestudy.review.domain.ReviewURL;
import com.recyclestudy.review.repository.ReviewCycleRepository;
import com.recyclestudy.review.service.output.ReviewSendOutput.ReviewSendElement;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailRetryService {

    private static final int MAX_RETRY_COUNT = 3;

    private final ReviewCycleRepository reviewCycleRepository;
    private final SingleReviewEmailSender singleReviewEmailSender;

    @Transactional(readOnly = true)
    public void retryFailedEmails() {
        final List<ReviewCycle> failedCycles = reviewCycleRepository.findAllRetryableCycles(
                MAX_RETRY_COUNT,
                NotificationStatus.SENT
        );

        if (failedCycles.isEmpty()) {
            return;
        }

        log.info("[EMAIL_RETRY] 재시도 대상 {}건 발견", failedCycles.size());

        final Map<Member, List<ReviewCycle>> cyclesByMember = failedCycles.stream()
                .collect(Collectors.groupingBy(reviewCycle -> reviewCycle.getReview().getMember()));

        for (final Map.Entry<Member, List<ReviewCycle>> entry : cyclesByMember.entrySet()) {
            final Member member = entry.getKey();
            final List<ReviewCycle> cycles = entry.getValue();

            final List<ReviewURL> urls = cycles.stream()
                    .map(rc -> rc.getReview().getUrl())
                    .toList();

            final List<Long> cycleIds = cycles.stream()
                    .map(ReviewCycle::getId)
                    .toList();

            final ReviewSendElement element = ReviewSendElement.of(
                    member.getEmail(),
                    cycleIds,
                    urls
            );

            singleReviewEmailSender.sendOne(element);
        }
    }
}
