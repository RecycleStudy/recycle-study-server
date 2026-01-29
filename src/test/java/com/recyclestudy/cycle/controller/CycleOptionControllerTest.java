package com.recyclestudy.cycle.controller;

import com.recyclestudy.cycle.domain.CycleOptionTitle;
import com.recyclestudy.cycle.service.CycleOptionService;
import com.recyclestudy.cycle.service.output.CycleOptionFindOutput;
import com.recyclestudy.exception.UnauthorizedException;
import com.recyclestudy.member.domain.ActivationExpiredDateTime;
import com.recyclestudy.member.domain.Device;
import com.recyclestudy.member.domain.DeviceIdentifier;
import com.recyclestudy.member.domain.Email;
import com.recyclestudy.member.domain.Member;
import com.recyclestudy.member.repository.DeviceRepository;
import com.recyclestudy.restdocs.APIBaseTest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static com.epages.restdocs.apispec.RestAssuredRestDocumentationWrapper.document;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

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
                true, ActivationExpiredDateTime.create(LocalDateTime.now()));
        given(deviceRepository.findByIdentifier(any(DeviceIdentifier.class))).willReturn(Optional.of(activeDevice));
    }

    @Test
    @DisplayName("멤버의 주기 옵션을 조회한다")
    void findAllCycleOptions() {
        // given
        final String headerIdentifier = "device-id";
        final CycleOptionFindOutput.CycleOptionElement option = new CycleOptionFindOutput.CycleOptionElement(
                1L,
                CycleOptionTitle.from("title"),
                List.of(Duration.ofMinutes(10), Duration.ofHours(1))
        );
        final CycleOptionFindOutput output = new CycleOptionFindOutput(List.of(option));

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
                                        fieldWithPath("options").type(JsonFieldType.ARRAY).description("주기 옵션 목록"),
                                        fieldWithPath("options[].id").type(JsonFieldType.NUMBER)
                                                .description("주기 옵션 ID"),
                                        fieldWithPath("options[].title").type(JsonFieldType.STRING)
                                                .description("주기 옵션 제목"),
                                        fieldWithPath("options[].durations").type(JsonFieldType.ARRAY)
                                                .description("주기 시간 목록 (ISO 8601 Duration)")
                                )
                ))
                .header("X-Device-Id", headerIdentifier)
                .when()
                .get("/api/v1/cycles/custom")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("options", hasSize(1));
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
}
