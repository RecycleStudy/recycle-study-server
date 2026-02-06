package com.recyclestudy.member.controller;

import com.recyclestudy.email.DeviceAuthEmailSender;
import com.recyclestudy.exception.NotFoundException;
import com.recyclestudy.exception.UnauthorizedException;
import com.recyclestudy.member.controller.request.MemberNotificationTimeUpdateRequest;
import com.recyclestudy.member.controller.request.MemberSaveRequest;
import com.recyclestudy.member.domain.ActivationExpiredDateTime;
import com.recyclestudy.member.domain.Device;
import com.recyclestudy.member.domain.DeviceIdentifier;
import com.recyclestudy.member.domain.Email;
import com.recyclestudy.member.domain.Member;
import com.recyclestudy.member.repository.DeviceRepository;
import com.recyclestudy.member.service.MemberService;
import com.recyclestudy.member.service.input.MemberNotificationTimeUpdateInput;
import com.recyclestudy.member.service.output.MemberFindOutput;
import com.recyclestudy.member.service.output.MemberSaveOutput;
import com.recyclestudy.restdocs.APIBaseTest;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static com.epages.restdocs.apispec.RestAssuredRestDocumentationWrapper.document;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;

class MemberControllerTest extends APIBaseTest {

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private DeviceAuthEmailSender deviceAuthEmailSender;

    @MockitoBean
    private DeviceRepository deviceRepository;

