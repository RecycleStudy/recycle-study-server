package com.recyclestudy.review.controller;

import com.recyclestudy.cycle.domain.selection.CustomCycleSelection;
import com.recyclestudy.cycle.domain.selection.DefaultCycleSelection;
import com.recyclestudy.exception.UnauthorizedException;
import com.recyclestudy.member.domain.ActivationExpiredDateTime;
import com.recyclestudy.member.domain.Device;
import com.recyclestudy.member.domain.DeviceIdentifier;
import com.recyclestudy.member.domain.Email;
import com.recyclestudy.member.domain.Member;
import com.recyclestudy.member.repository.DeviceRepository;
import com.recyclestudy.restdocs.APIBaseTest;
import com.recyclestudy.review.controller.request.ReviewSaveRequest;
import com.recyclestudy.review.domain.ReviewURL;
import com.recyclestudy.review.service.ReviewCycleService;
import com.recyclestudy.review.service.ReviewService;
import com.recyclestudy.review.service.output.NextReviewOutput;
import com.recyclestudy.review.service.output.ReviewSaveOutput;
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

import static com.epages.restdocs.apispec.ResourceSnippetParameters.builder;
import static com.epages.restdocs.apispec.RestAssuredRestDocumentationWrapper.document;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

class ReviewControllerTest extends APIBaseTest {

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private ReviewCycleService reviewCycleService;

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
    @DisplayName("기본 주기로 리뷰를 저장하면 201 응답을 반환한다")
    void saveReview_withDefaultCycle() {
        // given
        final String identifier = "device-id";
        final String url = "https://test.com";
        final DefaultCycleSelection cycleSelection = new DefaultCycleSelection("EBBINGHAUS");
        final ReviewSaveRequest request = new ReviewSaveRequest(url, cycleSelection);
        final ReviewSaveOutput output = ReviewSaveOutput.of(ReviewURL.from(url),
                List.of(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)));

