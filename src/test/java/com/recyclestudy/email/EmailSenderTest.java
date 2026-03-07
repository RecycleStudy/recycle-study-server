package com.recyclestudy.email;

import com.recyclestudy.exception.EmailSendException;
import com.recyclestudy.member.domain.Email;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailSenderTest {

    @Mock
    private SesV2Client sesV2Client;

    @InjectMocks
    private EmailSender emailSender;

    @Test
    @DisplayName("메일을 성공적으로 발송한다")
    void send_success() {
        // given
        final Email targetEmail = Email.from("test@test.com");
        final String subject = "테스트 제목";
        final String content = "<html>테스트 내용</html>";

        // when
        emailSender.send(targetEmail, subject, content);

        // then
        ArgumentCaptor<SendEmailRequest> captor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesV2Client).sendEmail(captor.capture());
        SendEmailRequest request = captor.getValue();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(request.fromEmailAddress()).isEqualTo("noreply@recycle-study.site");
            softly.assertThat(request.destination().toAddresses()).containsExactly("test@test.com");
            softly.assertThat(request.content().simple().subject().data()).isEqualTo(subject);
        });
    }

    @Test
    @DisplayName("메일 발송 실패 시 EmailSendException을 던진다")
    void send_fail_throwsException() {
        // given
        final Email targetEmail = Email.from("test@test.com");
        final String subject = "테스트 제목";
        final String content = "<html>테스트 내용</html>";

        willThrow(SesV2Exception.builder().message("SES 오류").build())
                .given(sesV2Client).sendEmail(any(SendEmailRequest.class));

        // when
        // then
        assertThatThrownBy(() -> emailSender.send(targetEmail, subject, content))
                .isInstanceOf(EmailSendException.class)
                .hasMessage("메일 전송 중 오류가 발생했습니다.");
    }

    @Test
    @DisplayName("네트워크 오류 시 EmailSendException을 던진다")
    void send_fail_networkError_throwsEmailSendException() {
        // given
        final Email targetEmail = Email.from("test@test.com");
        final String subject = "테스트 제목";
        final String content = "<html>테스트 내용</html>";

        willThrow(SdkClientException.create("네트워크 오류"))
                .given(sesV2Client).sendEmail(any(SendEmailRequest.class));

        // when
        // then
        assertThatThrownBy(() -> emailSender.send(targetEmail, subject, content))
                .isInstanceOf(EmailSendException.class)
                .hasMessage("메일 전송 중 오류가 발생했습니다.");
    }
}
