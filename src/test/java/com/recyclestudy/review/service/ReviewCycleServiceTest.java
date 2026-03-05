package com.recyclestudy.review.service;

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
import com.recyclestudy.review.service.input.NextReviewInput;
import com.recyclestudy.review.service.input.ReviewSendInput;
import com.recyclestudy.review.service.output.NextReviewOutput;
import com.recyclestudy.review.service.output.ReviewSendOutput;
import com.recyclestudy.review.service.output.ReviewSendOutput.ReviewSendElement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewCycleServiceTest {

    @Mock
    private ReviewCycleRepository reviewCycleRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private NotificationHistoryRepository notificationHistoryRepository;

    @InjectMocks
    private ReviewCycleService reviewCycleService;

    @Test
    @DisplayName("스케줄된 시간에 해당하는 복습 사이클을 조회한다")
    void findTargetReviewCycle_success() {
        // given
        final LocalDateTime scheduledAt = LocalDateTime.of(2025, 1, 1, 8, 0);
        final ReviewSendInput input = ReviewSendInput.from(scheduledAt);

        final Member member = Member.withoutId(Email.from("user@test.com"));
        final Review review = Review.withoutId(member, ReviewURL.from("https://example.com/article"));
        final ReviewCycle reviewCycle = ReviewCycle.withoutId(review, scheduledAt);

        given(reviewCycleRepository.findAllByScheduledAt(scheduledAt, NotificationStatus.PENDING)).willReturn(
                List.of(reviewCycle));

        // when
        final ReviewSendOutput result = reviewCycleService.findTargetReviewCycle(input);

        // then
        assertSoftly(softAssertions -> {
            softAssertions.assertThat(result.elements()).hasSize(1);
            softAssertions.assertThat(result.elements().getFirst().email()).isEqualTo(Email.from("user@test.com"));
        });
        verify(reviewCycleRepository).findAllByScheduledAt(scheduledAt, NotificationStatus.PENDING);
    }

    @Test
    @DisplayName("복습 대상이 없으면 빈 결과를 반환한다")
    void findTargetReviewCycle_empty() {
        // given
        final LocalDateTime scheduledAt = LocalDateTime.of(2025, 1, 1, 8, 0);
        final ReviewSendInput input = ReviewSendInput.from(scheduledAt);

        given(reviewCycleRepository.findAllByScheduledAt(scheduledAt, NotificationStatus.PENDING)).willReturn(
                List.of());

        // when
        final ReviewSendOutput result = reviewCycleService.findTargetReviewCycle(input);

        // then
        assertThat(result.elements()).isEmpty();
    }

    @Test
    @DisplayName("동일 사용자의 여러 복습 URL을 하나의 요소로 그룹화한다")
    void findTargetReviewCycle_groupByEmail() {
        // given
        final LocalDateTime scheduledAt = LocalDateTime.of(2025, 1, 1, 8, 0);
        final ReviewSendInput input = ReviewSendInput.from(scheduledAt);

        final Member member = Member.withoutId(Email.from("user@test.com"));
        final Review review1 = Review.withoutId(member, ReviewURL.from("https://example.com/article1"));
        final Review review2 = Review.withoutId(member, ReviewURL.from("https://example.com/article2"));
        final ReviewCycle cycle1 = ReviewCycle.withoutId(review1, scheduledAt);
        final ReviewCycle cycle2 = ReviewCycle.withoutId(review2, scheduledAt);

        given(reviewCycleRepository.findAllByScheduledAt(scheduledAt, NotificationStatus.PENDING)).willReturn(
                List.of(cycle1, cycle2));

        // when
        final ReviewSendOutput result = reviewCycleService.findTargetReviewCycle(input);

        // then
        assertSoftly(softAssertions -> {
            softAssertions.assertThat(result.elements()).hasSize(1);

            final ReviewSendElement element = result.elements().getFirst();
            softAssertions.assertThat(element.email()).isEqualTo(Email.from("user@test.com"));
            softAssertions.assertThat(element.targetUrls()).hasSize(2);
        });
    }

    @Test
    @DisplayName("여러 사용자의 복습 사이클을 각각 그룹화하여 반환한다")
    void findTargetReviewCycle_multipleUsers() {
        // given
        final LocalDateTime scheduledAt = LocalDateTime.of(2025, 1, 1, 8, 0);
        final ReviewSendInput input = ReviewSendInput.from(scheduledAt);

        final Member member1 = Member.withoutId(Email.from("user1@test.com"));
        final Member member2 = Member.withoutId(Email.from("user2@test.com"));
        final Review review1 = Review.withoutId(member1, ReviewURL.from("https://example.com/article1"));
        final Review review2 = Review.withoutId(member2, ReviewURL.from("https://example.com/article2"));
        final ReviewCycle cycle1 = ReviewCycle.withoutId(review1, scheduledAt);
        final ReviewCycle cycle2 = ReviewCycle.withoutId(review2, scheduledAt);

        given(reviewCycleRepository.findAllByScheduledAt(scheduledAt, NotificationStatus.PENDING)).willReturn(
                List.of(cycle1, cycle2));

        // when
        final ReviewSendOutput result = reviewCycleService.findTargetReviewCycle(input);

        // then
        assertSoftly(softAssertions -> {
            softAssertions.assertThat(result.elements()).hasSize(2);

            final List<Email> emails = result.elements().stream()
                    .map(ReviewSendElement::email)
                    .toList();
            softAssertions.assertThat(emails).containsExactlyInAnyOrder(
                    Email.from("user1@test.com"),
                    Email.from("user2@test.com")
            );
        });
    }

    @Test
    @DisplayName("PENDING이 없을 때 empty()를 반환한다")
    void findNextReview_empty() {
        // given
        final DeviceIdentifier identifier = DeviceIdentifier.from("device-id");
        final NextReviewInput input = NextReviewInput.from(identifier);
        final Member member = Member.withoutId(Email.from("user@test.com"));

        given(memberRepository.findByIdentifier(identifier)).willReturn(Optional.of(member));
        given(notificationHistoryRepository.findAllByMemberAndStatus(member.getId(), NotificationStatus.PENDING))
                .willReturn(List.of());

        // when
        final NextReviewOutput result = reviewCycleService.findNextReview(input);

        // then
        assertSoftly(softly -> {
            softly.assertThat(result.scheduledAt()).isNull();
            softly.assertThat(result.count()).isEqualTo(0);
        });
    }

    @Test
    @DisplayName("가장 빠른 시간 그룹 count만 반환한다")
    void findNextReview_returnsEarliestGroup() {
        // given
        final DeviceIdentifier identifier = DeviceIdentifier.from("device-id");
        final NextReviewInput input = NextReviewInput.from(identifier);
        final Member member = Member.withoutId(Email.from("user@test.com"));
        final LocalDateTime t1 = LocalDateTime.of(2026, 3, 6, 9, 0);
        final LocalDateTime t2 = LocalDateTime.of(2026, 3, 7, 9, 0);

        final Review review = Review.withoutId(member, ReviewURL.from("https://example.com"));
        final NotificationHistory nh1 = NotificationHistory.withoutId(ReviewCycle.withoutId(review, t1),
                NotificationStatus.PENDING);
        final NotificationHistory nh2 = NotificationHistory.withoutId(ReviewCycle.withoutId(review, t2),
                NotificationStatus.PENDING);

        given(memberRepository.findByIdentifier(identifier)).willReturn(Optional.of(member));
        given(notificationHistoryRepository.findAllByMemberAndStatus(member.getId(), NotificationStatus.PENDING))
                .willReturn(List.of(nh1, nh2));

        // when
        final NextReviewOutput result = reviewCycleService.findNextReview(input);

        // then
        assertSoftly(softly -> {
            softly.assertThat(result.scheduledAt()).isEqualTo(t1);
            softly.assertThat(result.count()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("같은 scheduledAt의 여러 NotificationHistory count를 정확히 집계한다")
    void findNextReview_countsSameScheduledAt() {
        // given
        final DeviceIdentifier identifier = DeviceIdentifier.from("device-id");
        final NextReviewInput input = NextReviewInput.from(identifier);
        final Member member = Member.withoutId(Email.from("user@test.com"));
        final LocalDateTime t1 = LocalDateTime.of(2026, 3, 6, 9, 0);

        final Review review = Review.withoutId(member, ReviewURL.from("https://example.com"));
        final NotificationHistory nh1 = NotificationHistory.withoutId(ReviewCycle.withoutId(review, t1),
                NotificationStatus.PENDING);
        final NotificationHistory nh2 = NotificationHistory.withoutId(ReviewCycle.withoutId(review, t1),
                NotificationStatus.PENDING);
        final NotificationHistory nh3 = NotificationHistory.withoutId(ReviewCycle.withoutId(review, t1),
                NotificationStatus.PENDING);

        given(memberRepository.findByIdentifier(identifier)).willReturn(Optional.of(member));
        given(notificationHistoryRepository.findAllByMemberAndStatus(member.getId(), NotificationStatus.PENDING))
                .willReturn(List.of(nh1, nh2, nh3));

        // when
        final NextReviewOutput result = reviewCycleService.findNextReview(input);

        // then
        assertSoftly(softly -> {
            softly.assertThat(result.scheduledAt()).isEqualTo(t1);
            softly.assertThat(result.count()).isEqualTo(3);
        });
    }

    @Test
    @DisplayName("미등록 identifier이면 UnauthorizedException을 던진다")
    void findNextReview_unauthorized() {
        // given
        final DeviceIdentifier identifier = DeviceIdentifier.from("unknown-id");
        final NextReviewInput input = NextReviewInput.from(identifier);

        given(memberRepository.findByIdentifier(identifier)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> reviewCycleService.findNextReview(input))
                .isInstanceOf(UnauthorizedException.class);
    }
}
