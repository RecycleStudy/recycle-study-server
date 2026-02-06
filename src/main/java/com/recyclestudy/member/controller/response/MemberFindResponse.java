package com.recyclestudy.member.controller.response;

import com.recyclestudy.member.service.output.MemberFindOutput;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record MemberFindResponse(String email, LocalTime notificationTime, List<MemberFindElement> devices) {

    public static MemberFindResponse from(final MemberFindOutput output) {
        final List<MemberFindElement> memberFindElements = output.elements().stream()
                .map(outputElement -> new MemberFindElement(outputElement.identifier().getValue(),
                        outputElement.createdAt()))
                .toList();
        return new MemberFindResponse(output.email().getValue(), output.notificationTime(), memberFindElements);
    }

    private record MemberFindElement(String identifier, LocalDateTime createdAt) {
    }
}
