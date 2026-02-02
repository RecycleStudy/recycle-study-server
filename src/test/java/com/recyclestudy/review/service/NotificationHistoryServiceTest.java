package com.recyclestudy.review.service;

import com.recyclestudy.member.domain.Email;
import com.recyclestudy.member.domain.Member;
import com.recyclestudy.review.domain.NotificationHistory;
import com.recyclestudy.review.domain.NotificationStatus;
import com.recyclestudy.review.domain.Review;
import com.recyclestudy.review.domain.ReviewCycle;
import com.recyclestudy.review.domain.ReviewURL;
import com.recyclestudy.review.repository.NotificationHistoryRepository;
import com.recyclestudy.review.repository.ReviewCycleRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationHistoryServiceTest {

    @Mock
    NotificationHistoryRepository notificationHistoryRepository;

    @Mock
    ReviewCycleRepository reviewCycleRepository;

    @InjectMocks
    NotificationHistoryService notificationHistoryService;

    @Test
    @DisplayName("ReviewCycle ID 목록으로 NotificationHistory를 저장한다")
    void saveAll() {
        // given
        final List<Long> reviewCycleIds = List.of(1L, 2L);
        final NotificationStatus status = NotificationStatus.SENT;

        final Member member = Member.withoutId(Email.from("test@test.com"));
        final Review review = Review.withoutId(member, ReviewURL.from("https://test.com"));
        
        final LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        final ReviewCycle cycle1 = ReviewCycle.withoutId(review, now);
        final ReviewCycle cycle2 = ReviewCycle.withoutId(review, now.plusDays(1));

        given(reviewCycleRepository.findAllById(reviewCycleIds)).willReturn(List.of(cycle1, cycle2));

        // when
        notificationHistoryService.saveAll(reviewCycleIds, status);

        // then
        final ArgumentCaptor<List<NotificationHistory>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationHistoryRepository).saveAll(captor.capture());

        final List<NotificationHistory> savedHistories = captor.getValue();
        assertSoftly(softAssertions -> {
            softAssertions.assertThat(savedHistories).hasSize(2);
            softAssertions.assertThat(savedHistories).allMatch(h -> h.getStatus() == NotificationStatus.SENT);
        });
    }

    @Test
    @DisplayName("빈 ID 목록이면 빈 NotificationHistory 목록을 저장한다")
    void saveAll_emptyIds() {
        // given
        final List<Long> reviewCycleIds = List.of();
        final NotificationStatus status = NotificationStatus.SENT;

        given(reviewCycleRepository.findAllById(reviewCycleIds)).willReturn(List.of());

        // when
        notificationHistoryService.saveAll(reviewCycleIds, status);

        // then
        final ArgumentCaptor<List<NotificationHistory>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationHistoryRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).isEmpty();
    }
}
