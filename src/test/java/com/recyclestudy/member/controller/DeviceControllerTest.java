package com.recyclestudy.member.controller;

import com.recyclestudy.exception.BadRequestException;
import com.recyclestudy.exception.DeviceActivationExpiredException;
import com.recyclestudy.exception.NotFoundException;
import com.recyclestudy.member.controller.request.DeviceDeleteRequest;
import com.recyclestudy.member.domain.ActivationExpiredDateTime;
import com.recyclestudy.member.domain.Device;
import com.recyclestudy.member.domain.DeviceIdentifier;
import com.recyclestudy.member.domain.Email;
import com.recyclestudy.member.domain.Member;
import com.recyclestudy.member.repository.DeviceRepository;
import com.recyclestudy.member.service.MemberService;
import com.recyclestudy.restdocs.APIBaseTest;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static com.epages.restdocs.apispec.RestAssuredRestDocumentationWrapper.document;
import static com.epages.restdocs.apispec.Schema.schema;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;

class DeviceControllerTest extends APIBaseTest {

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private DeviceRepository deviceRepository;

    @BeforeEach
    void setUpMocks(RestDocumentationContextProvider provider) {
        super.setUpRestDocs(provider);
        // Default mock: device exists and is active
        final Member member = Member.withoutId(Email.from("test@test.com"));
        final Device activeDevice = Device.withoutId(member, DeviceIdentifier.from("device-id"),
                true, ActivationExpiredDateTime.create(LocalDateTime.now()));
        given(deviceRepository.findByIdentifier(any(DeviceIdentifier.class))).willReturn(Optional.of(activeDevice));
    }

