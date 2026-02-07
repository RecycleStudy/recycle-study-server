package com.recyclestudy.member.controller.response;

import com.recyclestudy.member.service.output.MemberNotificationTimeFindOutput;
import java.time.LocalTime;

public record MemberNotificationTimeFindResponse(LocalTime notificationTime) {
    public static MemberNotificationTimeFindResponse from(final MemberNotificationTimeFindOutput output) {
        return new MemberNotificationTimeFindResponse(output.notificationTime());
    }
}
