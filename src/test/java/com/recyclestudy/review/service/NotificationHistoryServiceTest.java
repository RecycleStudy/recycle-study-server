package com.recyclestudy.review.service;

import com.recyclestudy.review.domain.NotificationStatus;
import com.recyclestudy.review.repository.NotificationHistoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationHistoryServiceTest {

    @Mock
    NotificationHistoryRepository notificationHistoryRepository;

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
    @DisplayName("FAILED 상태로 업데이트하면 배치 쿼리로 failCount를 1 증가시킨다")
    void updateStatus_failed() {
        // given
        final List<Long> reviewCycleIds = List.of(1L, 2L);
        BDDMockito.given(clock.instant()).willReturn(Instant.parse("2026-01-01T00:00:00Z"));
        BDDMockito.given(clock.getZone()).willReturn(ZoneId.of("UTC"));

        // when
        notificationHistoryService.updateStatus(reviewCycleIds, NotificationStatus.FAILED);

        // then
        verify(notificationHistoryRepository).updateStatusAndIncrementFailCount(
                eq(reviewCycleIds), eq(NotificationStatus.FAILED), any(LocalDateTime.class));
    }
}
