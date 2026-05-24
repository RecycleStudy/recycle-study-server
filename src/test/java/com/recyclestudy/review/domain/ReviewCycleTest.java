package com.recyclestudy.review.domain;

import com.recyclestudy.member.domain.Email;
import com.recyclestudy.member.domain.Member;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class ReviewCycleTest {

    private static Stream<Arguments> provideInvalidValue() {
        final Email email = Email.from("test@test.com");
        final Member member = Member.withoutId(email);
        final Review review = Review.withoutId(member, ReviewURL.from("https://test.com"));
        final LocalDateTime scheduledAt = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        final NotificationStatus status = NotificationStatus.PENDING;
        final LocalDateTime deadline = scheduledAt.plusHours(24);

        return Stream.of(
                Arguments.of(null, scheduledAt, status, deadline),
                Arguments.of(review, null, status, deadline),
                Arguments.of(review, scheduledAt, null, deadline),
                Arguments.of(review, scheduledAt, status, null),
                Arguments.of(null, null, null, null)
        );
    }

    @Test
    @DisplayName("ReviewCycle을 생성할 수 있다")
    void withoutId() {
        // given
        final Email email = Email.from("test@test.com");
        final Member member = Member.withoutId(email);
        final Review review = Review.withoutId(member, ReviewURL.from("https://test.com"));
        final LocalDateTime scheduledAt = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        final NotificationStatus status = NotificationStatus.PENDING;
        final LocalDateTime deadline = scheduledAt.plusHours(24);

        // when
        final ReviewCycle actual = ReviewCycle.withoutId(review, scheduledAt, status, deadline);

        // then
        assertSoftly(softly -> {
            softly.assertThat(actual.getReview()).isEqualTo(review);
            softly.assertThat(actual.getScheduledAt()).isEqualTo(scheduledAt);
            softly.assertThat(actual.getStatus()).isEqualTo(status);
            softly.assertThat(actual.getDeadline()).isEqualTo(deadline);
            softly.assertThat(actual.getFailCount()).isEqualTo(0);
            softly.assertThat(actual.getLastAttemptedAt()).isNull();
        });
    }

    @ParameterizedTest
    @MethodSource("provideInvalidValue")
    @DisplayName("null로 생성 시도 시, 예외를 던진다")
    void throwExceptionWhenNull(
            final Review review,
            final LocalDateTime scheduledAt,
            final NotificationStatus status,
            final LocalDateTime deadline
    ) {
        // given
        // when
        // then
        assertThatThrownBy(() -> ReviewCycle.withoutId(review, scheduledAt, status, deadline))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
