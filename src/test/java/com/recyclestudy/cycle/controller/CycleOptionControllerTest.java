package com.recyclestudy.cycle.controller;

import com.recyclestudy.cycle.controller.request.CycleOptionSaveRequest;
import com.recyclestudy.cycle.controller.request.CycleOptionUpdateRequest;
import com.recyclestudy.cycle.domain.CycleOption;
import com.recyclestudy.cycle.domain.CycleOptionTitle;
import com.recyclestudy.cycle.domain.DefaultCycleOption;
import com.recyclestudy.cycle.service.CycleOptionService;
import com.recyclestudy.cycle.service.output.CycleOptionFindOutput;
import com.recyclestudy.cycle.service.output.CycleOptionSaveOutput;
import com.recyclestudy.exception.BadRequestException;
import com.recyclestudy.member.domain.ActivationExpiredDateTime;
import com.recyclestudy.member.domain.Device;
import com.recyclestudy.member.domain.DeviceIdentifier;
import com.recyclestudy.member.domain.Email;
import com.recyclestudy.member.domain.Member;
import com.recyclestudy.member.repository.DeviceRepository;
import com.recyclestudy.restdocs.APIBaseTest;
import java.time.Duration;
import java.time.LocalDateTime;
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
import org.springframework.test.util.ReflectionTestUtils;

import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static com.epages.restdocs.apispec.RestAssuredRestDocumentationWrapper.document;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;

class CycleOptionControllerTest extends APIBaseTest {

    @MockitoBean
    private CycleOptionService cycleOptionService;

    @MockitoBean
    private DeviceRepository deviceRepository;

