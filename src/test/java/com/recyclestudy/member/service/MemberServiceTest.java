package com.recyclestudy.member.service;

import com.recyclestudy.exception.BadRequestException;
import com.recyclestudy.exception.NotFoundException;
import com.recyclestudy.exception.UnauthorizedException;
import com.recyclestudy.member.domain.ActivationExpiredDateTime;
import com.recyclestudy.member.domain.Device;
import com.recyclestudy.member.domain.DeviceIdentifier;
import com.recyclestudy.member.domain.Email;
import com.recyclestudy.member.domain.Member;
import com.recyclestudy.member.repository.DeviceRepository;
import com.recyclestudy.member.repository.MemberRepository;
import com.recyclestudy.member.service.input.DeviceDeleteInput;
import com.recyclestudy.member.service.input.MemberFindInput;
import com.recyclestudy.member.service.input.MemberNotificationTimeUpdateInput;
import com.recyclestudy.member.service.input.MemberSaveInput;
import com.recyclestudy.member.service.output.MemberFindOutput;
import com.recyclestudy.member.service.output.MemberNotificationTimeFindOutput;
import com.recyclestudy.member.service.output.MemberSaveOutput;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    MemberRepository memberRepository;

    @Mock
    DeviceRepository deviceRepository;

    @Spy
    Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneId.of("UTC"));

    @InjectMocks
    MemberService memberService;

    LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
    }

    @Test
    @DisplayName("저장되지 않은 이메일일 경우 멤버를 저장 후, 새로운 디바이스 id를 저장한다")
    void saveDevice_newMember() {
        // given
        final MemberSaveInput input = MemberSaveInput.from("new@test.com");
        final Member newMember = Member.withoutId(input.email());
        final Device device = Device.withoutId(newMember, DeviceIdentifier.create(), false,
                ActivationExpiredDateTime.create(now));

        given(memberRepository.findByEmail(any(Email.class))).willReturn(Optional.empty());
        given(memberRepository.save(any(Member.class))).willReturn(newMember);
        given(deviceRepository.save(any(Device.class))).willReturn(device);

        // when
        final MemberSaveOutput actual = memberService.saveDevice(input);

        // then
        assertThat(actual.email()).isEqualTo(input.email());
        verify(memberRepository).findByEmail(any(Email.class));
        verify(memberRepository).save(any(Member.class));
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    @DisplayName("이미 저장된 이메일일 경우 기존 멤버로 새로운 디바이스 id를 저장한다")
    void saveDevice_existedMember() {
        // given
        final MemberSaveInput input = MemberSaveInput.from("existed@test.com");
        final Member existedMember = Member.withoutId(input.email());
        final Device device = Device.withoutId(existedMember, DeviceIdentifier.create(), false,
                ActivationExpiredDateTime.create(now));

        given(memberRepository.findByEmail(any(Email.class))).willReturn(Optional.of(existedMember));
        given(deviceRepository.save(any(Device.class))).willReturn(device);

        // when
        final MemberSaveOutput actual = memberService.saveDevice(input);

        // then
        assertThat(actual.email()).isEqualTo(input.email());
        verify(memberRepository).findByEmail(any(Email.class));
        verify(memberRepository, never()).save(any(Member.class));
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    @DisplayName("대상 이메일을 가진 멤버의 디바이스를 모두 조회한다")
    void findAllMemberDevices() {
        // given
        final DeviceIdentifier identifier = DeviceIdentifier.from("device-id");
        final MemberFindInput input = MemberFindInput.from(identifier);
        final Email email = Email.from("existed@test.com");
        final Member existedMember = Member.withoutId(email);
        final Device device = Device.withoutId(existedMember, input.deviceIdentifier(), true,
                ActivationExpiredDateTime.create(now));

        given(memberRepository.findByIdentifier(identifier)).willReturn(Optional.of(existedMember));
        given(deviceRepository.findAllByMemberEmail(email)).willReturn(List.of(device));

        // when
        final MemberFindOutput actual = memberService.findAllMemberDevices(input);

        // then
        assertSoftly(softAssertions -> {
            softAssertions.assertThat(actual.email()).isEqualTo(email);
            softAssertions.assertThat(actual.elements()).hasSize(1);
            softAssertions.assertThat(actual.elements().getFirst().identifier()).isEqualTo(input.deviceIdentifier());
        });
    }

    @Test
    @DisplayName("대상 이메일을 가진 멤버의 디바이스가 없으면 빈 리스트를 리턴한다")
    void findAllMemberDevices_notExistedDevice() {
        // given
        final DeviceIdentifier identifier = DeviceIdentifier.from("device-id");
        final MemberFindInput input = MemberFindInput.from(identifier);
        final Member existedMember = Member.withoutId(Email.from("existed@test.com"));

        given(memberRepository.findByIdentifier(identifier)).willReturn(Optional.of(existedMember));
        given(deviceRepository.findAllByMemberEmail(existedMember.getEmail())).willReturn(List.of());

        // when
        final MemberFindOutput actual = memberService.findAllMemberDevices(input);

        // then
        assertThat(actual.elements()).isEmpty();
    }

    @Test
    @DisplayName("디바이스를 인증할 수 있다")
    void authenticateDevice() {
        // given
        final Email email = Email.from("test@test.com");
        final DeviceIdentifier deviceIdentifier = DeviceIdentifier.from("test");
        final Member member = Member.withoutId(email);
        final Device device = Device.withoutId(member, deviceIdentifier, false, ActivationExpiredDateTime.create(now));

        given(memberRepository.existsByEmail(email)).willReturn(true);
        given(deviceRepository.findByIdentifier(deviceIdentifier)).willReturn(Optional.of(device));

        // when
        memberService.authenticateDevice(email, deviceIdentifier);

        // then
        assertThat(device.isActive()).isTrue();
    }

    @Test
    @DisplayName("소유자가 아닌 이메일로 인증 시도 시 예외를 던진다")
    void authenticateDevice_fail_owner() {
        // given
        final Email email = Email.from("test@test.com");
        final Email otherEmail = Email.from("other@test.com");
        final DeviceIdentifier deviceIdentifier = DeviceIdentifier.from("test");
        final Member member = Member.withoutId(email);
        final Device device = Device.withoutId(member, deviceIdentifier, false, ActivationExpiredDateTime.create(now));

        given(memberRepository.existsByEmail(otherEmail)).willReturn(true);
        given(deviceRepository.findByIdentifier(deviceIdentifier)).willReturn(Optional.of(device));

        // when
        // then
        assertThatThrownBy(() -> memberService.authenticateDevice(otherEmail, deviceIdentifier))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("디바이스 소유자가 아닙니다.");
    }

    @Test
    @DisplayName("존재하지 않는 디바이스로 인증 시도 시 예외를 던진다")
    void authenticateDevice_not_existed_device() {
        // given
        final Email email = Email.from("test@test.com");
        final DeviceIdentifier deviceIdentifier = DeviceIdentifier.from("test");

        given(memberRepository.existsByEmail(email)).willReturn(true);
        given(deviceRepository.findByIdentifier(deviceIdentifier)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> memberService.authenticateDevice(email, deviceIdentifier))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("존재하지 않는 디바이스 식별자입니다: %s".formatted(deviceIdentifier.getValue()));
    }

    @Test
    @DisplayName("이미 인증 된 디바이스에 다시 인증 시도 시 예외를 던진다")
    void authenticateDevice_already_auth() {
        // given
        final Email email = Email.from("test@test.com");
        final Email otherEmail = Email.from("other@test.com");
        final DeviceIdentifier deviceIdentifier = DeviceIdentifier.from("test");
        final Member member = Member.withoutId(email);
        final Device device = Device.withoutId(member, deviceIdentifier, true, ActivationExpiredDateTime.create(now));

        given(memberRepository.existsByEmail(otherEmail)).willReturn(true);
        given(deviceRepository.findByIdentifier(deviceIdentifier)).willReturn(Optional.of(device));

        // when
        // then
        assertThatThrownBy(() -> memberService.authenticateDevice(otherEmail, deviceIdentifier))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("이미 인증된 디바이스입니다");
    }

    @Test
    @DisplayName("디바이스를 삭제할 수 있다")
    void deleteDevice() {
        // given
        final DeviceIdentifier deviceIdentifier = DeviceIdentifier.from("test");
        final DeviceIdentifier targetDeviceIdentifier = DeviceIdentifier.from("target");
        final DeviceDeleteInput input = DeviceDeleteInput.from(deviceIdentifier, targetDeviceIdentifier.getValue());
        final Member member = Member.withoutId(Email.from("test@test.com"));
        final Device targetDevice = Device.withoutId(member, targetDeviceIdentifier, true,
                ActivationExpiredDateTime.create(now));

        given(memberRepository.findByIdentifier(deviceIdentifier)).willReturn(Optional.of(member));
        given(deviceRepository.findByIdentifier(targetDeviceIdentifier)).willReturn(Optional.of(targetDevice));

        // when
        memberService.deleteDevice(input);

        // then
        verify(deviceRepository).delete(targetDevice);
    }

    @Test
    @DisplayName("다른 멤버가 소유한 디바이스를 삭제할 때 예외를 던진다")
    void deleteDevice_notOwner() {
        // given
        final DeviceIdentifier deviceIdentifier = DeviceIdentifier.from("request-device");
        final DeviceIdentifier targetDeviceIdentifier = DeviceIdentifier.from("target-device");
        final DeviceDeleteInput input = DeviceDeleteInput.from(deviceIdentifier, targetDeviceIdentifier.getValue());

        final Member requestMember = Member.withoutId(Email.from("request@test.com"));
        final Member targetMember = Member.withoutId(Email.from("target@test.com"));
        final Device targetDevice = Device.withoutId(targetMember, targetDeviceIdentifier, true,
                ActivationExpiredDateTime.create(now));

        given(memberRepository.findByIdentifier(deviceIdentifier)).willReturn(Optional.of(requestMember));
        given(deviceRepository.findByIdentifier(targetDeviceIdentifier)).willReturn(Optional.of(targetDevice));

        // when
        // then
        assertThatThrownBy(() -> memberService.deleteDevice(input))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("요청 디바이스가 유효하지 않을 때 디바이스 삭제 시 예외를 던진다")
    void deleteDevice_unauthorized() {
        // given
        final DeviceIdentifier deviceIdentifier = DeviceIdentifier.from("request-device");
        final DeviceIdentifier targetDeviceIdentifier = DeviceIdentifier.from("target-device");
        final DeviceDeleteInput input = DeviceDeleteInput.from(deviceIdentifier, targetDeviceIdentifier.getValue());

        given(memberRepository.findByIdentifier(deviceIdentifier)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> memberService.deleteDevice(input))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("디바이스 식별자로 멤버의 알림 시간을 조회한다")
    void findNotificationTime() {
        // given
        final DeviceIdentifier identifier = DeviceIdentifier.from("device-id");
        final Member member = Member.withoutId(Email.from("test@test.com"));
        final LocalTime expectedTime = LocalTime.of(9, 0);
        member.updateNotificationTime(expectedTime);

        given(memberRepository.findByIdentifier(identifier)).willReturn(Optional.of(member));

        // when
        final MemberNotificationTimeFindOutput actual = memberService.findNotificationTime(identifier);

        // then
        assertThat(actual.notificationTime()).isEqualTo(expectedTime);
    }

    @Test
    @DisplayName("알림 시간을 설정하지 않은 멤버 조회 시 null을 반환한다")
    void findNotificationTime_NullNotificationTime() {
        // given
        final DeviceIdentifier identifier = DeviceIdentifier.from("device-id");
        final Member member = Member.withoutId(Email.from("test@test.com"));

        given(memberRepository.findByIdentifier(identifier)).willReturn(Optional.of(member));

        // when
        final MemberNotificationTimeFindOutput actual = memberService.findNotificationTime(identifier);

        // then
        assertThat(actual.notificationTime()).isNull();
    }

    @Test
    @DisplayName("유효하지 않은 디바이스로 알림 시간 조회 시 예외를 던진다")
    void findNotificationTime_UnauthorizedDevice() {
        // given
        final DeviceIdentifier identifier = DeviceIdentifier.from("invalid-id");

        given(memberRepository.findByIdentifier(identifier)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> memberService.findNotificationTime(identifier))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("유효하지 않은 디바이스입니다");
    }

    @Test
    @DisplayName("멤버의 알림 시간을 업데이트할 수 있다")
    void updateNotificationTime() {
        // given
        final DeviceIdentifier identifier = DeviceIdentifier.from("device-id");
        final LocalTime notificationTime = LocalTime.of(9, 0);
        final MemberNotificationTimeUpdateInput input = new MemberNotificationTimeUpdateInput(identifier,
                notificationTime);
        final Member member = Member.withoutId(Email.from("test@test.com"));

        given(memberRepository.findByIdentifier(identifier)).willReturn(Optional.of(member));

        // when
        memberService.updateNotificationTime(input);

        // then
        assertSoftly(softAssertions -> {
            softAssertions.assertThat(member.getNotificationTime()).isNotNull();
            softAssertions.assertThat(member.getNotificationTime()).isEqualTo(notificationTime);
        });
    }

    @Test
    @DisplayName("유효하지 않은 디바이스로 알림 시간 업데이트 시 예외를 던진다")
    void updateNotificationTime_UnauthorizedDevice() {
        // given
        final DeviceIdentifier identifier = DeviceIdentifier.from("invalid-id");
        final LocalTime notificationTime = LocalTime.of(9, 0);
        final MemberNotificationTimeUpdateInput input = new MemberNotificationTimeUpdateInput(identifier,
                notificationTime);

        given(memberRepository.findByIdentifier(identifier)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> memberService.updateNotificationTime(input))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("유효하지 않은 디바이스입니다");
    }
}
