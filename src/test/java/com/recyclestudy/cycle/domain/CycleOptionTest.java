package com.recyclestudy.cycle.domain;

import com.recyclestudy.member.domain.Email;
import com.recyclestudy.member.domain.Member;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class CycleOptionTest {

    private static Stream<Arguments> provideInvalidValue() {
        final Member member = Member.withoutId(Email.from("test@test.com"));
        final CycleOptionTitle title = CycleOptionTitle.from("title");
        final List<Duration> durations = List.of(Duration.ofMinutes(10));

        return Stream.of(
                Arguments.of(null, title, durations),
                Arguments.of(member, null, durations),
                Arguments.of(member, title, null)
        );
    }

    @Test
    @DisplayName("withoutId 메서드를 통해 CycleOption을 생성할 수 있다")
    void withoutId() {
        // given
        final Member member = Member.withoutId(Email.from("test@test.com"));
        final CycleOptionTitle title = CycleOptionTitle.from("title");
        final List<Duration> durations = List.of(Duration.ofMinutes(10), Duration.ofDays(1));

        // when
        final CycleOption actual = CycleOption.withoutId(member, title, durations);

        // then
        assertSoftly(softAssertions -> {
            softAssertions.assertThat(actual.getMember()).isEqualTo(member);
            softAssertions.assertThat(actual.getTitle()).isEqualTo(title);
            softAssertions.assertThat(actual.getDurations()).hasSize(2);
        });
    }

    @ParameterizedTest
    @MethodSource("provideInvalidValue")
    @DisplayName("생성 시 필수값이 null이면 예외를 던진다")
    void throwExceptionWhenNull(
            final Member member,
            final CycleOptionTitle title,
            final List<Duration> durations
    ) {
        // given
        // when
        // then
        assertThatThrownBy(() -> CycleOption.withoutId(member, title, durations))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
