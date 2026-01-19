package com.recyclestudy.member.controller;

import com.recyclestudy.common.annotation.AuthDevice;
import com.recyclestudy.email.DeviceAuthEmailSender;
import com.recyclestudy.member.controller.request.MemberSaveRequest;
import com.recyclestudy.member.controller.response.MemberFindResponse;
import com.recyclestudy.member.controller.response.MemberSaveResponse;
import com.recyclestudy.member.domain.DeviceIdentifier;
import com.recyclestudy.member.service.MemberService;
import com.recyclestudy.member.service.input.MemberFindInput;
import com.recyclestudy.member.service.input.MemberSaveInput;
import com.recyclestudy.member.service.output.MemberFindOutput;
import com.recyclestudy.member.service.output.MemberSaveOutput;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final DeviceAuthEmailSender deviceAuthEmailSender;

    @PostMapping
    public ResponseEntity<MemberSaveResponse> saveMember(@RequestBody final MemberSaveRequest request) {
        final MemberSaveInput input = request.toInput();
        final MemberSaveOutput output = memberService.saveDevice(input);

        deviceAuthEmailSender.sendDeviceAuthMail(output.email(), output.identifier());

        final MemberSaveResponse response = MemberSaveResponse.from(output);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<MemberFindResponse> findAllMemberDevices(
            @RequestParam(name = "email") final String email,
            @AuthDevice final DeviceIdentifier identifier
    ) {
        final MemberFindInput input = MemberFindInput.from(email, identifier);
        final MemberFindOutput output = memberService.findAllMemberDevices(input);
        final MemberFindResponse response = MemberFindResponse.from(output);
        return ResponseEntity.ok(response);
    }
}