    @Test
    @DisplayName("디바이스 인증 성공 시 200 응답을 반환한다")
    void authenticateDevice_Success() {
        // given
        final String email = "test@test.com";
        final String identifier = "device-identifier";

        doNothing().when(memberService).authenticateDevice(any(Email.class), any(DeviceIdentifier.class));

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        resource(
                                builder()
                                        .tag("Device")
                                        .summary("디바이스 인증")
                                        .description("디바이스 인증 성공 시 인증 완료 안내 HTML 페이지를 반환합니다.")
                                        .queryParameters(
                                                parameterWithName("email").description("이메일"),
                                                parameterWithName("identifier").description("디바이스 식별자")
                                        )
                                        .responseHeaders(
                                                headerWithName(HttpHeaders.CONTENT_TYPE)
                                                        .description("text/html;charset=UTF-8")
                                        )
                                        .responseSchema(schema("string"))
                                        .build()
                        ),
                        queryParameters(
                                parameterWithName("email").description("이메일"),
                                parameterWithName("identifier").description("디바이스 식별자")
                        )
                ))
                .param("email", email)
                .param("identifier", identifier)
                .accept(MediaType.TEXT_HTML_VALUE)
                .when()
                .get("/api/v1/device/auth")
                .then()
                .statusCode(HttpStatus.OK.value())
                .contentType(MediaType.TEXT_HTML_VALUE);
    }

    @Test
    @DisplayName("이미 인증된 디바이스 인증 시도 시 400 응답을 반환한다")
    void authenticateDevice_AlreadyAuthenticated() {
        // given
        final String email = "test@test.com";
        final String identifier = "device-identifier";

        doThrow(new BadRequestException("이미 인증되었습니다"))
                .when(memberService).authenticateDevice(any(Email.class), any(DeviceIdentifier.class));

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Device")
                                .summary("디바이스 인증")
                                .description("이미 인증된 디바이스 인증 시도 시 400 응답을 반환한다")
                                .queryParameters(
                                        parameterWithName("email").description("이메일"),
                                        parameterWithName("identifier").description("디바이스 식별자")
                                )
                                .responseFields(
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                                )
                ))
                .param("email", email)
                .param("identifier", identifier)
                .when()
                .get("/api/v1/device/auth")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", equalTo("이미 인증되었습니다"));
    }

    @Test
    @DisplayName("인증 시 유효하지 않은 이메일 형식인 경우 400 응답을 반환한다")
    void authenticateDevice_InvalidEmailFormat() {
        // given
        final String invalidEmail = "invalid-email";
        final String identifier = "device-identifier";

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Device")
                                .summary("디바이스 인증")
                                .description("유효하지 않은 이메일 형식인 경우 400 응답을 반환한다")
                                .queryParameters(
                                        parameterWithName("email").description("이메일"),
                                        parameterWithName("identifier").description("디바이스 식별자")
                                )
                                .responseFields(
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                                )
                ))
                .param("email", invalidEmail)
                .param("identifier", identifier)
                .when()
                .get("/api/v1/device/auth")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", equalTo("유효하지 않은 이메일 형식입니다."));
    }

    @Test
    @DisplayName("인증 유효 시간이 만료된 경우 400 응답을 반환한다")
    void authenticateDevice_Expired() {
        // given
        final String email = "test@test.com";
        final String identifier = "device-identifier";

        doThrow(new DeviceActivationExpiredException("인증 유효 시간이 만료되었습니다."))
                .when(memberService).authenticateDevice(any(Email.class), any(DeviceIdentifier.class));

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Device")
                                .summary("디바이스 인증")
                                .description("인증 유효 시간이 만료된 경우 400 응답을 반환한다")
                                .queryParameters(
                                        parameterWithName("email").description("이메일"),
                                        parameterWithName("identifier").description("디바이스 식별자")
                                )
                                .responseFields(
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                                )
                ))
                .param("email", email)
                .param("identifier", identifier)
                .when()
                .get("/api/v1/device/auth")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", equalTo("인증 유효 시간이 만료되었습니다."));
    }

    @Test
    @DisplayName("디바이스 삭제 시 204 응답을 반환한다")
    void deleteDevice() {
        // given
        final String headerIdentifier = "device-id";
        final DeviceDeleteRequest request = new DeviceDeleteRequest("test@test.com", "target-id");

        doNothing().when(memberService).deleteDevice(any());

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Device")
                                .summary("디바이스 삭제")
                                .description("디바이스 삭제 시 204 응답을 반환한다")
                                .requestHeaders(
                                        headerWithName("X-Device-Id").description("디바이스 식별자")
                                )
                                .requestFields(
                                        fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                                        fieldWithPath("targetDeviceIdentifier").type(JsonFieldType.STRING)
                                                .description("삭제할 디바이스 식별자")
                                )
                ))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("X-Device-Id", headerIdentifier)
                .body(request)
                .when()
                .delete("/api/v1/device")
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @Test
    @DisplayName("존재하지 않는 멤버의 이메일로 인증 시도 시 404 응답을 반환한다")
    void authenticateDevice_NotFoundMember() {
        // given
        final String email = "notfound@test.com";
        final String identifier = "device-identifier";

        doThrow(new NotFoundException("존재하지 않는 멤버입니다"))
                .when(memberService).authenticateDevice(any(Email.class), any(DeviceIdentifier.class));

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Device")
                                .summary("디바이스 인증")
                                .description("존재하지 않는 멤버의 이메일로 인증 시도 시 404 응답을 반환한다")
                                .queryParameters(
                                        parameterWithName("email").description("이메일"),
                                        parameterWithName("identifier").description("디바이스 식별자")
                                )
                                .responseFields(
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                                )
                ))
                .param("email", email)
                .param("identifier", identifier)
                .when()
                .get("/api/v1/device/auth")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("message", equalTo("존재하지 않는 멤버입니다"));
    }

    @Test
    @DisplayName("존재하지 않는 디바이스 식별자로 인증 시도 시 404 응답을 반환한다")
    void authenticateDevice_NotFoundDevice() {
        // given
        final String email = "test@test.com";
        final String identifier = "not-found-id";

        doThrow(new NotFoundException("존재하지 않는 디바이스 아이디입니다"))
                .when(memberService).authenticateDevice(any(Email.class), any(DeviceIdentifier.class));

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Device")
                                .summary("디바이스 인증")
                                .description("존재하지 않는 디바이스 식별자로 인증 시도 시 404 응답을 반환한다")
                                .queryParameters(
                                        parameterWithName("email").description("이메일"),
                                        parameterWithName("identifier").description("디바이스 식별자")
                                )
                                .responseFields(
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                                )
                ))
                .param("email", email)
                .param("identifier", identifier)
                .when()
                .get("/api/v1/device/auth")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("message", equalTo("존재하지 않는 디바이스 아이디입니다"));
    }

    @Test
    @DisplayName("디바이스 소유자가 아닌 이메일로 인증 시도 시 400 응답을 반환한다")
    void authenticateDevice_NotOwner() {
        // given
        final String email = "other@test.com";
        final String identifier = "device-identifier";

        doThrow(new BadRequestException("디바이스 소유자가 아닙니다."))
                .when(memberService).authenticateDevice(any(Email.class), any(DeviceIdentifier.class));

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Device")
                                .summary("디바이스 인증")
                                .description("디바이스 소유자가 아닌 이메일로 인증 시도 시 400 응답을 반환한다")
                                .queryParameters(
                                        parameterWithName("email").description("이메일"),
                                        parameterWithName("identifier").description("디바이스 식별자")
                                )
                                .responseFields(
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                                )
                ))
                .param("email", email)
                .param("identifier", identifier)
                .when()
                .get("/api/v1/device/auth")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", equalTo("디바이스 소유자가 아닙니다."));
    }

    @Test
    @DisplayName("인증 시 이메일 파라미터가 누락된 경우 400 응답을 반환한다")
    void authenticateDevice_NullEmail() {
        // given
        final String identifier = "device-identifier";

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Device")
                                .summary("디바이스 인증")
                                .description("이메일 파라미터가 누락된 경우 400 응답을 반환한다")
                                .queryParameters(
                                        parameterWithName("identifier").description("디바이스 식별자")
                                )
                                .responseFields(
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                                )
                ))
                .param("identifier", identifier)
                .when()
                .get("/api/v1/device/auth")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("인증 시 디바이스 식별자 파라미터가 누락된 경우 400 응답을 반환한다")
    void authenticateDevice_NullIdentifier() {
        // given
        final String email = "test@test.com";

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Device")
                                .summary("디바이스 인증")
                                .description("디바이스 식별자 파라미터가 누락된 경우 400 응답을 반환한다")
                                .queryParameters(
                                        parameterWithName("email").description("이메일"),
                                        parameterWithName("identifier").description("디바이스 식별자")
                                )
                                .responseFields(
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                                )
                ))
                .param("email", email)
                .when()
                .get("/api/v1/device/auth")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("삭제 요청 시 이메일이 누락된 경우 400 응답을 반환한다")
    void deleteDevice_NullEmail() {
        // given
        final String headerIdentifier = "device-id";
        final DeviceDeleteRequest request = new DeviceDeleteRequest(null, "target-id");

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Device")
                                .summary("디바이스 삭제")
                                .description("삭제 요청 시 이메일이 누락된 경우 400 응답을 반환한다")
                                .requestHeaders(
                                        headerWithName("X-Device-Id").description("디바이스 식별자")
                                )
                                .requestFields(
                                        fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                                        fieldWithPath("targetDeviceIdentifier").type(JsonFieldType.STRING)
                                                .description("삭제할 디바이스 식별자")
                                )
                                .responseFields(
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                                )
                ))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("X-Device-Id", headerIdentifier)
                .body(request)
                .when()
                .delete("/api/v1/device")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", equalTo("null이 될 수 없습니다: value"));
    }

    @Test
    @DisplayName("삭제 요청 시 삭제할 디바이스 식별자가 누락된 경우 400 응답을 반환한다")
    void deleteDevice_NullTargetIdentifier() {
        // given
        final String headerIdentifier = "device-id";
        final DeviceDeleteRequest request = new DeviceDeleteRequest("test@test.com", null);

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Device")
                                .summary("디바이스 삭제")
                                .description("삭제 요청 시 삭제할 디바이스 식별자가 누락된 경우 400 응답을 반환한다")
                                .requestHeaders(
                                        headerWithName("X-Device-Id").description("디바이스 식별자")
                                )
                                .requestFields(
                                        fieldWithPath("email").type(JsonFieldType.STRING).description("이메일"),
                                        fieldWithPath("targetDeviceIdentifier").type(JsonFieldType.STRING)
                                                .description("삭제할 디바이스 식별자")
                                )
                                .responseFields(
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                                )
                ))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("X-Device-Id", headerIdentifier)
                .body(request)
                .when()
                .delete("/api/v1/device")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", equalTo("null이 될 수 없습니다: value"));
    }
}
