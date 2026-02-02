package com.recyclestudy.cycle.service.resolver;

import com.recyclestudy.cycle.domain.CycleOption;
import com.recyclestudy.cycle.domain.CycleOptionTitle;
import com.recyclestudy.cycle.domain.selection.CustomCycleSelection;
import com.recyclestudy.cycle.repository.CycleOptionRepository;
import com.recyclestudy.exception.NotFoundException;
import com.recyclestudy.member.domain.Email;
import com.recyclestudy.member.domain.Member;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CustomCycleSelectionResolverTest {

    @Mock
    CycleOptionRepository cycleOptionRepository;

    @InjectMocks
    CustomCycleSelectionResolver resolver;

    @Test
    @DisplayName("지원하는 타입은 CustomCycleSelection이다")
    void getSupportedType() {
        // when
        final Class<CustomCycleSelection> supportedType = resolver.getSupportedType();

        // then
        assertThat(supportedType).isEqualTo(CustomCycleSelection.class);
    }

    @Test
    @DisplayName("커스텀 주기 선택을 resolve하면 해당 주기의 durations를 반환한다")
    void resolve() {
        // given
        final Long cycleOptionId = 1L;
        final CustomCycleSelection selection = new CustomCycleSelection(cycleOptionId);

        final Member member = Member.withoutId(Email.from("test@test.com"));
        final List<Duration> expectedDurations = List.of(Duration.ofMinutes(10), Duration.ofHours(1));
        final CycleOption cycleOption = CycleOption.withoutId(
                member,
                CycleOptionTitle.from("custom"),
                expectedDurations
        );

        given(cycleOptionRepository.findById(cycleOptionId)).willReturn(Optional.of(cycleOption));

        // when
        final List<Duration> durations = resolver.resolve(selection);

        // then
        assertThat(durations).isEqualTo(expectedDurations);
    }

    @Test
    @DisplayName("존재하지 않는 커스텀 주기를 resolve하면 예외를 던진다")
    void resolve_notFound() {
        // given
        final Long cycleOptionId = 999L;
        final CustomCycleSelection selection = new CustomCycleSelection(cycleOptionId);

        given(cycleOptionRepository.findById(cycleOptionId)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> resolver.resolve(selection))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 주기 옵션입니다.");
    }
}
