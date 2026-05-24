package com.recyclestudy.email;

import com.recyclestudy.member.domain.Email;
import com.recyclestudy.review.domain.NotificationStatus;
import com.recyclestudy.review.domain.ReviewURL;
import com.recyclestudy.review.service.ReviewCycleService;
import com.recyclestudy.review.service.output.ReviewSendOutput.ReviewSendElement;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SingleReviewEmailSenderTest {

    @Mock
    private EmailSender emailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private ReviewCycleService reviewCycleService;

    @InjectMocks
    private SingleReviewEmailSender singleReviewEmailSender;

    @Test
    @DisplayName("복습 대상 한 명에게 메일을 발송하고 성공 상태를 저장한다")
    void sendOne_success() {
        // given
        final Email email = Email.from("user@test.com");
        final List<Long> ids = List.of(1L, 2L);
        final List<ReviewURL> urls = List.of(ReviewURL.from("https://example.com"));
        final ReviewSendElement element = ReviewSendElement.of(email, ids, urls);

        given(templateEngine.process(eq("review_email"), any(Context.class))).willReturn("<html></html>");

        // when
        singleReviewEmailSender.sendOne(element);

        // then
        verify(emailSender).send(eq(email), eq("[Recycle Study] 오늘의 복습 목록이 도착했습니다"), any());
        verify(reviewCycleService).updateStatus(ids, NotificationStatus.SENT);
    }

    @Test
    @DisplayName("메일 발송 실패 시 실패 상태를 저장한다")
    void sendOne_failure() {
        // given
        final Email email = Email.from("user@test.com");
        final List<Long> ids = List.of(1L);
        final ReviewSendElement element = ReviewSendElement.of(email, ids, List.of());

        given(templateEngine.process(anyString(), any())).willReturn("<html></html>");
        willThrow(new RuntimeException("SMTP error")).given(emailSender).send(any(), any(), any());

        // when
        singleReviewEmailSender.sendOne(element);

        // then
        verify(reviewCycleService).updateStatus(ids, NotificationStatus.FAILED);
    }

    @Test
    @DisplayName("템플릿 엔진에 올바른 변수가 전달된다")
    void sendOne_templateVariables() {
        // given
        final List<ReviewURL> urls = List.of(
                ReviewURL.from("https://link1.com"),
                ReviewURL.from("https://link2.com")
        );
        final ReviewSendElement element = ReviewSendElement.of(Email.from("user@test.com"), List.of(1L), urls);
        final ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);

        given(templateEngine.process(eq("review_email"), contextCaptor.capture())).willReturn("<html></html>");

        // when
        singleReviewEmailSender.sendOne(element);

        // then
        final Context context = contextCaptor.getValue();
        final List<ReviewURL> targetUrls = (List<ReviewURL>) context.getVariable("targetUrls");

        assertSoftly(softly -> {
            softly.assertThat(targetUrls).hasSize(2);
            softly.assertThat(targetUrls.get(0).getValue()).isEqualTo("https://link1.com");
            softly.assertThat(targetUrls.get(1).getValue()).isEqualTo("https://link2.com");
        });
    }
}
