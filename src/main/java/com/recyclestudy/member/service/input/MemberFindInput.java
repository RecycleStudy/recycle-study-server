package com.recyclestudy.member.service.input;

import com.recyclestudy.member.domain.DeviceIdentifier;

public record MemberFindInput(DeviceIdentifier deviceIdentifier) {

    public static MemberFindInput from(final DeviceIdentifier identifier) {
        return new MemberFindInput(identifier);
    }
}
