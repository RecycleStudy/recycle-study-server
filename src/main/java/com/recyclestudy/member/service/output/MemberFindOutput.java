package com.recyclestudy.member.service.output;

import com.recyclestudy.member.domain.Device;
import com.recyclestudy.member.domain.DeviceIdentifier;
import com.recyclestudy.member.domain.Email;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

public record MemberFindOutput(Email email, List<MemberFindElement> elements) {

    public static MemberFindOutput of(
            final Email email,
            final List<Device> devices
    ) {
        final List<MemberFindElement> memberFindElements = devices.stream()
                .map(device -> new MemberFindElement(device.getIdentifier(),
                        device.getCreatedAt() != null ? device.getCreatedAt().toInstant(ZoneOffset.UTC) : null))
                .toList();
        return new MemberFindOutput(email, memberFindElements);
    }

    public record MemberFindElement(DeviceIdentifier identifier, Instant createdAt) {
    }
}
