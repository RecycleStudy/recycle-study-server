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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class NotificationHistoryTest {

    private static Stream<Arguments> provideInvalidValue() {
        final ReviewCycle reviewCycle = createReviewCycle();
        final NotificationStatus status = NotificationStatus.PENDING;

        return Stream.of(
                Arguments.of(null, status),
                Arguments.of(reviewCycle, null),
                Arguments.of(null, null)
        );
    }

    private static ReviewCycle createReviewCycle() {
        final Email email = Email.from("test@test.com");
        final Member member = Member.withoutId(email);
        final Review review = Review.withoutId(member, ReviewURL.from("https://test.com"));
        return ReviewCycle.withoutId(review, LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("NotificationHistory를 생성할 수 있다")
    void withoutId() {
        // given
        final ReviewCycle reviewCycle = createReviewCycle();
        final NotificationStatus status = NotificationStatus.PENDING;

        // when
        final NotificationHistory actual = NotificationHistory.withoutId(reviewCycle, status);

        // then
        assertSoftly(softAssertions -> {
            softAssertions.assertThat(actual.getReviewCycle()).isEqualTo(reviewCycle);
            softAssertions.assertThat(actual.getStatus()).isEqualTo(status);
        });
    }

    @ParameterizedTest
    @MethodSource("provideInvalidValue")
    @DisplayName("null로 생성 시도 시, 예외를 던진다")
    void throwExceptionWhenNull(
            final ReviewCycle reviewCycle,
            final NotificationStatus status
    ) {
        // given
        // when
        // then
        assertThatThrownBy(() -> NotificationHistory.withoutId(reviewCycle, status))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
