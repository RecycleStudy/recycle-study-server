package com.recyclestudy.member.service.output;

import com.recyclestudy.member.domain.Device;
import com.recyclestudy.member.domain.DeviceIdentifier;
import com.recyclestudy.member.domain.Email;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record MemberFindOutput(Email email, LocalTime notificationTime, List<MemberFindElement> elements) {

    public static MemberFindOutput of(
            final Email email,
            final LocalTime notificationTime,
            final List<Device> devices
    ) {
        final List<MemberFindElement> memberFindElements = devices.stream()
                .map(device -> new MemberFindElement(device.getIdentifier(), device.getCreatedAt()))
                .toList();
        return new MemberFindOutput(email, notificationTime, memberFindElements);
    }

    public record MemberFindElement(DeviceIdentifier identifier, LocalDateTime createdAt) {
    }
}
