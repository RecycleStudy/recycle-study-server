package com.recyclestudy.review.service;

import com.recyclestudy.common.BaseEntity;
import com.recyclestudy.cycle.domain.selection.CycleSelection;
import com.recyclestudy.cycle.domain.selection.DefaultCycleSelection;
import com.recyclestudy.cycle.service.resolver.CycleSelectionResolverRegistry;
import com.recyclestudy.exception.UnauthorizedException;
import com.recyclestudy.member.domain.Member;
import com.recyclestudy.member.repository.MemberRepository;
import com.recyclestudy.review.domain.NotificationHistory;
import com.recyclestudy.review.domain.NotificationStatus;
import com.recyclestudy.review.domain.Review;
import com.recyclestudy.review.domain.ReviewCycle;
import com.recyclestudy.review.repository.NotificationHistoryRepository;
import com.recyclestudy.review.repository.ReviewCycleRepository;
import com.recyclestudy.review.repository.ReviewRepository;
import com.recyclestudy.review.service.input.ReviewSaveInput;
import com.recyclestudy.review.service.output.ReviewSaveOutput;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewCycleRepository reviewCycleRepository;
    private final MemberRepository memberRepository;
    private final CycleSelectionResolverRegistry cycleSelectionResolverRegistry;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final Clock clock;

    @Transactional
    public ReviewSaveOutput saveReview(final ReviewSaveInput input) {
        final Member member = memberRepository.findByIdentifier(input.identifier())
                .orElseThrow(() -> new UnauthorizedException("유효하지 않은 디바이스입니다"));

        final Review review = Review.withoutId(member, input.url());
        final Review savedReview = reviewRepository.save(review);
        log.info("[REVIEW_SAVED] 복습 주제 저장 성공: reviewId={}", savedReview.getId());

        final List<LocalDateTime> scheduledAts = calculateScheduledAts(input.cycle(), member);

        final List<ReviewCycle> reviewCycles = scheduledAts.stream()
                .map(scheduledAt -> ReviewCycle.withoutId(savedReview, scheduledAt))
                .toList();

        final List<ReviewCycle> savedReviewCycles = reviewCycleRepository.saveAll(reviewCycles);
        final List<LocalDateTime> savedScheduledAts = savedReviewCycles.stream()
                .map(ReviewCycle::getScheduledAt)
                .toList();
        log.info("[REVIEW_CYCLE_SAVED] 복습 주기 저장 성공: reviewCycleId={}",
                savedReviewCycles.stream().map(BaseEntity::getId).toList());

        savePendingNotificationHistory(savedReviewCycles);

        return ReviewSaveOutput.of(savedReview.getUrl(), savedScheduledAts);
    }

    private List<LocalDateTime> calculateScheduledAts(final CycleSelection cycleSelection, final Member member) {
        final CycleSelection resolvedCycle = resolveDefaultCycleIfNull(cycleSelection);
        final List<Duration> durations = cycleSelectionResolverRegistry.resolve(resolvedCycle);
        final LocalDateTime baseTime = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);

        return durations.stream()
                .map(duration -> calculateScheduledAt(baseTime, duration, member))
                .toList();
    }

    private LocalDateTime calculateScheduledAt(final LocalDateTime baseTime, final Duration duration, final Member member) {
        final LocalDateTime scheduledAt = baseTime.plus(duration);
        if (duration.toDays() < 1 || member.getNotificationTime() == null) {
            return scheduledAt;
        }
        final LocalDateTime adjustedTime = scheduledAt.with(member.getNotificationTime()).truncatedTo(ChronoUnit.MINUTES);
        log.info("[REVIEW_SCHEDULE_ADJUSTED] 복습 주기 시간 조정: original={}, adjusted={}, memberId={}",
                scheduledAt, adjustedTime, member.getId());
        return adjustedTime;
    }

    @Deprecated // 프론트 마이그레이션 완료 후 제거 예정
    private CycleSelection resolveDefaultCycleIfNull(final CycleSelection cycleSelection) {
        if (cycleSelection != null) {
            return cycleSelection;
        }
        return new DefaultCycleSelection("EBBINGHAUS");
    }

    private void savePendingNotificationHistory(final List<ReviewCycle> savedReviewCycles) {
        final List<NotificationHistory> notificationHistories = savedReviewCycles.stream()
                .map(reviewCycle -> NotificationHistory.withoutId(reviewCycle, NotificationStatus.PENDING))
                .toList();
        final List<NotificationHistory> savedNotificationHistories
                = notificationHistoryRepository.saveAll(notificationHistories);
        log.info("[NOTIFY_HIST_SAVED] 전송 현황 등록 성공: status={}, notificationHistoryId={}",
                NotificationStatus.PENDING, savedNotificationHistories.stream().map(BaseEntity::getId).toList());
    }
}
