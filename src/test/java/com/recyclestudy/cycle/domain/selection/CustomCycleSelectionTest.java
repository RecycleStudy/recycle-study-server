package com.recyclestudy.cycle.domain.selection;

import com.recyclestudy.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomCycleSelectionTest {

    @Test
    @DisplayName("유효한 ID로 생성할 수 있다")
    void create() {
        // given
        final Long id = 1L;

        // when
        final CustomCycleSelection selection = new CustomCycleSelection(id);

        // then
        assertThat(selection.id()).isEqualTo(id);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1, -100})
    @DisplayName("유효하지 않은 ID로 생성 시 예외를 던진다")
    void create_invalidId(final Long invalidId) {
        // when
        // then
        assertThatThrownBy(() -> new CustomCycleSelection(invalidId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("유효하지 않은 커스텀 주기 ID입니다");
    }
}
