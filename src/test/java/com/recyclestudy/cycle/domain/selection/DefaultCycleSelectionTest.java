package com.recyclestudy.cycle.domain.selection;

import com.recyclestudy.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultCycleSelectionTest {

    @Test
    @DisplayName("유효한 기본 주기 코드로 생성할 수 있다")
    void create() {
        // given
        final String code = "EBBINGHAUS";

        // when
        final DefaultCycleSelection selection = new DefaultCycleSelection(code);

        // then
        assertThat(selection.code()).isEqualTo(code);
    }

    @Test
    @DisplayName("존재하지 않는 기본 주기 코드로 생성 시 예외를 던진다")
    void create_invalidCode() {
        // given
        final String invalidCode = "INVALID_CODE";

        // when
        // then
        assertThatThrownBy(() -> new DefaultCycleSelection(invalidCode))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("존재하지 않는 기본 주기입니다");
    }
}
