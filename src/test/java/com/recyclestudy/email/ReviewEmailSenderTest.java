package com.recyclestudy.email;

import com.recyclestudy.member.domain.Email;
import com.recyclestudy.review.domain.ReviewURL;
import com.recyclestudy.review.service.ReviewCycleService;
import com.recyclestudy.review.service.output.ReviewSendOutput;
import com.recyclestudy.review.service.output.ReviewSendOutput.ReviewSendElement;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewEmailSenderTest {

    @Mock
    SingleReviewEmailSender singleReviewEmailSender;

    @Mock
    ReviewCycleService reviewCycleService;

    @Spy
    Clock clock = Clock.fixed(Instant.parse("2025-01-01T08:00:00Z"), ZoneId.of("UTC"));

    @InjectMocks
    ReviewEmailSender reviewEmailSender;

    @Test
    @DisplayName("복습 대상자에게 비동기 메일 발송을 요청한다")
    void sendReviewMail_success() {
        // given
        final Email targetEmail = Email.from("user@test.com");
        final List<Long> reviewCycleIds = List.of(1L, 2L);
        final List<ReviewURL> targetUrls = List.of(
                ReviewURL.from("https://example.com/article1"),
                ReviewURL.from("https://example.com/article2")
        );
        final ReviewSendElement element = ReviewSendElement.of(targetEmail, reviewCycleIds, targetUrls);
        final ReviewSendOutput output = new ReviewSendOutput(List.of(element));

        given(reviewCycleService.findTargetReviewCycle(any())).willReturn(output);

        // when
        reviewEmailSender.sendReviewMail();

        // then
        verify(singleReviewEmailSender).sendOne(element);
    }

    @Test
    @DisplayName("여러 대상자에게 각각 비동기 요청을 보낸다")
    void sendReviewMail_multipleRecipients() {
        // given
        final ReviewSendElement element1 = ReviewSendElement.of(
                Email.from("user1@test.com"),
                List.of(1L),
                List.of(ReviewURL.from("https://example.com/1"))
        );
        final ReviewSendElement element2 = ReviewSendElement.of(
                Email.from("user2@test.com"),
                List.of(2L),
                List.of(ReviewURL.from("https://example.com/2"))
        );
        final ReviewSendOutput output = new ReviewSendOutput(List.of(element1, element2));

        given(reviewCycleService.findTargetReviewCycle(any())).willReturn(output);

        // when
        reviewEmailSender.sendReviewMail();

        // then
        verify(singleReviewEmailSender, times(2)).sendOne(any());
        verify(singleReviewEmailSender).sendOne(element1);
        verify(singleReviewEmailSender).sendOne(element2);
    }

    @Test
    @DisplayName("복습 대상이 없으면 요청을 보내지 않는다")
    void sendReviewMail_noRecipients() {
        // given
        final ReviewSendOutput output = new ReviewSendOutput(List.of());

        given(reviewCycleService.findTargetReviewCycle(any())).willReturn(output);

        // when
        reviewEmailSender.sendReviewMail();

        // then
        verify(singleReviewEmailSender, never()).sendOne(any());
    }

    @Test
    @DisplayName("메일 발송은 비동기적으로 처리되어 전체 시간이 지연되지 않아야 한다 (Non-blocking Expectation)")
    void sendReviewMail_executesAsynchronously() {
        // given
        final ReviewSendElement user1 = ReviewSendElement.of(Email.from("user1@test.com"), List.of(1L), List.of());
        final ReviewSendElement user2 = ReviewSendElement.of(Email.from("user2@test.com"), List.of(2L), List.of());
        final ReviewSendOutput output = new ReviewSendOutput(List.of(user1, user2));

        given(reviewCycleService.findTargetReviewCycle(any())).willReturn(output);

        // then
        assertTimeout(Duration.ofMillis(100), () -> {
            reviewEmailSender.sendReviewMail();
        }, "메일 발송 요청은 즉시 완료되어야 합니다.");

        verify(singleReviewEmailSender, times(2)).sendOne(any());
    }
}
