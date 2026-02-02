package com.recyclestudy.cycle.domain;

import com.recyclestudy.exception.BadRequestException;
import com.recyclestudy.member.domain.Email;
import com.recyclestudy.member.domain.Member;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CycleDurationsTest {

    private CycleOption cycleOption;

    @BeforeEach
    void setup() {
        final Member member = Member.withoutId(Email.from("test@test.com"));
        cycleOption = CycleOption.withoutId(
                member,
                CycleOptionTitle.from("title"),
                OptionType.CUSTOM,
                List.of(Duration.ofMinutes(10))
        );
    }

    private static Stream<Arguments> provideInvalidDurations() {
        return Stream.of(
                Arguments.of(List.of(), "주기는 최소 1개 이상이어야 합니다."),
                Arguments.of(List.of("PT5M"), "주기는 10분 단위여야 합니다."),
                Arguments.of(List.of("P366D"), "주기는 최대 1년 이내여야 합니다.")
        );
    }

    @Test
    @DisplayName("CycleDurations를 생성할 수 있다")
    void create() {
        // given
        final List<CycleOptionDuration> durations = List.of(
                CycleOptionDuration.of(cycleOption, Duration.ofMinutes(10)),
                CycleOptionDuration.of(cycleOption, Duration.ofDays(1))
        );

        // when
        final CycleDurations actual = new CycleDurations(durations);

        // then
        assertThat(actual.getValues()).hasSize(2);
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("provideInvalidDurations")
    @DisplayName("유효하지 않은 주기로 생성 시 예외를 던진다")
    void create_invalid(List<String> durationStrings, String errorMessage) {
        // given
        final List<CycleOptionDuration> durations = durationStrings.stream()
                .map(d -> CycleOptionDuration.of(cycleOption, Duration.parse(d)))
                .toList();

        // when
        // then
        assertThatThrownBy(() -> new CycleDurations(durations))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(errorMessage);
    }
}
