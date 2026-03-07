package com.recyclestudy.review.service;

import com.recyclestudy.review.domain.NotificationStatus;
import com.recyclestudy.review.domain.ReviewCycle;
import com.recyclestudy.review.repository.NotificationHistoryRepository;
import com.recyclestudy.review.repository.ReviewCycleRepository;
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
public class NotificationHistoryService {

    private static final long LAST_CYCLE_DEADLINE_HOURS = 24;

    private final NotificationHistoryRepository notificationHistoryRepository;
    private final ReviewCycleRepository reviewCycleRepository;
    private final Clock clock;

    @Transactional
    public void updateStatus(final List<Long> reviewCycleIds, final NotificationStatus status) {
        if (reviewCycleIds.isEmpty()) {
            return;
        }
        final LocalDateTime now = LocalDateTime.now(clock);
        int updated;
        if (status == NotificationStatus.FAILED) {
            updated = updateToFailed(reviewCycleIds, now);
        } else {
            updated = notificationHistoryRepository.updateStatus(reviewCycleIds, status, now);
        }
        if (updated != reviewCycleIds.size()) {
            log.warn("[NOTIFY_HIST_MISMATCH] 기대={}, 실제={}", reviewCycleIds.size(), updated);
        }
        log.info("[NOTIFY_HIST_UPDATED] 알림 이력 상태 변경: status={}, count={}", status, updated);
    }

    private int updateToFailed(final List<Long> reviewCycleIds, final LocalDateTime now) {
        int updated = 0;
        for (final Long reviewCycleId : reviewCycleIds) {
            final ReviewCycle reviewCycle = reviewCycleRepository.findById(reviewCycleId)
                    .orElseThrow(() -> new IllegalStateException("ReviewCycle을 찾을 수 없습니다: " + reviewCycleId));
            final LocalDateTime deadline = reviewCycleRepository
                    .findFirstByReview_IdAndScheduledAtGreaterThanOrderByScheduledAtAsc(
                            reviewCycle.getReview().getId(), reviewCycle.getScheduledAt())
                    .map(ReviewCycle::getScheduledAt)
                    .orElse(reviewCycle.getScheduledAt().plusHours(LAST_CYCLE_DEADLINE_HOURS));
            updated += notificationHistoryRepository.updateStatusWithIncrementFailCount(
                    reviewCycleId, NotificationStatus.FAILED, now, deadline);
        }
        return updated;
    }
}
