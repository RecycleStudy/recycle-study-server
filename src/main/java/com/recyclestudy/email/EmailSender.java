package com.recyclestudy.email;

import com.recyclestudy.exception.EmailSendException;
import com.recyclestudy.member.domain.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSender {

    private static final String FROM_ADDRESS = "noreply@recycle-study.site";

    private final SesV2Client sesV2Client;

    public void send(final Email targetEmail, final String subject, final String content) {
        final SendEmailRequest request = SendEmailRequest.builder()
                .fromEmailAddress(FROM_ADDRESS)
                .destination(Destination.builder()
                        .toAddresses(targetEmail.getValue())
                        .build())
                .content(EmailContent.builder()
                        .simple(Message.builder()
                                .subject(Content.builder().data(subject).charset("UTF-8").build())
                                .body(Body.builder()
                                        .html(Content.builder().data(content).charset("UTF-8").build())
                                        .build())
                                .build())
                        .build())
                .build();
        try {
            sesV2Client.sendEmail(request);
            log.info("[MAIL_SENT] 메일 발송 성공: email={}", targetEmail.toMaskedValue());
        } catch (final SesV2Exception e) {
            log.error("[MAIL_SEND_FAILED] 메일 발송 실패: email={}", targetEmail.toMaskedValue(), e);
            throw new EmailSendException("메일 전송 중 오류가 발생했습니다.", e);
        }
    }
}