    @BeforeEach
    void setUpMocks(RestDocumentationContextProvider provider) {
        super.setUpRestDocs(provider);
        // Default mock: device exists and is active
        final Member member = Member.withoutId(Email.from("test@test.com"));
        final Device activeDevice = Device.withoutId(member, DeviceIdentifier.from("device-id"),
                true, ActivationExpiredDateTime.create(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)));
        given(deviceRepository.findByIdentifier(any(DeviceIdentifier.class))).willReturn(Optional.of(activeDevice));
    }

    @Test
    @DisplayName("멤버의 주기 옵션을 조회한다")
    void findAllCycleOptions() {
        // given
        final String headerIdentifier = "device-id";
        final Member member = Member.withoutId(Email.from("test@test.com"));
        final CycleOption customCycleOption = CycleOption.withoutId(
                member,
                CycleOptionTitle.from("custom title"),
                List.of(Duration.ofMinutes(10), Duration.ofHours(1))
        );
        ReflectionTestUtils.setField(customCycleOption, "id", 1L);

        final CycleOptionFindOutput output = CycleOptionFindOutput.of(
                List.of(DefaultCycleOption.EBBINGHAUS),
                List.of(customCycleOption)
        );

        given(cycleOptionService.findAllCycleOptions(any())).willReturn(output);

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("CycleOption")
                                .summary("주기 옵션 조회")
                                .description("멤버의 주기 옵션을 조회한다")
                                .requestHeaders(
                                        headerWithName("X-Device-Id").description("디바이스 식별자")
                                )
                                .responseFields(
                                        fieldWithPath("defaultOptions").type(JsonFieldType.ARRAY)
                                                .description("기본 주기 옵션 목록"),
                                        fieldWithPath("defaultOptions[].code").type(JsonFieldType.STRING)
                                                .description("기본 주기 옵션 코드"),
                                        fieldWithPath("defaultOptions[].title").type(JsonFieldType.STRING)
                                                .description("기본 주기 옵션 제목"),
                                        fieldWithPath("defaultOptions[].durations").type(JsonFieldType.ARRAY)
                                                .description("주기 시간 목록 (ISO 8601 Duration)"),
                                        fieldWithPath("customOptions").type(JsonFieldType.ARRAY)
                                                .description("커스텀 주기 옵션 목록"),
                                        fieldWithPath("customOptions[].id").type(JsonFieldType.NUMBER)
                                                .description("커스텀 주기 옵션 ID"),
                                        fieldWithPath("customOptions[].title").type(JsonFieldType.STRING)
                                                .description("커스텀 주기 옵션 제목"),
                                        fieldWithPath("customOptions[].durations").type(JsonFieldType.ARRAY)
                                                .description("주기 시간 목록 (ISO 8601 Duration)")
                                )
                ))
                .header("X-Device-Id", headerIdentifier)
                .when()
                .get("/api/v1/cycles/custom")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("defaultOptions", hasSize(1))
                .body("customOptions", hasSize(1));
    }

    @Test
    @DisplayName("유효하지 않은 디바이스로 조회 시 401 응답을 반환한다")
    void findAllCycleOptions_Unauthorized() {
        // given
        final String headerIdentifier = "device-id";

        given(deviceRepository.findByIdentifier(any()))
                .willReturn(Optional.empty());

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("CycleOption")
                                .summary("주기 옵션 조회")
                                .description("유효하지 않은 디바이스로 조회 시 401 응답을 반환한다")
                                .requestHeaders(
                                        headerWithName("X-Device-Id").description("디바이스 식별자")
                                )
                ))
                .header("X-Device-Id", headerIdentifier)
                .when()
                .get("/api/v1/cycles/custom")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    @DisplayName("커스텀 주기 옵션을 저장한다")
    void saveCycleOption() {
        // given
        final String headerIdentifier = "device-id";
        final CycleOptionSaveRequest request = new CycleOptionSaveRequest(
                "custom title",
                List.of("PT10M", "P1D")
        );

        final Member member = Member.withoutId(Email.from("test@test.com"));
        final CycleOption customCycleOption = CycleOption.withoutId(
                member,
                CycleOptionTitle.from("custom title"),
                List.of(Duration.ofMinutes(10), Duration.ofDays(1))
        );
        ReflectionTestUtils.setField(customCycleOption, "id", 1L);

        final CycleOptionSaveOutput output = CycleOptionSaveOutput.from(customCycleOption);

        given(cycleOptionService.saveCycleOption(any(), any())).willReturn(output);

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("CycleOption")
                                .summary("커스텀 주기 옵션 저장")
                                .description("새로운 커스텀 주기 옵션을 저장한다")
                                .requestHeaders(
                                        headerWithName("X-Device-Id").description("디바이스 식별자")
                                )
                                .requestFields(
                                        fieldWithPath("title").type(JsonFieldType.STRING).description("주기 옵션 제목"),
                                        fieldWithPath("durations").type(JsonFieldType.ARRAY)
                                                .description("주기 시간 목록 (ISO 8601 Duration)")
                                )
                                .responseFields(
                                        fieldWithPath("id").type(JsonFieldType.NUMBER).description("생성된 주기 옵션 ID"),
                                        fieldWithPath("title").type(JsonFieldType.STRING).description("주기 옵션 제목"),
                                        fieldWithPath("durations").type(JsonFieldType.ARRAY).description("주기 시간 목록")
                                )
                ))
                .header("X-Device-Id", headerIdentifier)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .when()
                .post("/api/v1/cycles/custom")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .header("Location", "/api/v1/cycles/custom/1")
                .body("title", equalTo("custom title"))
                .body("durations", hasSize(2));
    }

    @Test
    @DisplayName("잘못된 요청 데이터로 저장 시 400 응답을 반환한다")
    void saveCycleOption_BadRequest() {
        // given
        final String headerIdentifier = "device-id";
        final CycleOptionSaveRequest request = new CycleOptionSaveRequest(
                "title",
                List.of("PT5M")
        );

        given(cycleOptionService.saveCycleOption(any(), any()))
                .willThrow(new BadRequestException("주기는 10분 단위여야 합니다."));

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("CycleOption")
                                .summary("커스텀 주기 옵션 저장")
                                .description("잘못된 데이터로 저장 시 400 응답을 반환한다")
                                .requestHeaders(
                                        headerWithName("X-Device-Id").description("디바이스 식별자")
                                )
                                .responseFields(
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                                )
                ))
                .header("X-Device-Id", headerIdentifier)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .when()
                .post("/api/v1/cycles/custom")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("message", equalTo("주기는 10분 단위여야 합니다."));
    }

    @Test
    @DisplayName("커스텀 주기 옵션을 수정한다")
    void updateCycleOption() {
        // given
        final String headerIdentifier = "device-id";
        final Long cycleOptionId = 1L;
        final CycleOptionUpdateRequest request = new CycleOptionUpdateRequest(
                "updated title",
                List.of("PT20M")
        );

        final Member member = Member.withoutId(Email.from("test@test.com"));
        final CycleOption customCycleOption = CycleOption.withoutId(
                member,
                CycleOptionTitle.from("updated title"),
                List.of(Duration.ofMinutes(20))
        );
        ReflectionTestUtils.setField(customCycleOption, "id", cycleOptionId);

        final CycleOptionSaveOutput output = CycleOptionSaveOutput.from(customCycleOption);

        given(cycleOptionService.updateCycleOption(any(), any(), any())).willReturn(output);

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("CycleOption")
                                .summary("커스텀 주기 옵션 수정")
                                .description("기존 커스텀 주기 옵션을 수정한다")
                                .requestHeaders(
                                        headerWithName("X-Device-Id").description("디바이스 식별자")
                                )
                                .pathParameters(
                                        parameterWithName("id").description("수정할 주기 옵션 ID")
                                )
                                .requestFields(
                                        fieldWithPath("title").type(JsonFieldType.STRING).description("수정할 주기 옵션 제목"),
                                        fieldWithPath("durations").type(JsonFieldType.ARRAY)
                                                .description("수정할 주기 시간 목록")
                                )
                                .responseFields(
                                        fieldWithPath("id").type(JsonFieldType.NUMBER).description("주기 옵션 ID"),
                                        fieldWithPath("title").type(JsonFieldType.STRING).description("주기 옵션 제목"),
                                        fieldWithPath("durations").type(JsonFieldType.ARRAY).description("주기 시간 목록")
                                )
                ))
                .header("X-Device-Id", headerIdentifier)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .when()
                .put("/api/v1/cycles/custom/{id}", cycleOptionId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("title", equalTo("updated title"))
                .body("durations", hasSize(1));
    }

    @Test
    @DisplayName("커스텀 주기 옵션을 삭제한다")
    void deleteCycleOption() {
        // given
        final String headerIdentifier = "device-id";
        final Long cycleOptionId = 1L;

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("CycleOption")
                                .summary("커스텀 주기 옵션 삭제")
                                .description("커스텀 주기 옵션을 삭제한다")
                                .requestHeaders(
                                        headerWithName("X-Device-Id").description("디바이스 식별자")
                                )
                                .pathParameters(
                                        parameterWithName("id").description("삭제할 주기 옵션 ID")
                                )
                ))
                .header("X-Device-Id", headerIdentifier)
                .when()
                .delete("/api/v1/cycles/custom/{id}", cycleOptionId)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }
}
