package com.recyclestudy.cycle.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CycleOptionTitleTest {

    @Test
    @DisplayName("from 메서드를 통해 CycleOptionTitle을 생성할 수 있다")
    void from() {
        // given
        final String value = "title";

        // when
        final CycleOptionTitle actual = CycleOptionTitle.from(value);

        // then
        assertThat(actual.getValue()).isEqualTo(value);
    }

    @Test
    @DisplayName("null로 생성 시도 시, 예외를 던진다")
    void throwExceptionWhenNull() {
        // when & then
        assertThatThrownBy(() -> CycleOptionTitle.from(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
