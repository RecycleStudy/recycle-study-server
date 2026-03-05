package com.recyclestudy.email;

import com.recyclestudy.member.domain.DeviceIdentifier;
import com.recyclestudy.member.domain.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceAuthEmailSender {

    private final EmailSender emailSender;
    private final TemplateEngine templateEngine;

    @Value("${auth.base-url}")
    private String baseUrl;

    @Async
    public void sendDeviceAuthMail(final Email email, final DeviceIdentifier deviceIdentifier) {
        final String authUrl = createAuthUrl(email, deviceIdentifier);
        final String message = createMessage(authUrl);

        emailSender.send(email, "[Recycle Study] 디바이스 인증을 완료해주세요.", message);

        log.info("[AUTH_MAIL_SENT] 인증 메일 발송 성공: {}", email);
    }

    private String createAuthUrl(final Email email, final DeviceIdentifier deviceIdentifier) {
        return String.format("%s/api/v1/device/auth?email=%s&identifier=%s",
                baseUrl, email.getValue(), deviceIdentifier.getValue());
    }

    private String createMessage(final String authUrl) {
        final Context context = new Context();
        context.setVariable("authUrl", authUrl);
        return templateEngine.process("auth_email", context);
    }
}
