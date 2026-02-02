package com.recyclestudy.cycle.util;

import com.recyclestudy.exception.BadRequestException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CycleDurationParserTest {

    @Test
    @DisplayName("문자열 포맷 리스트를 Duration 리스트로 변환한다")
    void parseList() {
        // given
        final List<String> formats = List.of("PT10M", "PT1H", "P1D");

        // when
        final List<Duration> actual = CycleDurationParser.parseList(formats);

        // then
        assertThat(actual).containsExactly(
                Duration.ofMinutes(10),
                Duration.ofHours(1),
                Duration.ofDays(1)
        );
    }

    @Test
    @DisplayName("잘못된 형식의 문자열이 포함되면 BadRequestException이 발생한다")
    void parseList_invalidFormat_throwsException() {
        // given
        final List<String> formats = List.of("PT10M", "10분");

        // when
        // then
        assertThatThrownBy(() -> CycleDurationParser.parseList(formats))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("잘못된 주기 형식입니다");
    }
}
