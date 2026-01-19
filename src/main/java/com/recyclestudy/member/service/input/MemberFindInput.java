package com.recyclestudy.member.service.input;

import com.recyclestudy.member.domain.DeviceIdentifier;
import com.recyclestudy.member.domain.Email;

public record MemberFindInput(Email email, DeviceIdentifier deviceIdentifier) {

    public static MemberFindInput from(final String emailValue, final DeviceIdentifier identifier) {
        final Email email = Email.from(emailValue);
        return new MemberFindInput(email, identifier);
    }
}
