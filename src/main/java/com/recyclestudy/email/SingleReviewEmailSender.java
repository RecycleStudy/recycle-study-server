package com.recyclestudy.email;

import com.recyclestudy.member.domain.Email;
import com.recyclestudy.review.domain.NotificationStatus;
import com.recyclestudy.review.domain.ReviewURL;
import com.recyclestudy.review.service.ReviewCycleService;
import com.recyclestudy.review.service.output.ReviewSendOutput.ReviewSendElement;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Component
@RequiredArgsConstructor
public class SingleReviewEmailSender {

    private final EmailSender emailSender;
    private final TemplateEngine templateEngine;
    private final ReviewCycleService reviewCycleService;

    @Async
    public void sendOne(final ReviewSendElement element) {
        final String message = createMessage(element.targetUrls());
        final Email targetEmail = element.email();

        final boolean success = sendToTargetEmail(targetEmail, message);

        if (success) {
            reviewCycleService.updateStatus(element.reviewCycleIds(), NotificationStatus.SENT);
            return;
        }
        reviewCycleService.updateStatus(element.reviewCycleIds(), NotificationStatus.FAILED);
    }

    private boolean sendToTargetEmail(final Email targetEmail, final String message) {
        try {
            emailSender.send(targetEmail, "[Recycle Study] 오늘의 복습 목록이 도착했습니다", message);
            return true;
        } catch (final Exception e) {
            log.error("[REVIEW_MAIL_SEND_FAILED] 복습 메일 발송 실패: email={}", targetEmail.getValue(), e);
            return false;
        }
    }

    private String createMessage(final List<ReviewURL> targetUrls) {
        final Context context = new Context();
        context.setVariable("targetUrls", targetUrls);
        return templateEngine.process("review_email", context);
    }
}
