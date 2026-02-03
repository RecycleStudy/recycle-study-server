package com.recyclestudy.member.domain;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class DeviceTest {

    private static Stream<Arguments> provideInvalidValue() {
        final Email email = Email.from("test@test.com");
        final Member member = Member.withoutId(email);
        final DeviceIdentifier deviceIdentifier = DeviceIdentifier.from("test");
        final ActivationExpiredDateTime activationExpiredDateTime = ActivationExpiredDateTime.create(
                LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));

        return Stream.of(
                Arguments.of(member, null, activationExpiredDateTime),
                Arguments.of(null, deviceIdentifier, activationExpiredDateTime),
                Arguments.of(member, deviceIdentifier, null),
                Arguments.of(null, null, null)
        );
    }

    @Test
    @DisplayName("withoutId 메서드를 통해 Device를 생성할 수 있다")
    void withoutId() {
        // given
        final Email email = Email.from("test@test.com");
        final Member member = Member.withoutId(email);
        final DeviceIdentifier deviceIdentifier = DeviceIdentifier.from("test");
        final ActivationExpiredDateTime activationExpiredDateTime = ActivationExpiredDateTime.create(
                LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));

        // when
        final Device actual = Device.withoutId(member, deviceIdentifier, false, activationExpiredDateTime);

        // then
        assertSoftly(softAssertions -> {
            softAssertions.assertThat(actual.getMember().getEmail()).isEqualTo(email);
            softAssertions.assertThat(actual.getIdentifier()).isEqualTo(deviceIdentifier);
            softAssertions.assertThat(actual.isActive()).isFalse();
            softAssertions.assertThat(actual.getActivationExpiresAt()).isEqualTo(activationExpiredDateTime);
        });
    }

    @ParameterizedTest
    @MethodSource("provideInvalidValue")
    @DisplayName("null로 생성 시도 시, 예외를 던진다")
    void throwExceptionWhenNull(final Member member, final DeviceIdentifier deviceIdentifier,
                                final ActivationExpiredDateTime activationExpiredDateTime) {
        //given
        //when
        //then
        assertThatThrownBy(() -> Device.withoutId(member, deviceIdentifier, false, activationExpiredDateTime))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("activate 메서드를 통해 Device를 활성화할 수 있다")
    void activate() {
        // given
        final LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        final Device device = Device.withoutId(
                Member.withoutId(Email.from("test@test.com")),
                DeviceIdentifier.from("test"),
                false,
                ActivationExpiredDateTime.create(now)
        );

        // when
        device.activate(now);

        // then
        assertSoftly(softAssertions -> {
            softAssertions.assertThat(device.isActive()).isTrue();
        });
    }

    @Test
    @DisplayName("만료 시간이 지난 후 activate 메서드 호출 시, 예외를 던진다")
    void activate_fail_expired() {
        // given
        final LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        final Device device = Device.withoutId(
                Member.withoutId(Email.from("test@test.com")),
                DeviceIdentifier.from("test"),
                false,
                ActivationExpiredDateTime.create(now)
        );

        // when
        // then
        final LocalDateTime expiredTime = now.plusMinutes(6);
        assertThatThrownBy(() -> device.activate(expiredTime))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("verifyOwner 메서드를 통해 소유자를 검증할 수 있다")
    void verifyOwner() {
        // given
        final Email email = Email.from("test@test.com");
        final Device device = Device.withoutId(
                Member.withoutId(email),
                DeviceIdentifier.from("test"),
                false,
                ActivationExpiredDateTime.create(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
        );

        // when
        // then
        assertThatCode(() -> device.verifyOwner(email))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("소유자가 아닌 이메일로 verifyOwner 메서드 호출 시, 예외를 던진다")
    void verifyOwner_fail() {
        // given
        final Email email = Email.from("test@test.com");
        final Device device = Device.withoutId(
                Member.withoutId(email),
                DeviceIdentifier.from("test"),
                false,
                ActivationExpiredDateTime.create(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
        );

        // when
        // then
        final Email otherEmail = Email.from("other@test.com");
        assertThatThrownBy(() -> device.verifyOwner(otherEmail))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("디바이스 소유자가 아닙니다.");
    }
}
