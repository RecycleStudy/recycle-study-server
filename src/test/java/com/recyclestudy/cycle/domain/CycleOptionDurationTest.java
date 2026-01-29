package com.recyclestudy.cycle.domain;

import com.recyclestudy.member.domain.Email;
import com.recyclestudy.member.domain.Member;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class CycleOptionDurationTest {

    @Test
    @DisplayName("of 메서드를 통해 CycleOptionDuration을 생성할 수 있다")
    void of() {
        // given
        final Member member = Member.withoutId(Email.from("test@test.com"));
        final CycleOption cycleOption = CycleOption.withoutId(member, CycleOptionTitle.from("title"), OptionType.CUSTOM);
        final Duration duration = Duration.ofMinutes(10);

        // when
        final CycleOptionDuration actual = CycleOptionDuration.of(cycleOption, duration);

        // then
        assertSoftly(softAssertions -> {
            softAssertions.assertThat(actual.getId().getCycleOption()).isEqualTo(cycleOption);
            softAssertions.assertThat(actual.getId().getDuration()).isEqualTo(duration);
        });
    }
}