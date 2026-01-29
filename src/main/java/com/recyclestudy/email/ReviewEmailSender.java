package com.recyclestudy.email;

import com.recyclestudy.review.service.ReviewCycleService;
import com.recyclestudy.review.service.input.ReviewSendInput;
import com.recyclestudy.review.service.output.ReviewSendOutput;
import com.recyclestudy.review.service.output.ReviewSendOutput.ReviewSendElement;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewEmailSender {

    private final SingleReviewEmailSender singleReviewEmailSender;
    private final ReviewCycleService reviewCycleService;
    private final Clock clock;

    @Scheduled(cron = "${schedule.review-mail.cron}", zone = "Asia/Seoul")
    public void sendReviewMail() {

        final LocalDate targetDate = LocalDate.now(clock);
        final LocalTime targetTime = LocalTime.of(8, 0);

        final ReviewSendOutput targetReviewCycle = reviewCycleService.findTargetReviewCycle(
                ReviewSendInput.from(targetDate, targetTime));

        final List<ReviewSendElement> elements = targetReviewCycle.elements();
        log.info("[REVIEW_MAIL_SENT] 복습 메일 발송 시작: date={}, time={}, size={}", targetDate, targetTime, elements.size());

        for (final ReviewSendElement element : elements) {
            singleReviewEmailSender.sendOne(element);
        }

        log.info("[REVIEW_MAIL_SENT] 복습 메일 발송 요청 완료: size={}", elements.size());
    }
}
