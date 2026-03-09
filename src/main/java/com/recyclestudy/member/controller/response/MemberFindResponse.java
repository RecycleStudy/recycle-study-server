package com.recyclestudy.member.controller.response;

import com.recyclestudy.member.service.output.MemberFindOutput;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

public record MemberFindResponse(String email, List<MemberFindElement> devices) {

    public static MemberFindResponse from(final MemberFindOutput output) {
        final List<MemberFindElement> memberFindElements = output.elements().stream()
                .map(outputElement -> new MemberFindElement(outputElement.identifier().getValue(),
                        outputElement.createdAt().toInstant(ZoneOffset.UTC)))
                .toList();
        return new MemberFindResponse(output.email().getValue(), memberFindElements);
    }

    private record MemberFindElement(String identifier, Instant createdAt) {
    }
}
