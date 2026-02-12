package com.recyclestudy.email;

import com.recyclestudy.exception.EmailSendException;
import com.recyclestudy.member.domain.Email;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("perftest")
@Primary
@ConditionalOnProperty(name = "perftest.email.delay-enabled", havingValue = "true", matchIfMissing = true)
public class DelayEmailSender extends EmailSender {

    private final long delayMs;
    private final long jitterMs;

    public DelayEmailSender(
            final JavaMailSender javaMailSender,
            @Value("${perftest.email.delay-ms:3163}") final long delayMs,
            @Value("${perftest.email.jitter-ms:257}") final long jitterMs
    ) {
        super(javaMailSender);
        this.delayMs = delayMs;
        this.jitterMs = jitterMs;
    }

    @Override
    public void send(final Email targetEmail, final String subject, final String content) {
        try {
            final long actualDelay = delayMs + ThreadLocalRandom.current().nextLong(-jitterMs, jitterMs + 1);
            Thread.sleep(actualDelay);
            log.info("[MAIL_SENT] 메일 발송 성공 (delay={}ms, threadId={}): email={}", actualDelay, Thread.currentThread().getId(), targetEmail.toMaskedValue());
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EmailSendException("메일 전송 시뮬레이션 중 인터럽트 발생", e);
        }
    }
}