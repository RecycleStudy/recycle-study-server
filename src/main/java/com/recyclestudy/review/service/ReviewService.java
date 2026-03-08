package com.recyclestudy.review.service;

import com.recyclestudy.common.BaseEntity;
import com.recyclestudy.cycle.domain.CycleOption;
import com.recyclestudy.cycle.domain.selection.CustomCycleSelection;
import com.recyclestudy.cycle.domain.selection.CycleSelection;
import com.recyclestudy.cycle.repository.CycleOptionRepository;
import com.recyclestudy.cycle.service.resolver.CycleSelectionResolverRegistry;
import com.recyclestudy.exception.NotFoundException;
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
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private static final long LAST_CYCLE_DEADLINE_HOURS = 24;

    private final ReviewRepository reviewRepository;
    private final ReviewCycleRepository reviewCycleRepository;
    private final MemberRepository memberRepository;
    private final CycleOptionRepository cycleOptionRepository;
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

        validateCycleSelectionOwnership(input.cycle(), member);
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
        final List<Duration> durations = cycleSelectionResolverRegistry.resolve(cycleSelection);
        final LocalDateTime baseTime = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);

        return durations.stream()
                .map(duration -> calculateScheduledAt(baseTime, duration, member))
                .toList();
    }

    private LocalDateTime calculateScheduledAt(
            final LocalDateTime baseTime,
            final Duration duration,
            final Member member
    ) {
        final LocalDateTime scheduledAt = baseTime.plus(duration);
        if (duration.toDays() < 1 || member.getNotificationTime() == null) {
            return scheduledAt;
        }
        final LocalDateTime adjustedTime = scheduledAt.with(member.getNotificationTime())
                .truncatedTo(ChronoUnit.MINUTES);
        log.info("[REVIEW_SCHEDULE_ADJUSTED] 복습 주기 시간 조정: original={}, adjusted={}, memberId={}",
                scheduledAt, adjustedTime, member.getId());
        return adjustedTime;
    }

    private void savePendingNotificationHistory(final List<ReviewCycle> savedReviewCycles) {
        final List<NotificationHistory> notificationHistories = IntStream.range(0, savedReviewCycles.size())
                .mapToObj(i -> {
                    final ReviewCycle reviewCycle = savedReviewCycles.get(i);
                    final LocalDateTime deadline = (i < savedReviewCycles.size() - 1)
                            ? savedReviewCycles.get(i + 1).getScheduledAt()
                            : reviewCycle.getScheduledAt().plusHours(LAST_CYCLE_DEADLINE_HOURS);
                    return NotificationHistory.withoutId(reviewCycle, NotificationStatus.PENDING, deadline);
                })
                .toList();
        final List<NotificationHistory> savedNotificationHistories
                = notificationHistoryRepository.saveAll(notificationHistories);
        log.info("[NOTIFY_HIST_SAVED] 전송 현황 등록 성공: status={}, notificationHistoryId={}",
                NotificationStatus.PENDING, savedNotificationHistories.stream().map(BaseEntity::getId).toList());
    }

    private void validateCycleSelectionOwnership(final CycleSelection cycleSelection, final Member member) {
        if (!(cycleSelection instanceof CustomCycleSelection(Long id))) {
            return;
        }

        final CycleOption cycleOption = cycleOptionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 복습 주기입니다"));

        if (!cycleOption.isOwner(member)) {
            throw new NotFoundException("존재하지 않는 복습 주기입니다");
        }
    }
}
