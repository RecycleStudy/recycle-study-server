package com.recyclestudy.member.service.input;

import com.recyclestudy.member.domain.DeviceIdentifier;
import com.recyclestudy.member.domain.Email;

public record DeviceDeleteInput(Email email, DeviceIdentifier deviceIdentifier,
                                DeviceIdentifier targetDeviceIdentifier) {

    public static DeviceDeleteInput from(
            final String emailValue,
            final DeviceIdentifier identifier,
            final String targetIdentifier
    ) {
        final Email email = Email.from(emailValue);
        final DeviceIdentifier targetDeviceIdentifier = DeviceIdentifier.from(targetIdentifier);
        return new DeviceDeleteInput(email, identifier, targetDeviceIdentifier);
    }
}