        given(reviewService.saveReview(any())).willReturn(output);

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Review")
                                .summary("리뷰 저장")
                                .description("기본 주기로 리뷰를 저장하면 201 응답을 반환한다")
                                .requestHeaders(
                                        headerWithName("X-Device-Id").description("디바이스 식별자")
                                )
                                .requestFields(
                                        fieldWithPath("targetUrl").type(JsonFieldType.STRING)
                                                .description("리뷰할 URL"),
                                        fieldWithPath("cycle").type(JsonFieldType.OBJECT)
                                                .description("복습 주기 선택"),
                                        fieldWithPath("cycle.type").type(JsonFieldType.STRING)
                                                .description("주기 타입 (DEFAULT 또는 CUSTOM)"),
                                        fieldWithPath("cycle.code").type(JsonFieldType.STRING)
                                                .description("기본 주기 코드 (DEFAULT 타입일 때)")
                                )
                                .responseFields(
                                        fieldWithPath("url").type(JsonFieldType.STRING).description("리뷰할 URL"),
                                        fieldWithPath("scheduledAts").type(JsonFieldType.ARRAY)
                                                .description("복습 예정 일시 목록 (UTC, ISO 8601)")
                                )
                ))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("X-Device-Id", identifier)
                .body(request)
                .when()
                .post("/api/v1/reviews")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("url", equalTo(url));
    }

    @Test
    @DisplayName("커스텀 주기로 리뷰를 저장하면 201 응답을 반환한다")
    void saveReview_withCustomCycle() {
        // given
        final String identifier = "device-id";
        final String url = "https://test.com";
        final CustomCycleSelection cycleSelection = new CustomCycleSelection(1L);
        final ReviewSaveRequest request = new ReviewSaveRequest(url, cycleSelection);
        final ReviewSaveOutput output = ReviewSaveOutput.of(ReviewURL.from(url),
                List.of(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)));

        given(reviewService.saveReview(any())).willReturn(output);

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Review")
                                .summary("리뷰 저장")
                                .description("커스텀 주기로 리뷰를 저장하면 201 응답을 반환한다")
                                .requestHeaders(
                                        headerWithName("X-Device-Id").description("디바이스 식별자")
                                )
                                .requestFields(
                                        fieldWithPath("targetUrl").type(JsonFieldType.STRING)
                                                .description("리뷰할 URL"),
                                        fieldWithPath("cycle").type(JsonFieldType.OBJECT)
                                                .description("복습 주기 선택"),
                                        fieldWithPath("cycle.type").type(JsonFieldType.STRING)
                                                .description("주기 타입 (DEFAULT 또는 CUSTOM)"),
                                        fieldWithPath("cycle.id").type(JsonFieldType.NUMBER)
                                                .description("커스텀 주기 ID (CUSTOM 타입일 때)")
                                )
                                .responseFields(
                                        fieldWithPath("url").type(JsonFieldType.STRING).description("리뷰할 URL"),
                                        fieldWithPath("scheduledAts").type(JsonFieldType.ARRAY)
                                                .description("복습 예정 일시 목록 (UTC, ISO 8601)")
                                )
                ))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("X-Device-Id", identifier)
                .body(request)
                .when()
                .post("/api/v1/reviews")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("url", equalTo(url));
    }

    @Test
    @DisplayName("유효하지 않은 디바이스로 리뷰 저장 시 401 응답을 반환한다")
    void saveReview_Unauthorized() {
        // given
        final String identifier = "invalid-id";
        final DefaultCycleSelection cycleSelection = new DefaultCycleSelection("EBBINGHAUS");
        final ReviewSaveRequest request = new ReviewSaveRequest("https://test.com", cycleSelection);

        given(reviewService.saveReview(any()))
                .willThrow(new UnauthorizedException("유효하지 않은 디바이스입니다"));

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Review")
                                .summary("리뷰 저장")
                                .description("유효하지 않은 디바이스로 리뷰 저장 시 401 응답을 반환한다")
                                .requestHeaders(
                                        headerWithName("X-Device-Id").description("디바이스 식별자")
                                )
                                .requestFields(
                                        fieldWithPath("targetUrl").type(JsonFieldType.STRING)
                                                .description("리뷰할 URL"),
                                        fieldWithPath("cycle").type(JsonFieldType.OBJECT)
                                                .description("복습 주기 선택"),
                                        fieldWithPath("cycle.type").type(JsonFieldType.STRING)
                                                .description("주기 타입"),
                                        fieldWithPath("cycle.code").type(JsonFieldType.STRING)
                                                .description("기본 주기 코드")
                                )
                                .responseFields(
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                                )
                ))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("X-Device-Id", identifier)
                .body(request)
                .when()
                .post("/api/v1/reviews")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("message", equalTo("유효하지 않은 디바이스입니다"));
    }

    @Test
    @DisplayName("인증되지 않은 디바이스로 리뷰 저장 시 401 응답을 반환한다")
    void saveReview_InactiveDevice() {
        // given
        final String identifier = "inactive-id";
        final DefaultCycleSelection cycleSelection = new DefaultCycleSelection("EBBINGHAUS");
        final ReviewSaveRequest request = new ReviewSaveRequest("https://test.com", cycleSelection);

        given(reviewService.saveReview(any()))
                .willThrow(new UnauthorizedException("인증되지 않은 디바이스입니다"));

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Review")
                                .summary("리뷰 저장")
                                .description("인증되지 않은 디바이스로 리뷰 저장 시 401 응답을 반환한다")
                                .requestHeaders(
                                        headerWithName("X-Device-Id").description("디바이스 식별자")
                                )
                                .requestFields(
                                        fieldWithPath("targetUrl").type(JsonFieldType.STRING)
                                                .description("리뷰할 URL"),
                                        fieldWithPath("cycle").type(JsonFieldType.OBJECT)
                                                .description("복습 주기 선택"),
                                        fieldWithPath("cycle.type").type(JsonFieldType.STRING)
                                                .description("주기 타입"),
                                        fieldWithPath("cycle.code").type(JsonFieldType.STRING)
                                                .description("기본 주기 코드")
                                )
                                .responseFields(
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                                )
                ))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("X-Device-Id", identifier)
                .body(request)
                .when()
                .post("/api/v1/reviews")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("message", equalTo("인증되지 않은 디바이스입니다"));
    }

    @Test
    @DisplayName("다음 리뷰 조회 시 200 응답과 scheduledAt, count를 반환한다")
    void findNextReview_success() {
        // given
        final String identifier = "device-id";
        final LocalDateTime scheduledAt = LocalDateTime.of(2026, 3, 6, 9, 0);
        final NextReviewOutput output = NextReviewOutput.of(scheduledAt, 3);

        given(reviewCycleService.findNextReview(any())).willReturn(output);

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Review")
                                .summary("다음 리뷰 조회")
                                .description("다음 발송 예정 시간과 URL 개수를 반환한다")
                                .requestHeaders(
                                        headerWithName("X-Device-Id").description("디바이스 식별자")
                                )
                                .responseFields(
                                        fieldWithPath("scheduledAt").type(JsonFieldType.STRING)
                                                .description("다음 발송 예정 시간, UTC ISO 8601 형식 (PENDING 없을 시 null)"),
                                        fieldWithPath("count").type(JsonFieldType.NUMBER)
                                                .description("해당 시간에 발송될 URL 개수")
                                )
                ))
                .header("X-Device-Id", identifier)
                .when()
                .get("/api/v1/reviews/next")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("scheduledAt", equalTo("2026-03-06T09:00:00Z"))
                .body("count", equalTo(3));
    }

    @Test
    @DisplayName("PENDING이 없을 때 200 응답과 scheduledAt=null, count=0을 반환한다")
    void findNextReview_empty() {
        // given
        final String identifier = "device-id";
        final NextReviewOutput output = NextReviewOutput.empty();

        given(reviewCycleService.findNextReview(any())).willReturn(output);

        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Review")
                                .summary("다음 리뷰 조회")
                                .description("PENDING이 없을 때 scheduledAt=null, count=0을 반환한다")
                                .requestHeaders(
                                        headerWithName("X-Device-Id").description("디바이스 식별자")
                                )
                                .responseFields(
                                        fieldWithPath("scheduledAt").type(JsonFieldType.NULL)
                                                .description("다음 발송 예정 시간, UTC ISO 8601 형식 (PENDING 없을 시 null)"),
                                        fieldWithPath("count").type(JsonFieldType.NUMBER)
                                                .description("해당 시간에 발송될 URL 개수")
                                )
                ))
                .header("X-Device-Id", identifier)
                .when()
                .get("/api/v1/reviews/next")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("scheduledAt", equalTo(null))
                .body("count", equalTo(0));
    }

    @Test
    @DisplayName("헤더 없이 다음 리뷰 조회 시 401 응답을 반환한다")
    void findNextReview_noHeader() {
        // when
        // then
        given(this.spec)
                .filter(document(DEFAULT_REST_DOC_PATH,
                        builder()
                                .tag("Review")
                                .summary("다음 리뷰 조회")
                                .description("헤더 없이 다음 리뷰 조회 시 401 응답을 반환한다")
                                .responseFields(
                                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지")
                                )
                ))
                .when()
                .get("/api/v1/reviews/next")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
