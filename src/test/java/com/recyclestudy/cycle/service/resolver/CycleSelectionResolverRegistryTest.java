package com.recyclestudy.cycle.service.resolver;

import com.recyclestudy.cycle.domain.DefaultCycleOption;
import com.recyclestudy.cycle.domain.selection.CycleSelection;
import com.recyclestudy.cycle.domain.selection.DefaultCycleSelection;
import com.recyclestudy.cycle.repository.CycleOptionRepository;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class CycleSelectionResolverRegistryTest {

    @Mock
    CycleOptionRepository cycleOptionRepository;

    CycleSelectionResolverRegistry registry;

    @BeforeEach
    void setUp() {
        final List<CycleSelectionResolver<?>> resolvers = List.of(
                new DefaultCycleSelectionResolver(),
                new CustomCycleSelectionResolver(cycleOptionRepository)
        );
        registry = new CycleSelectionResolverRegistry(resolvers);
    }

    @Test
    @DisplayName("DefaultCycleSelection을 resolve할 수 있다")
    void resolve_default() {
        // given
        final CycleSelection selection = new DefaultCycleSelection("EBBINGHAUS");

        // when
        final List<Duration> durations = registry.resolve(selection);

        // then
        assertThat(durations).isEqualTo(DefaultCycleOption.EBBINGHAUS.getDurations());
    }

    @Test
    @DisplayName("지원하지 않는 타입을 resolve하면 예외를 던진다")
    void resolve_unsupportedType() {
        // given
        final CycleSelectionResolverRegistry emptyRegistry = new CycleSelectionResolverRegistry(List.of());
        final CycleSelection selection = new DefaultCycleSelection("EBBINGHAUS");

        // when
        // then
        assertThatThrownBy(() -> emptyRegistry.resolve(selection))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("지원하지 않는 주기 타입입니다");
    }
}
