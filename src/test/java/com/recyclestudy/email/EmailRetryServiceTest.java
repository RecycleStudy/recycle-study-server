package com.recyclestudy.email;

import com.recyclestudy.member.domain.Email;
import com.recyclestudy.member.domain.Member;
import com.recyclestudy.review.domain.NotificationStatus;
import com.recyclestudy.review.domain.Review;
import com.recyclestudy.review.domain.ReviewCycle;
import com.recyclestudy.review.domain.ReviewURL;
import com.recyclestudy.review.repository.ReviewCycleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailRetryServiceTest {

    @Mock
    ReviewCycleRepository reviewCycleRepository;

    @Mock
    SingleReviewEmailSender singleReviewEmailSender;

    @Mock
    Clock clock;

    @InjectMocks
    EmailRetryService emailRetryService;

    @Test
    @DisplayName("재시도 대상이 없으면 아무 동작도 하지 않는다")
    void retryFailedEmails_noData() {
        // given
        given(clock.instant()).willReturn(Instant.parse("2026-01-01T00:00:00Z"));
        given(clock.getZone()).willReturn(ZoneId.of("UTC"));
        given(reviewCycleRepository.findAllRetryableCycles(
                eq(NotificationStatus.FAILED), any(Integer.class), any(LocalDateTime.class)))
                .willReturn(Collections.emptyList());

        // when
        emailRetryService.retryFailedEmails();

        // then
        verify(singleReviewEmailSender, never()).sendOne(any());
    }

    @Test
    @DisplayName("재시도 대상을 멤버별로 그룹화하여 메일을 발송한다")
    void retryFailedEmails_success() {
        // given
        given(clock.instant()).willReturn(Instant.parse("2026-01-01T00:00:00Z"));
        given(clock.getZone()).willReturn(ZoneId.of("UTC"));

        final Member member1 = mock(Member.class);
        final Email email1 = Email.from("user1@test.com");
        given(member1.getEmail()).willReturn(email1);

        final Review review1 = mock(Review.class);
        given(review1.getMember()).willReturn(member1);
        given(review1.getUrl()).willReturn(ReviewURL.from("url1"));

        final ReviewCycle cycle1 = mock(ReviewCycle.class);
        given(cycle1.getId()).willReturn(1L);
        given(cycle1.getReview()).willReturn(review1);

        final ReviewCycle cycle2 = mock(ReviewCycle.class);
        given(cycle2.getId()).willReturn(2L);
        given(cycle2.getReview()).willReturn(review1);

        given(reviewCycleRepository.findAllRetryableCycles(
                eq(NotificationStatus.FAILED), any(Integer.class), any(LocalDateTime.class)))
                .willReturn(List.of(cycle1, cycle2));

        // when
        emailRetryService.retryFailedEmails();

        // then
        verify(singleReviewEmailSender, times(1)).sendOne(argThat(element ->
                element.email().equals(email1) &&
                        element.reviewCycleIds().containsAll(List.of(1L, 2L)) &&
                        element.targetUrls().size() == 2
        ));
    }
}
