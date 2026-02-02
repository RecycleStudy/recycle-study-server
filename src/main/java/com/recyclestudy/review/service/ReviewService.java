package com.recyclestudy.review.service;

import com.recyclestudy.common.BaseEntity;
import com.recyclestudy.exception.UnauthorizedException;
import com.recyclestudy.member.domain.Member;
import com.recyclestudy.member.repository.MemberRepository;
import com.recyclestudy.review.domain.NotificationHistory;
import com.recyclestudy.review.domain.NotificationStatus;
import com.recyclestudy.review.domain.Review;
import com.recyclestudy.review.domain.ReviewCycle;
import com.recyclestudy.review.domain.ReviewCycleDuration;
import com.recyclestudy.review.repository.NotificationHistoryRepository;
import com.recyclestudy.review.repository.ReviewCycleRepository;
import com.recyclestudy.review.repository.ReviewRepository;
import com.recyclestudy.review.service.input.ReviewSaveInput;
import com.recyclestudy.review.service.output.ReviewSaveOutput;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final Clock clock;

    @Transactional
    public ReviewSaveOutput saveReview(final ReviewSaveInput input) {
        final Member member = memberRepository.findByIdentifier(input.identifier())
                .orElseThrow(() -> new UnauthorizedException("유효하지 않은 디바이스입니다"));

        final Review review = Review.withoutId(member, input.url());
        final Review savedReview = reviewRepository.save(review);
        log.info("[REVIEW_SAVED] 복습 주제 저장 성공: reviewId={}", savedReview.getId());

        final LocalDate current = LocalDate.now(clock);
        final List<LocalDateTime> scheduledAts = ReviewCycleDuration.calculate(current);

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
