package com.recyclestudy.member.controller;

import com.recyclestudy.common.annotation.AuthDevice;
import com.recyclestudy.member.controller.request.DeviceDeleteRequest;
import com.recyclestudy.member.domain.DeviceIdentifier;
import com.recyclestudy.member.domain.Email;
import com.recyclestudy.member.service.MemberService;
import com.recyclestudy.member.service.input.DeviceDeleteInput;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/api/v1/device")
@RequiredArgsConstructor
public class DeviceController {

    private final MemberService memberService;

    @GetMapping("/auth")
    public String authenticateDevice(
            @RequestParam("email") String email,
            @RequestParam("identifier") String deviceIdentifier
    ) {
        memberService.authenticateDevice(Email.from(email), DeviceIdentifier.from(deviceIdentifier));
        return "auth_success";
    }

    @DeleteMapping
    @ResponseBody
    public ResponseEntity<Void> deleteDevice(
            @AuthDevice final DeviceIdentifier identifier,
            @RequestBody final DeviceDeleteRequest request
    ) {
        final DeviceDeleteInput input = DeviceDeleteInput.from(request.email(), identifier,
                request.targetDeviceIdentifier());
        memberService.deleteDevice(input);
        return ResponseEntity.noContent().build();
    }

    private String getResolvedIdentifier(final String identifier, final String headerIdentifier) {
        if (headerIdentifier == null) {
            return identifier;
        }
        return headerIdentifier;
    }
}
