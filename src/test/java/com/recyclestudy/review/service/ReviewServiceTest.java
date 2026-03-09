package com.recyclestudy.review.service;

import com.recyclestudy.cycle.domain.CycleOption;
import com.recyclestudy.cycle.domain.selection.CustomCycleSelection;
import com.recyclestudy.cycle.domain.selection.DefaultCycleSelection;
import com.recyclestudy.cycle.repository.CycleOptionRepository;
import com.recyclestudy.cycle.service.resolver.CycleSelectionResolverRegistry;
import com.recyclestudy.exception.NotFoundException;
import com.recyclestudy.exception.UnauthorizedException;
import com.recyclestudy.member.domain.DeviceIdentifier;
import com.recyclestudy.member.domain.Email;
import com.recyclestudy.member.domain.Member;
import com.recyclestudy.member.repository.MemberRepository;
import com.recyclestudy.review.domain.NotificationHistory;
import com.recyclestudy.review.domain.NotificationStatus;
import com.recyclestudy.review.domain.Review;
import com.recyclestudy.review.domain.ReviewCycle;
import com.recyclestudy.review.domain.ReviewURL;
import com.recyclestudy.review.repository.NotificationHistoryRepository;
import com.recyclestudy.review.repository.ReviewCycleRepository;
import com.recyclestudy.review.repository.ReviewRepository;
import com.recyclestudy.review.service.input.ReviewSaveInput;
import com.recyclestudy.review.service.output.ReviewSaveOutput;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    ReviewRepository reviewRepository;

    @Mock
    ReviewCycleRepository reviewCycleRepository;

    @Mock
    MemberRepository memberRepository;

    @Mock
    CycleOptionRepository cycleOptionRepository;

    @Mock
    CycleSelectionResolverRegistry cycleSelectionResolverRegistry;

    @Mock
    NotificationHistoryRepository notificationHistoryRepository;

    @Spy
    Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("UTC"));

    @InjectMocks
    ReviewService reviewService;

    LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
    }

    @Test
    @DisplayName("리뷰와 리뷰 주기를 저장한다")
    void saveReview() {
        // given
        final DeviceIdentifier identifier = DeviceIdentifier.from("device-id");
        final String urlValue = "https://test.com";
        final DefaultCycleSelection cycleSelection = new DefaultCycleSelection("EBBINGHAUS");
        final ReviewSaveInput input = ReviewSaveInput.of(identifier, urlValue, cycleSelection);

        final Email email = Email.from("test@test.com");
        final Member member = Member.withoutId(email);
        final Review review = Review.withoutId(member, ReviewURL.from(urlValue));
        final ReviewCycle cycle = ReviewCycle.withoutId(review, now.plusDays(1));

        final List<Duration> durations = List.of(Duration.ofDays(1));

        given(memberRepository.findByIdentifier(any(DeviceIdentifier.class))).willReturn(Optional.of(member));
        given(cycleSelectionResolverRegistry.resolve(cycleSelection)).willReturn(durations);
        given(reviewRepository.save(any(Review.class))).willReturn(review);
        given(reviewCycleRepository.saveAll(anyList())).willReturn(List.of(cycle));

        // when
        final ReviewSaveOutput actual = reviewService.saveReview(input);

        // then
        final ArgumentCaptor<List<NotificationHistory>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationHistoryRepository).saveAll(captor.capture());

        final LocalDateTime expectedDeadline = now.plusDays(1).plusHours(24);
        assertSoftly(softAssertions -> {
            softAssertions.assertThat(actual.url()).isEqualTo(ReviewURL.from(urlValue));
            softAssertions.assertThat(actual.scheduledAts()).hasSize(1);
            softAssertions.assertThat(captor.getValue()).allMatch(h -> h.getStatus() == NotificationStatus.PENDING);
            softAssertions.assertThat(captor.getValue()).allMatch(h -> h.getDeadline().equals(expectedDeadline));
        });

        verify(memberRepository).findByIdentifier(any(DeviceIdentifier.class));
        verify(cycleSelectionResolverRegistry).resolve(cycleSelection);
        verify(reviewRepository).save(any(Review.class));
        verify(reviewCycleRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("존재하지 않는 디바이스 아이디일 경우 예외를 던진다")
    void saveReview_fail_notFoundDevice() {
        // given
        final DefaultCycleSelection cycleSelection = new DefaultCycleSelection("EBBINGHAUS");
        final ReviewSaveInput input = ReviewSaveInput.of(
                DeviceIdentifier.from("not-found"),
                "https://test.com",
                cycleSelection
        );
        given(memberRepository.findByIdentifier(any(DeviceIdentifier.class))).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> reviewService.saveReview(input))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("유효하지 않은 디바이스입니다");
    }

    @Test
    @DisplayName("사용자가 선호 알림 시간을 설정한 경우 1일 이상의 주기는 해당 시간에 맞춰 조정된다")
    void saveReview_withPreferredNotificationTime() {
        // given
        final DeviceIdentifier identifier = DeviceIdentifier.from("device-id");
        final String urlValue = "https://test.com";
        final DefaultCycleSelection cycleSelection = new DefaultCycleSelection("EBBINGHAUS");
        final ReviewSaveInput input = ReviewSaveInput.of(identifier, urlValue, cycleSelection);

        final Email email = Email.from("test@test.com");
        final Member member = Member.withoutId(email);
        final LocalTime preferredTime = LocalTime.of(0, 0);
        member.updateNotificationTime(preferredTime);

        final Review review = Review.withoutId(member, ReviewURL.from(urlValue));

        final List<Duration> durations = List.of(Duration.ofMinutes(10), Duration.ofDays(1));

        given(memberRepository.findByIdentifier(any(DeviceIdentifier.class))).willReturn(Optional.of(member));
        given(cycleSelectionResolverRegistry.resolve(cycleSelection)).willReturn(durations);
        given(reviewRepository.save(any(Review.class))).willReturn(review);

        final ArgumentCaptor<List<ReviewCycle>> cycleCaptor = ArgumentCaptor.forClass(List.class);
        given(reviewCycleRepository.saveAll(cycleCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        reviewService.saveReview(input);

        // then
        final List<ReviewCycle> capturedCycles = cycleCaptor.getValue();
        assertSoftly(softAssertions -> {
            softAssertions.assertThat(capturedCycles).hasSize(2);
            softAssertions.assertThat(capturedCycles.getFirst().getScheduledAt())
                    .isEqualTo(now.plusMinutes(10));
            softAssertions.assertThat(capturedCycles.get(1).getScheduledAt())
                    .isEqualTo(now.plusDays(1).with(preferredTime));
        });
    }

    @Test
    @DisplayName("사용자가 소유한 커스텀 주기로 리뷰를 저장한다")
    void saveReview_withCustomCycle_owner() {
        // given
        final long cycleOptionId = 1L;
        final DeviceIdentifier identifier = DeviceIdentifier.from("device-id");
        final CustomCycleSelection cycleSelection = new CustomCycleSelection(cycleOptionId);
        final ReviewSaveInput input = ReviewSaveInput.of(identifier, "https://test.com", cycleSelection);

        final Member member = Member.withoutId(Email.from("test@test.com"));
        final Review review = Review.withoutId(member, input.url());
        final ReviewCycle cycle = ReviewCycle.withoutId(review, now.plusDays(1));
        final CycleOption cycleOption = mock(CycleOption.class);
        final List<Duration> durations = List.of(Duration.ofDays(1));

        given(memberRepository.findByIdentifier(identifier)).willReturn(Optional.of(member));
        given(reviewRepository.save(any(Review.class))).willReturn(review);
        given(cycleOptionRepository.findById(cycleOptionId)).willReturn(Optional.of(cycleOption));
        given(cycleOption.isOwner(member)).willReturn(true);
        given(cycleSelectionResolverRegistry.resolve(cycleSelection)).willReturn(durations);
        given(reviewCycleRepository.saveAll(anyList())).willReturn(List.of(cycle));

        // when
        reviewService.saveReview(input);

        // then
        verify(cycleOptionRepository).findById(cycleOptionId);
        verify(cycleOption).isOwner(member);
        verify(cycleSelectionResolverRegistry).resolve(cycleSelection);
    }

    @Test
    @DisplayName("존재하지 않는 커스텀 주기일 경우 예외를 던진다")
    void saveReview_withCustomCycle_notFoundCycleOption() {
        // given
        final long cycleOptionId = 999L;
        final DeviceIdentifier identifier = DeviceIdentifier.from("device-id");
        final CustomCycleSelection cycleSelection = new CustomCycleSelection(cycleOptionId);
        final ReviewSaveInput input = ReviewSaveInput.of(identifier, "https://test.com", cycleSelection);

        final Member member = Member.withoutId(Email.from("test@test.com"));
        final Review review = Review.withoutId(member, input.url());

        given(memberRepository.findByIdentifier(identifier)).willReturn(Optional.of(member));
        given(reviewRepository.save(any(Review.class))).willReturn(review);
        given(cycleOptionRepository.findById(cycleOptionId)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> reviewService.saveReview(input))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("본인이 소유하지 않은 커스텀 주기일 경우 예외를 던진다")
    void saveReview_withCustomCycle_notOwner() {
        // given
        final long cycleOptionId = 10L;
        final DeviceIdentifier identifier = DeviceIdentifier.from("device-id");
        final CustomCycleSelection cycleSelection = new CustomCycleSelection(cycleOptionId);
        final ReviewSaveInput input = ReviewSaveInput.of(identifier, "https://test.com", cycleSelection);

        final Member member = Member.withoutId(Email.from("test@test.com"));
        final Review review = Review.withoutId(member, input.url());
        final CycleOption cycleOption = mock(CycleOption.class);

        given(memberRepository.findByIdentifier(identifier)).willReturn(Optional.of(member));
        given(reviewRepository.save(any(Review.class))).willReturn(review);
        given(cycleOptionRepository.findById(cycleOptionId)).willReturn(Optional.of(cycleOption));
        given(cycleOption.isOwner(member)).willReturn(false);

        // when
        // then
        assertThatThrownBy(() -> reviewService.saveReview(input))
                .isInstanceOf(NotFoundException.class);
    }
}
