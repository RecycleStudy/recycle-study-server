package com.recyclestudy.member.service.output;

import java.time.LocalTime;

public record MemberNotificationTimeFindOutput(LocalTime notificationTime) {
    public static MemberNotificationTimeFindOutput from(final LocalTime notificationTime) {
        return new MemberNotificationTimeFindOutput(notificationTime);
    }
}
