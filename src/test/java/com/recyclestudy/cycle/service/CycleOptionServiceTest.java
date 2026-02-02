package com.recyclestudy.cycle.service;

import com.recyclestudy.cycle.domain.CycleOption;
import com.recyclestudy.cycle.domain.CycleOptionTitle;
import com.recyclestudy.cycle.domain.DefaultCycleOption;
import com.recyclestudy.cycle.repository.CycleOptionRepository;
import com.recyclestudy.cycle.service.input.CycleOptionSaveInput;
import com.recyclestudy.cycle.service.input.CycleOptionUpdateInput;
import com.recyclestudy.cycle.service.output.CycleOptionFindOutput;
import com.recyclestudy.cycle.service.output.CycleOptionSaveOutput;
import com.recyclestudy.exception.BadRequestException;
import com.recyclestudy.exception.NotFoundException;
import com.recyclestudy.exception.UnauthorizedException;
import com.recyclestudy.member.domain.DeviceIdentifier;
import com.recyclestudy.member.domain.Email;
import com.recyclestudy.member.domain.Member;
import com.recyclestudy.member.repository.MemberRepository;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CycleOptionServiceTest {

    @Mock
    MemberRepository memberRepository;

    @Mock
    CycleOptionRepository cycleOptionRepository;

    @InjectMocks
    CycleOptionService cycleOptionService;

    private static Stream<Arguments> provideInvalidDurations() {
        return Stream.of(
                Arguments.of(List.of(), "주기는 최소 1개 이상이어야 합니다."),
                Arguments.of(List.of("PT5M"), "주기는 10분 단위여야 합니다."),
                Arguments.of(List.of("P366D"), "주기는 최대 1년 이내여야 합니다.")
        );
    }

    @Test
    @DisplayName("멤버가 가진 주기 옵션을 조회할 수 있다")
    void findAllCycleOptions() {
        // given
        final DeviceIdentifier identifier = DeviceIdentifier.from("device-id");
        final Member member = Member.withoutId(Email.from("test@test.com"));
        final CycleOption cycleOption = CycleOption.withoutId(
                member,
                CycleOptionTitle.from("custom title"),
                List.of(Duration.ofMinutes(10))
        );

        given(memberRepository.findByIdentifier(identifier)).willReturn(Optional.of(member));
        given(cycleOptionRepository.findAllByMember(member)).willReturn(List.of(cycleOption));

        // when
        final CycleOptionFindOutput actual = cycleOptionService.findAllCycleOptions(identifier);

        // then
        assertSoftly(softAssertions -> {
            softAssertions.assertThat(actual.defaultOptions()).hasSize(DefaultCycleOption.getAll().size());
            softAssertions.assertThat(actual.customOptions()).hasSize(1);
            softAssertions.assertThat(actual.customOptions().getFirst().title()).isEqualTo("custom title");
            softAssertions.assertThat(actual.customOptions().getFirst().durations()).hasSize(1);
            softAssertions.assertThat(actual.customOptions().getFirst().durations().getFirst())
                    .isEqualTo(Duration.ofMinutes(10));
        });
    }

    @Test
    @DisplayName("유효하지 않은 디바이스로 조회 시 예외를 던진다")
    void findAllCycleOptions_unauthorized() {
        // given
        final DeviceIdentifier identifier = DeviceIdentifier.from("device-id");

        given(memberRepository.findByIdentifier(identifier)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> cycleOptionService.findAllCycleOptions(identifier))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("새로운 주기 옵션을 저장할 수 있다")
    void saveCycleOption() {
        // given
        final DeviceIdentifier identifier = DeviceIdentifier.from("device-id");
        final Member member = Member.withoutId(Email.from("test@test.com"));
        final CycleOptionSaveInput input = CycleOptionSaveInput.of("title", List.of("PT10M", "PT1H"));

        given(memberRepository.findByIdentifier(identifier)).willReturn(Optional.of(member));
        given(cycleOptionRepository.countByMember(member)).willReturn(0L);
        given(cycleOptionRepository.save(any(CycleOption.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        final CycleOptionSaveOutput actual = cycleOptionService.saveCycleOption(identifier, input);

        // then
        assertSoftly(softAssertions -> {
            softAssertions.assertThat(actual.title()).isEqualTo(input.title());
            softAssertions.assertThat(actual.durations()).hasSize(2);
            softAssertions.assertThat(actual.durations().get(0)).isEqualTo(Duration.ofMinutes(10));
            softAssertions.assertThat(actual.durations().get(1)).isEqualTo(Duration.ofHours(1));
        });
    }

    @Test
    @DisplayName("커스텀 주기가 5개 이상이면 예외를 던진다")
    void saveCycleOption_limitExceeded() {
        // given
        final DeviceIdentifier identifier = DeviceIdentifier.from("device-id");
        final Member member = Member.withoutId(Email.from("test@test.com"));
        final CycleOptionSaveInput input = CycleOptionSaveInput.of("title", List.of("PT10M"));

        given(memberRepository.findByIdentifier(identifier)).willReturn(Optional.of(member));
        given(cycleOptionRepository.countByMember(member)).willReturn(5L);

        // when
        // then
        assertThatThrownBy(() -> cycleOptionService.saveCycleOption(identifier, input))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("커스텀 주기는 최대 5개까지만 생성 가능합니다.");
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("provideInvalidDurations")
    @DisplayName("주기 시간 간격이 유효하지 않으면 예외를 던진다")
    void saveCycleOption_invalidDuration(List<String> durationStrings, String errorMessage) {
        // given
        final DeviceIdentifier identifier = DeviceIdentifier.from("device-id");
        final Member member = Member.withoutId(Email.from("test@test.com"));
        final CycleOptionSaveInput input = CycleOptionSaveInput.of("title", durationStrings);

        given(memberRepository.findByIdentifier(identifier)).willReturn(Optional.of(member));
        given(cycleOptionRepository.countByMember(member)).willReturn(0L);

        // when
        // then
        assertThatThrownBy(() -> cycleOptionService.saveCycleOption(identifier, input))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(errorMessage);
    }

    @Test
    @DisplayName("커스텀 주기 옵션을 수정할 수 있다")
    void updateCycleOption() {
        // given
        final DeviceIdentifier identifier = DeviceIdentifier.from("device-id");
        final Member member = Member.withoutId(Email.from("test@test.com"));
        ReflectionTestUtils.setField(member, "id", 1L);

        final CycleOption cycleOption = CycleOption.withoutId(
                member,
                CycleOptionTitle.from("old"),
                List.of(Duration.ofMinutes(10))
        );
        ReflectionTestUtils.setField(cycleOption, "id", 1L);

        final CycleOptionUpdateInput input = CycleOptionUpdateInput.of("new title", List.of("PT20M"));

        given(memberRepository.findByIdentifier(identifier)).willReturn(Optional.of(member));
        given(cycleOptionRepository.findByIdWithDurations(1L)).willReturn(Optional.of(cycleOption));

        // when
        final CycleOptionSaveOutput actual = cycleOptionService.updateCycleOption(identifier, 1L, input);

        // then
        assertSoftly(softAssertions -> {
            softAssertions.assertThat(actual.title().getValue()).isEqualTo("new title");
            softAssertions.assertThat(actual.durations()).hasSize(1);
            softAssertions.assertThat(actual.durations().get(0)).isEqualTo(Duration.ofMinutes(20));
        });
    }

    @Test
    @DisplayName("소유자가 아닌 경우 수정 시 예외를 던진다")
    void updateCycleOption_notOwner() {
        // given
        final DeviceIdentifier identifier = DeviceIdentifier.from("device-id");
        final Member owner = Member.withoutId(Email.from("owner@test.com"));
        ReflectionTestUtils.setField(owner, "id", 1L);

        final Member other = Member.withoutId(Email.from("other@test.com"));
        ReflectionTestUtils.setField(other, "id", 2L);

        final CycleOption cycleOption = CycleOption.withoutId(
                owner,
                CycleOptionTitle.from("title"),
                List.of(Duration.ofMinutes(10))
        );
        final CycleOptionUpdateInput input = CycleOptionUpdateInput.of("new title", List.of("PT20M"));

        given(memberRepository.findByIdentifier(identifier)).willReturn(Optional.of(other));
        given(cycleOptionRepository.findByIdWithDurations(1L)).willReturn(Optional.of(cycleOption));

        // when
        // then
        assertThatThrownBy(() -> cycleOptionService.updateCycleOption(identifier, 1L, input))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 주기 옵션입니다.");
    }

    @Test
    @DisplayName("커스텀 주기 옵션을 삭제할 수 있다")
    void deleteCycleOption() {
        // given
        final DeviceIdentifier identifier = DeviceIdentifier.from("device-id");
        final Member member = Member.withoutId(Email.from("test@test.com"));
        ReflectionTestUtils.setField(member, "id", 1L);

        final CycleOption cycleOption = CycleOption.withoutId(
                member,
                CycleOptionTitle.from("title"),
                List.of(Duration.ofMinutes(10))
        );
        ReflectionTestUtils.setField(cycleOption, "id", 1L);

        given(memberRepository.findByIdentifier(identifier)).willReturn(Optional.of(member));
        given(cycleOptionRepository.findById(1L)).willReturn(Optional.of(cycleOption));

        // when
        cycleOptionService.deleteCycleOption(identifier, 1L);

        // then
        verify(cycleOptionRepository).delete(cycleOption);
    }
}
