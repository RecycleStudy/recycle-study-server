package com.recyclestudy.member.service.input;

import com.recyclestudy.member.domain.DeviceIdentifier;
import java.time.LocalTime;

public record MemberNotificationTimeUpdateInput(DeviceIdentifier identifier, LocalTime notificationTime) {

    public static MemberNotificationTimeUpdateInput of(
            final DeviceIdentifier identifier,
            final LocalTime notificationTime
    ) {
        return new MemberNotificationTimeUpdateInput(identifier, notificationTime);
    }
}
