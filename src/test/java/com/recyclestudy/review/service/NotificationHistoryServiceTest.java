package com.recyclestudy.review.service;

import com.recyclestudy.review.domain.NotificationStatus;
import com.recyclestudy.review.domain.Review;
import com.recyclestudy.review.domain.ReviewCycle;
import com.recyclestudy.review.repository.NotificationHistoryRepository;
import com.recyclestudy.review.repository.ReviewCycleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationHistoryServiceTest {

    @Mock
    NotificationHistoryRepository notificationHistoryRepository;

    @Mock
    ReviewCycleRepository reviewCycleRepository;

    @Mock
    Clock clock;

    @InjectMocks
    NotificationHistoryService notificationHistoryService;

    @Test
    @DisplayName("SENT 상태로 업데이트한다")
    void updateStatus_sent() {
        // given
        final List<Long> reviewCycleIds = List.of(1L, 2L);
        BDDMockito.given(clock.instant())
                .willReturn(Instant.parse("2026-01-01T00:00:00Z"));
        BDDMockito.given(clock.getZone())
                .willReturn(ZoneId.of("UTC"));

        // when
        notificationHistoryService.updateStatus(reviewCycleIds, NotificationStatus.SENT);

        // then
        verify(notificationHistoryRepository).updateStatus(
                eq(reviewCycleIds), eq(NotificationStatus.SENT), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("FAILED 상태로 업데이트하면 deadline과 함께 failCount를 1 증가시킨다")
    void updateStatus_failed() {
        // given
        final Long reviewCycleId = 1L;
        final LocalDateTime scheduledAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        final LocalDateTime nextScheduledAt = LocalDateTime.of(2026, 1, 2, 10, 0);

        final Review review = mock(Review.class);
        BDDMockito.given(review.getId()).willReturn(100L);

        final ReviewCycle reviewCycle = mock(ReviewCycle.class);
        BDDMockito.given(reviewCycle.getReview()).willReturn(review);
        BDDMockito.given(reviewCycle.getScheduledAt()).willReturn(scheduledAt);

        BDDMockito.given(clock.instant()).willReturn(Instant.parse("2026-01-01T00:00:00Z"));
        BDDMockito.given(clock.getZone()).willReturn(ZoneId.of("UTC"));
        final ReviewCycle nextReviewCycle = mock(ReviewCycle.class);
        BDDMockito.given(nextReviewCycle.getScheduledAt()).willReturn(nextScheduledAt);

        BDDMockito.given(reviewCycleRepository.findById(reviewCycleId))
                .willReturn(Optional.of(reviewCycle));
        BDDMockito.given(reviewCycleRepository
                .findFirstByReview_IdAndScheduledAtGreaterThanOrderByScheduledAtAsc(review.getId(), scheduledAt))
                .willReturn(Optional.of(nextReviewCycle));

        // when
        notificationHistoryService.updateStatus(List.of(reviewCycleId), NotificationStatus.FAILED);

        // then
        verify(notificationHistoryRepository).updateStatusWithIncrementFailCount(
                eq(reviewCycleId), eq(NotificationStatus.FAILED), any(LocalDateTime.class), eq(nextScheduledAt));
    }

    @Test
    @DisplayName("마지막 주기이면 deadline을 scheduledAt + 24h로 설정한다")
    void updateStatus_failed_lastCycle() {
        // given
        final Long reviewCycleId = 1L;
        final LocalDateTime scheduledAt = LocalDateTime.of(2026, 1, 30, 10, 0);
        final LocalDateTime expectedDeadline = scheduledAt.plusHours(24);

        final Review review = mock(Review.class);
        BDDMockito.given(review.getId()).willReturn(100L);

        final ReviewCycle reviewCycle = mock(ReviewCycle.class);
        BDDMockito.given(reviewCycle.getReview()).willReturn(review);
        BDDMockito.given(reviewCycle.getScheduledAt()).willReturn(scheduledAt);

        BDDMockito.given(clock.instant()).willReturn(Instant.parse("2026-01-30T00:00:00Z"));
        BDDMockito.given(clock.getZone()).willReturn(ZoneId.of("UTC"));
        BDDMockito.given(reviewCycleRepository.findById(reviewCycleId))
                .willReturn(Optional.of(reviewCycle));
        BDDMockito.given(reviewCycleRepository
                .findFirstByReview_IdAndScheduledAtGreaterThanOrderByScheduledAtAsc(review.getId(), scheduledAt))
                .willReturn(Optional.empty());

        // when
        notificationHistoryService.updateStatus(List.of(reviewCycleId), NotificationStatus.FAILED);

        // then
        verify(notificationHistoryRepository).updateStatusWithIncrementFailCount(
                eq(reviewCycleId), eq(NotificationStatus.FAILED), any(LocalDateTime.class), eq(expectedDeadline));
    }
}
