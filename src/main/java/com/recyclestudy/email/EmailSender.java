package com.recyclestudy.email;

import com.recyclestudy.exception.EmailSendException;
import com.recyclestudy.member.domain.Email;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSender {

    private final JavaMailSender javaMailSender;

    public void send(final Email targetEmail, final String subject, final String content) {
        try {
            final MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            final MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            helper.setTo(targetEmail.getValue());
            helper.setSubject(subject);
            helper.setText(content, true);

            javaMailSender.send(mimeMessage);
            log.info("[MAIL_SENT] 메일 발송 성공: email={}", targetEmail.toMaskedValue());
        } catch (MessagingException e) {
            log.error("[MAIL_SEND_FAILED] 메일 발송 실패: email={}", targetEmail, e);
            throw new EmailSendException("메일 전송 중 오류가 발생했습니다.", e);
        }
    }
}
