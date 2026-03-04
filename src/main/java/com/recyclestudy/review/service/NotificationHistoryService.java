package com.recyclestudy.review.service;

import com.recyclestudy.review.domain.NotificationStatus;
import com.recyclestudy.review.repository.NotificationHistoryRepository;
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

    private final NotificationHistoryRepository notificationHistoryRepository;
    private final Clock clock;

    @Transactional
    public void updateStatus(final List<Long> reviewCycleIds, final NotificationStatus status) {
        final LocalDateTime now = LocalDateTime.now(clock);
        if (reviewCycleIds.isEmpty()) {
            return;
        }
        if (status == NotificationStatus.FAILED) {
            notificationHistoryRepository.updateStatusWithIncrementFailCount(reviewCycleIds, status, now);
        } else {
            notificationHistoryRepository.updateStatus(reviewCycleIds, status, now);
        }
        log.info("[NOTIFY_HIST_UPDATED] 알림 이력 상태 변경: status={}, count={}", status, reviewCycleIds.size());
    }
}
