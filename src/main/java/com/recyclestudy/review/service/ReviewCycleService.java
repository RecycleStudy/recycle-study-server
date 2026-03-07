package com.recyclestudy.review.service;

import com.recyclestudy.exception.UnauthorizedException;
import com.recyclestudy.member.domain.Member;
import com.recyclestudy.member.repository.MemberRepository;
import com.recyclestudy.review.domain.NotificationHistory;
import com.recyclestudy.review.domain.NotificationStatus;
import com.recyclestudy.review.domain.ReviewCycle;
import com.recyclestudy.review.repository.NotificationHistoryRepository;
import com.recyclestudy.review.repository.ReviewCycleRepository;
import com.recyclestudy.review.service.input.NextReviewInput;
import com.recyclestudy.review.service.input.ReviewSendInput;
import com.recyclestudy.review.service.output.NextReviewOutput;
import com.recyclestudy.review.service.output.ReviewSendOutput;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewCycleService {

    private final ReviewCycleRepository reviewCycleRepository;
    private final MemberRepository memberRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;

    @Transactional(readOnly = true)
    public ReviewSendOutput findTargetReviewCycle(final ReviewSendInput input) {
        final List<ReviewCycle> targetCycle = reviewCycleRepository.findAllByScheduledAt(
                input.scheduledAt(), NotificationStatus.PENDING);
        return ReviewSendOutput.from(targetCycle);
    }

    @Transactional(readOnly = true)
    public NextReviewOutput findNextReview(final NextReviewInput input) {
        final Member member = memberRepository.findByIdentifier(input.identifier())
                .orElseThrow(() -> new UnauthorizedException("유효하지 않은 디바이스입니다"));

        final List<NotificationHistory> allPending = notificationHistoryRepository
                .findAllByMemberAndStatus(member.getId(), NotificationStatus.PENDING);

        if (allPending.isEmpty()) {
            return NextReviewOutput.empty();
        }

        final LocalDateTime earliest = allPending.getFirst().getReviewCycle().getScheduledAt();
        final int count = (int) allPending.stream()
                .takeWhile(nh -> nh.getReviewCycle().getScheduledAt().equals(earliest))
                .count();

        return NextReviewOutput.of(earliest, count);
    }
}
