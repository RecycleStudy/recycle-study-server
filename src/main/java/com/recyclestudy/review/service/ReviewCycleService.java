package com.recyclestudy.review.service;

import com.recyclestudy.exception.UnauthorizedException;
import com.recyclestudy.member.domain.Member;
import com.recyclestudy.member.repository.MemberRepository;
import com.recyclestudy.review.domain.NotificationStatus;
import com.recyclestudy.review.domain.ReviewCycle;
import com.recyclestudy.review.repository.ReviewCycleRepository;
import com.recyclestudy.review.service.input.NextReviewInput;
import com.recyclestudy.review.service.input.ReviewSendInput;
import com.recyclestudy.review.service.output.NextReviewOutput;
import com.recyclestudy.review.service.output.ReviewSendOutput;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewCycleService {

    private final ReviewCycleRepository reviewCycleRepository;
    private final MemberRepository memberRepository;
    private final Clock clock;

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

        final List<ReviewCycle> allPending = reviewCycleRepository
                .findAllByMemberAndStatus(member.getId(), NotificationStatus.PENDING);

        if (allPending.isEmpty()) {
            return NextReviewOutput.empty();
        }

        final LocalDateTime earliest = allPending.getFirst().getScheduledAt();
        final int count = (int) allPending.stream()
                .takeWhile(rc -> rc.getScheduledAt().equals(earliest))
                .count();

        return NextReviewOutput.of(earliest, count);
    }

    @Transactional
    public void updateStatus(final List<Long> reviewCycleIds, final NotificationStatus status) {
        if (reviewCycleIds.isEmpty()) {
            return;
        }
        final LocalDateTime now = LocalDateTime.now(clock);
        final int updated;
        if (status == NotificationStatus.FAILED) {
            updated = reviewCycleRepository.updateStatusAndIncrementFailCount(reviewCycleIds, status, now);
        } else {
            updated = reviewCycleRepository.updateStatus(reviewCycleIds, status, now);
        }
        if (updated != reviewCycleIds.size()) {
            log.warn("[RC_STATUS_MISMATCH] 기대={}, 실제={}", reviewCycleIds.size(), updated);
        }
        log.info("[RC_STATUS_UPDATED] 상태 변경: status={}, count={}", status, updated);
    }
}
