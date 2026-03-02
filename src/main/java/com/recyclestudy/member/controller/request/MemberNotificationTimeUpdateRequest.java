package com.recyclestudy.member.controller.request;

import com.recyclestudy.member.domain.DeviceIdentifier;
import com.recyclestudy.member.service.input.MemberNotificationTimeUpdateInput;
import java.time.LocalTime;

public record MemberNotificationTimeUpdateRequest(LocalTime notificationTime) {

    public MemberNotificationTimeUpdateInput toInput(final DeviceIdentifier identifier) {
        return MemberNotificationTimeUpdateInput.of(identifier, notificationTime);
    }
}
