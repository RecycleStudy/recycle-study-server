package com.recyclestudy.cycle.service.resolver;

import com.recyclestudy.cycle.domain.DefaultCycleOption;
import com.recyclestudy.cycle.domain.selection.DefaultCycleSelection;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultCycleSelectionResolverTest {

    private final DefaultCycleSelectionResolver resolver = new DefaultCycleSelectionResolver();

    @Test
    @DisplayName("지원하는 타입은 DefaultCycleSelection이다")
    void getSupportedType() {
        // when
        final Class<DefaultCycleSelection> supportedType = resolver.getSupportedType();

        // then
        assertThat(supportedType).isEqualTo(DefaultCycleSelection.class);
    }

    @Test
    @DisplayName("기본 주기 선택을 resolve하면 해당 주기의 durations를 반환한다")
    void resolve() {
        // given
        final DefaultCycleSelection selection = new DefaultCycleSelection("EBBINGHAUS");

        // when
        final List<Duration> durations = resolver.resolve(selection);

        // then
        assertThat(durations).isEqualTo(DefaultCycleOption.EBBINGHAUS.getDurations());
    }
}
