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

class ReviewCycleTest {

    private static Stream<Arguments> provideInvalidValue() {
        final Email email = Email.from("test@test.com");
        final Member member = Member.withoutId(email);
        final Review review = Review.withoutId(member, ReviewURL.from("https://test.com"));
        final LocalDateTime scheduledAt = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        return Stream.of(
                Arguments.of(null, scheduledAt),
                Arguments.of(review, null),
                Arguments.of(null, null)
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

        // when
        final ReviewCycle actual = ReviewCycle.withoutId(review, scheduledAt);

        // then
        assertThat(actual.getReview()).isEqualTo(review);
        assertThat(actual.getScheduledAt()).isEqualTo(scheduledAt);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidValue")
    @DisplayName("null로 생성 시도 시, 예외를 던진다")
    void throwExceptionWhenNull(
            final Review review,
            final LocalDateTime scheduledAt
    ) {
        // given
        // when
        // then
        assertThatThrownBy(() -> ReviewCycle.withoutId(review, scheduledAt))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
