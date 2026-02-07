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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;
    private final DeviceRepository deviceRepository;
    private final Clock clock;

    @Transactional
    public MemberSaveOutput saveDevice(final MemberSaveInput input) {
        final Member member = saveNewMember(input.email());
        final DeviceIdentifier deviceIdentifier = DeviceIdentifier.create();

        final ActivationExpiredDateTime activationExpiredDateTime
                = ActivationExpiredDateTime.create(LocalDateTime.now(clock));

        final Device notSavedDevice = Device.withoutId(member, deviceIdentifier, false, activationExpiredDateTime);
        final Device device = deviceRepository.save(notSavedDevice);
        log.info("[DEVICE_SAVED] 디바이스 저장 성공: {}", device.getIdentifier());

        return MemberSaveOutput.from(device);
    }

    @Transactional(readOnly = true)
    public MemberFindOutput findAllMemberDevices(final MemberFindInput input) {
        final List<Device> devices = deviceRepository.findAllByMemberEmail(input.email());
        return MemberFindOutput.of(input.email(), devices);
    }

    @Transactional(readOnly = true)
    public MemberNotificationTimeFindOutput findNotificationTime(final DeviceIdentifier identifier) {
        final Member member = memberRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new UnauthorizedException("유효하지 않은 디바이스입니다"));
        return MemberNotificationTimeFindOutput.from(member.getNotificationTime());
    }

    @Transactional
    public void authenticateDevice(final Email email, final DeviceIdentifier deviceIdentifier) {
        checkExistedMember(email);

        final Device device = deviceRepository.findByIdentifier(deviceIdentifier)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 디바이스 아이디입니다: %s"
                        .formatted(deviceIdentifier.getValue())));

        if (device.isActive()) {
            throw new BadRequestException("이미 인증되었습니다");
        }

        device.verifyOwner(email);
        device.activate(LocalDateTime.now(clock));
        log.info("[DEVICE_AUTH] 디바이스 인증 성공: email={}, device={}", email.toMaskedValue(), deviceIdentifier);
    }

    @Transactional
    public void deleteDevice(final DeviceDeleteInput input) {
        deviceRepository.deleteByIdentifier(input.targetDeviceIdentifier());
        log.info("[DEVICE_DELETED] 디바이스 삭제 성공: {}", input.targetDeviceIdentifier());
    }

    @Transactional
    public void updateNotificationTime(final MemberNotificationTimeUpdateInput input) {
        final Member member = memberRepository.findByIdentifier(input.identifier())
                .orElseThrow(() -> new UnauthorizedException("유효하지 않은 디바이스입니다"));
        final LocalTime previousNotificationTime = member.getNotificationTime();

        member.updateNotificationTime(input.notificationTime());
        log.info("[MEMBER_NOTI_TIME_UPDATED] 멤버 알림 시간 변경: memberId={}, from={}, to={}",
                member.getId(), previousNotificationTime, input.notificationTime());
    }

    private Member saveNewMember(final Email email) {
        final Optional<Member> memberOptional = memberRepository.findByEmail(email);

        if (memberOptional.isPresent()) {
            return memberOptional.get();
        }

        log.info("[MEMBER_REGISTER_START] 신규 유저 이메일 등록 시작: {}", email.toMaskedValue());
        final Member notSavedMember = Member.withoutId(email);
        final Member savedMember = memberRepository.save(notSavedMember);
        log.info("[MEMBER_SAVED] 신규 유저 이메일 등록 성공: {}", email.toMaskedValue());
        return savedMember;
    }

    private void checkExistedMember(final Email email) {
        if (!memberRepository.existsByEmail(email)) {
            throw new NotFoundException("존재하지 않는 멤버입니다: %s".formatted(email.getValue()));
        }
    }
}