    @BeforeEach
    void setUpMocks(RestDocumentationContextProvider provider) {
        super.setUpRestDocs(provider);
        // Default mock: device exists and is active
        final Member member = Member.withoutId(Email.from("test@test.com"));
        final Device activeDevice = Device.withoutId(member, DeviceIdentifier.from("device-id-1"),
                true, ActivationExpiredDateTime.create(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)));
        given(deviceRepository.findByIdentifier(any(DeviceIdentifier.class))).willReturn(Optional.of(activeDevice));
    }

    @Test
    @DisplayName("이메일을 통해 새로운 디바이스를 저장한다")
    void saveMember() {
        // given
        final String email = "test@test.com";
        final String identifier = "device-identifier";
        final MemberSaveRequest request = new MemberSaveRequest(email);
        final MemberSaveOutput output = new MemberSaveOutput(Email.from(email), DeviceIdentifier.from(identifier));

        given(memberService.saveDevice(any())).willReturn(output);

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Member")
                                .summary("멤버 저장")
                                .description("이메일을 통해 새로운 디바이스를 저장한다")
                                .requestFields(
                                        fieldWithPath("email").type(JsonFieldType.STRING).description("이메일")
                                )
                                .responseFields(
                                        fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                                        fieldWithPath("identifier").type(JsonFieldType.STRING).description("디바이스 식별자")
                                )
                ))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .when()
                .post("/api/v1/members")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("email", equalTo(email))
                .body("identifier", equalTo(identifier));

        verify(deviceAuthEmailSender).sendDeviceAuthMail(Email.from(email), DeviceIdentifier.from(identifier));
    }

    @Test
    @DisplayName("멤버의 모든 디바이스 정보를 조회한다")
    void findAllMemberDevices() {
        // given
        final String email = "test@test.com";
        final String headerIdentifier = "device-id-1";

        final MemberFindOutput.MemberFindElement device1 = new MemberFindOutput.MemberFindElement(
                DeviceIdentifier.from(headerIdentifier),
                LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusDays(1)
        );
        final MemberFindOutput.MemberFindElement device2 = new MemberFindOutput.MemberFindElement(
                DeviceIdentifier.from("device-id-2"),
                LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        );

        final MemberFindOutput output = new MemberFindOutput(
                Email.from(email),
                LocalTime.of(9, 0),
                List.of(device1, device2)
        );

        given(memberService.findAllMemberDevices(any())).willReturn(output);

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Member")
                                .summary("멤버 디바이스 조회")
                                .description("멤버의 모든 디바이스 정보를 조회한다")
                                .requestHeaders(
                                        headerWithName("X-Device-Id").description("디바이스 식별자")
                                )
                                .queryParameters(
                                        parameterWithName("email").description("이메일")
                                )
                                .responseFields(
                                        fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                                        fieldWithPath("notificationTime").type(JsonFieldType.STRING)
                                                .description("알림 시간").optional(),
                                        fieldWithPath("devices").type(JsonFieldType.ARRAY).description("디바이스 목록"),
                                        fieldWithPath("devices[].identifier").type(JsonFieldType.STRING)
                                                .description("디바이스 식별자 값"),
                                        fieldWithPath("devices[].createdAt").type(JsonFieldType.STRING)
                                                .description("디바이스 생성일")
                                ),
                        queryParameters(
                                parameterWithName("email").description("이메일")
                        )
                ))
                .header("X-Device-Id", headerIdentifier)
                .param("email", email)
                .when()
                .get("/api/v1/members")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("devices", hasSize(2));
    }

    @Test
    @DisplayName("존재하지 않는 멤버 조회 시 404 응답을 반환한다")
    void findAllMemberDevices_NotFoundMember() {
        // given
        final String email = "notfound@test.com";
        final String headerIdentifier = "device-identifier";

        given(memberService.findAllMemberDevices(any()))
                .willThrow(new NotFoundException("존재하지 않는 멤버입니다"));

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Member")
                                .summary("멤버 디바이스 조회")
                                .description("존재하지 않는 멤버 조회 시 404 응답을 반환한다")
                                .requestHeaders(
                                        headerWithName("X-Device-Id").description("디바이스 식별자")
                                )
                                .queryParameters(
                                        parameterWithName("email").description("이메일")
                                )
                                .responseFields(
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                                ),
                        queryParameters(
                                parameterWithName("email").description("이메일")
                        )
                ))
                .header("X-Device-Id", headerIdentifier)
                .param("email", email)
                .when()
                .get("/api/v1/members")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("message", equalTo("존재하지 않는 멤버입니다"));
    }

    @Test
    @DisplayName("인증되지 않은 디바이스로 조회 시 401 응답을 반환한다")
    void findAllMemberDevices_UnauthorizedDevice() {
        // given
        final String email = "test@test.com";
        final String headerIdentifier = "unauthorized-id";

        given(memberService.findAllMemberDevices(any()))
                .willThrow(new UnauthorizedException("인증되지 않은 디바이스입니다"));

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Member")
                                .summary("멤버 디바이스 조회")
                                .description("인증되지 않은 디바이스로 조회 시 401 응답을 반환한다")
                                .requestHeaders(
                                        headerWithName("X-Device-Id").description("디바이스 식별자")
                                )
                                .queryParameters(
                                        parameterWithName("email").description("이메일")
                                )
                                .responseFields(
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                                ),
                        queryParameters(
                                parameterWithName("email").description("이메일")
                        )
                ))
                .header("X-Device-Id", headerIdentifier)
                .param("email", email)
                .when()
                .get("/api/v1/members")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("message", equalTo("인증되지 않은 디바이스입니다"));
    }

    @Test
    @DisplayName("조회 시 유효하지 않은 이메일 형식인 경우 400 응답을 반환한다")
    void findAllMemberDevices_InvalidEmailFormat() {
        // given
        final String invalidEmail = "invalid-email";
        final String headerIdentifier = "device-identifier";

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Member")
                                .summary("멤버 디바이스 조회")
                                .description("조회 시 유효하지 않은 이메일 형식인 경우 400 응답을 반환한다")
                                .requestHeaders(
                                        headerWithName("X-Device-Id").description("디바이스 식별자")
                                )
                                .queryParameters(
                                        parameterWithName("email").description("이메일")
                                )
                                .responseFields(
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                                ),
                        queryParameters(
                                parameterWithName("email").description("이메일")
                        )
                ))
                .header("X-Device-Id", headerIdentifier)
                .param("email", invalidEmail)
                .when()
                .get("/api/v1/members")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", equalTo("유효하지 않은 이메일 형식입니다."));
    }

    @Test
    @DisplayName("이메일이 누락된 경우 400 응답을 반환한다")
    void saveMember_NullEmail() {
        // given
        final MemberSaveRequest request = new MemberSaveRequest(null);

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Member")
                                .summary("멤버 저장")
                                .description("이메일이 누락된 경우 400 응답을 반환한다")
                                .requestFields(
                                        fieldWithPath("email").type(JsonFieldType.STRING).description("이메일").optional()
                                )
                                .responseFields(
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                                )
                ))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .when()
                .post("/api/v1/members")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", equalTo("null이 될 수 없습니다: value"));
    }

    @Test
    @DisplayName("유효하지 않은 이메일 형식으로 요청 시 400 응답을 반환한다")
    void saveMember_InvalidEmailFormat() {
        // given
        final String invalidEmail = "invalid-email";
        final MemberSaveRequest request = new MemberSaveRequest(invalidEmail);

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Member")
                                .summary("멤버 저장")
                                .description("유효하지 않은 이메일 형식으로 요청 시 400 응답을 반환한다")
                                .requestFields(
                                        fieldWithPath("email").description("이메일")
                                )
                                .responseFields(
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                                )
                ))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .when()
                .post("/api/v1/members")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", equalTo("유효하지 않은 이메일 형식입니다."));
    }

    @Test
    @DisplayName("존재하지 않는 디바이스 식별자로 조회 시 404 응답을 반환한다")
    void findAllMemberDevices_NotFoundDevice() {
        // given
        final String email = "test@test.com";
        final String headerIdentifier = "not-found-id";

        given(memberService.findAllMemberDevices(any()))
                .willThrow(new NotFoundException("존재하지 않는 디바이스 아이디입니다"));

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Member")
                                .summary("멤버 디바이스 조회")
                                .description("존재하지 않는 디바이스 식별자로 조회 시 404 응답을 반환한다")
                                .requestHeaders(
                                        headerWithName("X-Device-Id").description("디바이스 식별자")
                                )
                                .queryParameters(
                                        parameterWithName("email").description("이메일")
                                )
                                .responseFields(
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                                ),
                        queryParameters(
                                parameterWithName("email").description("이메일")
                        )
                ))
                .header("X-Device-Id", headerIdentifier)
                .param("email", email)
                .when()
                .get("/api/v1/members")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("message", equalTo("존재하지 않는 디바이스 아이디입니다"));
    }

    @Test
    @DisplayName("이메일 파라미터가 누락된 경우 400 응답을 반환한다")
    void findAllMemberDevices_NullEmail() {
        // given
        final String headerIdentifier = "device-identifier";

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Member")
                                .summary("멤버 디바이스 조회")
                                .description("이메일 파라미터가 누락된 경우 400 응답을 반환한다")
                                .requestHeaders(
                                        headerWithName("X-Device-Id").description("디바이스 식별자")
                                )
                                .responseFields(
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                                )
                ))
                .header("X-Device-Id", headerIdentifier)
                .when()
                .get("/api/v1/members")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("헤더로 디바이스 인증하여 멤버의 모든 디바이스 정보를 조회한다")
    void findAllMemberDevices_WithHeader() {
        // given
        final String email = "test@test.com";
        final String headerIdentifier = "device-id-1";

        final MemberFindOutput.MemberFindElement device1 = new MemberFindOutput.MemberFindElement(
                DeviceIdentifier.from(headerIdentifier),
                LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).minusDays(1)
        );
        final MemberFindOutput.MemberFindElement device2 = new MemberFindOutput.MemberFindElement(
                DeviceIdentifier.from("device-id-2"),
                LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        );

        final MemberFindOutput output = new MemberFindOutput(
                Email.from(email),
                null,
                List.of(device1, device2)
        );

        given(memberService.findAllMemberDevices(any())).willReturn(output);

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Member")
                                .summary("멤버 디바이스 조회")
                                .description("헤더로 디바이스 인증하여 멤버의 모든 디바이스 정보를 조회한다")
                                .requestHeaders(
                                        headerWithName("X-Device-Id").description("디바이스 식별자")
                                )
                                .queryParameters(
                                        parameterWithName("email").description("이메일")
                                )
                                .responseFields(
                                        fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                                        fieldWithPath("notificationTime").type(JsonFieldType.STRING)
                                                .description("알림 시간").optional(),
                                        fieldWithPath("devices").type(JsonFieldType.ARRAY).description("디바이스 목록"),
                                        fieldWithPath("devices[].identifier").type(JsonFieldType.STRING)
                                                .description("디바이스 식별자 값"),
                                        fieldWithPath("devices[].createdAt").type(JsonFieldType.STRING)
                                                .description("디바이스 생성일")
                                ),
                        queryParameters(
                                parameterWithName("email").description("이메일")
                        )
                ))
                .header("X-Device-Id", headerIdentifier)
                .param("email", email)
                .when()
                .get("/api/v1/members")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("devices", hasSize(2));
    }

    @Test
    @DisplayName("멤버의 알림 시간을 업데이트한다")
    void updateNotificationTime() {
        // given
        final String headerIdentifier = "device-id-1";
        final LocalTime notificationTime = LocalTime.of(9, 0);
        final MemberNotificationTimeUpdateRequest request = new MemberNotificationTimeUpdateRequest(notificationTime);

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Member")
                                .summary("멤버 알림 시간 업데이트")
                                .description("멤버의 알림 시간을 업데이트한다")
                                .requestHeaders(
                                        headerWithName("X-Device-Id").description("디바이스 식별자")
                                )
                                .requestFields(
                                        fieldWithPath("notificationTime").type(JsonFieldType.STRING).description("알림 시간 (HH:mm:ss)")
                                )
                ))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("X-Device-Id", headerIdentifier)
                .body(request)
                .when()
                .patch("/api/v1/members/notification-time")
                .then()
                .statusCode(HttpStatus.OK.value());

        verify(memberService).updateNotificationTime(any(MemberNotificationTimeUpdateInput.class));
    }
}
