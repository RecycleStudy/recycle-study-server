package com.recyclestudy.cycle.service;

import com.recyclestudy.cycle.domain.CycleOption;
import com.recyclestudy.cycle.domain.CycleOptionDuration;
import com.recyclestudy.cycle.domain.CycleOptionTitle;
import com.recyclestudy.cycle.domain.OptionType;
import com.recyclestudy.cycle.repository.CycleOptionRepository;
import com.recyclestudy.cycle.service.output.CycleOptionFindOutput;
import com.recyclestudy.exception.UnauthorizedException;
import com.recyclestudy.member.domain.DeviceIdentifier;
import com.recyclestudy.member.domain.Email;
import com.recyclestudy.member.domain.Member;
import com.recyclestudy.member.repository.MemberRepository;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CycleOptionServiceTest {

    @Mock
    MemberRepository memberRepository;

    @Mock
    CycleOptionRepository cycleOptionRepository;

    @InjectMocks
    CycleOptionService cycleOptionService;

    @Test
    @DisplayName("멤버가 가진 주기 옵션을 조회할 수 있다")
    void findAllCycleOptions() {
        // given
        final DeviceIdentifier identifier = DeviceIdentifier.from("device-id");
        final Member member = Member.withoutId(Email.from("test@test.com"));
        final CycleOption cycleOption = CycleOption.withoutId(member, CycleOptionTitle.from("title"),
                OptionType.CUSTOM);
        cycleOption.getDurations().add(CycleOptionDuration.of(cycleOption, Duration.ofMinutes(10)));

        given(memberRepository.findByIdentifier(identifier)).willReturn(Optional.of(member));
        given(cycleOptionRepository.findAllByMember(member)).willReturn(List.of(cycleOption));

        // when
        final CycleOptionFindOutput actual = cycleOptionService.findAllCycleOptions(identifier);

        // then
        assertSoftly(softAssertions -> {
            softAssertions.assertThat(actual.options()).hasSize(1);
            softAssertions.assertThat(actual.options().getFirst().title().getValue()).isEqualTo("title");
            softAssertions.assertThat(actual.options().getFirst().durations()).hasSize(1);
            softAssertions.assertThat(actual.options().getFirst().durations().getFirst())
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
}
