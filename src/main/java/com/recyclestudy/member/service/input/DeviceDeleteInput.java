package com.recyclestudy.member.service.input;

import com.recyclestudy.member.domain.DeviceIdentifier;

public record DeviceDeleteInput(DeviceIdentifier deviceIdentifier, DeviceIdentifier targetDeviceIdentifier) {

    public static DeviceDeleteInput from(
            final DeviceIdentifier identifier,
            final String targetIdentifier
    ) {
        final DeviceIdentifier targetDeviceIdentifier = DeviceIdentifier.from(targetIdentifier);
        return new DeviceDeleteInput(identifier, targetDeviceIdentifier);
    }
}
