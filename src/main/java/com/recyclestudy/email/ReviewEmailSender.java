package com.recyclestudy.email;

import com.recyclestudy.review.service.ReviewCycleService;
import com.recyclestudy.review.service.input.ReviewSendInput;
import com.recyclestudy.review.service.output.ReviewSendOutput;
import com.recyclestudy.review.service.output.ReviewSendOutput.ReviewSendElement;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewEmailSender {

    private final SingleReviewEmailSender singleReviewEmailSender;
    private final ReviewCycleService reviewCycleService;
    private final Clock clock;

    @Scheduled(cron = "${schedule.review-mail.cron}", zone = "UTC")
    public void sendReviewMail() {
        final LocalDateTime targetDateTime = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);

        final ReviewSendOutput targetReviewCycle = reviewCycleService.findTargetReviewCycle(
                ReviewSendInput.from(targetDateTime));

        final List<ReviewSendElement> elements = targetReviewCycle.elements();
        log.info("[REVIEW_MAIL_SENT] 복습 메일 발송 시작: datetime={}, size={}", targetDateTime, elements.size());

        for (final ReviewSendElement element : elements) {
            singleReviewEmailSender.sendOne(element);
        }

        log.info("[REVIEW_MAIL_SENT] 복습 메일 발송 요청 완료: size={}", elements.size());
    }
}
