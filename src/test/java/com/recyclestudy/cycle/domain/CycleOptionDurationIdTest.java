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

class CycleOptionDurationIdTest {

    private static Stream<Arguments> provideInvalidValue() {
        final Member member = Member.withoutId(Email.from("test@test.com"));
        final CycleOption cycleOption = CycleOption.withoutId(member, CycleOptionTitle.from("title"), OptionType.CUSTOM,
                List.of(Duration.ofMinutes(10)));
        final Duration duration = Duration.ofMinutes(10);

        return Stream.of(
                Arguments.of(null, duration),
                Arguments.of(cycleOption, null)
        );
    }

    @Test
    @DisplayName("withoutId 메서드를 통해 CycleOptionDurationId를 생성할 수 있다")
    void of() {
        // given
        final Member member = Member.withoutId(Email.from("test@test.com"));
        final CycleOption cycleOption = CycleOption.withoutId(
                member,
                CycleOptionTitle.from("title"),
                OptionType.CUSTOM,
                List.of(Duration.ofMinutes(10))
        );
        final Duration duration = Duration.ofMinutes(10);

        // when
        final CycleOptionDurationId actual = CycleOptionDurationId.of(cycleOption, duration);

        // then
        assertSoftly(softAssertions -> {
            softAssertions.assertThat(actual.getCycleOption()).isEqualTo(cycleOption);
            softAssertions.assertThat(actual.getDuration()).isEqualTo(duration);
        });
    }

    @ParameterizedTest
    @MethodSource("provideInvalidValue")
    @DisplayName("생성 시 필수값이 null이면 예외를 던진다")
    void throwExceptionWhenNull(final CycleOption cycleOption, final Duration duration) {
        // given
        // when
        // then
        assertThatThrownBy(() -> CycleOptionDurationId.of(cycleOption, duration))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
