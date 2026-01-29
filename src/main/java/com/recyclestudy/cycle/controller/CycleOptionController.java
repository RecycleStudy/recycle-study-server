package com.recyclestudy.cycle.controller;

import com.recyclestudy.common.annotation.AuthDevice;
import com.recyclestudy.cycle.controller.response.CycleOptionFindResponse;
import com.recyclestudy.cycle.service.CycleOptionService;
import com.recyclestudy.cycle.service.output.CycleOptionFindOutput;
import com.recyclestudy.member.domain.DeviceIdentifier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cycles/custom")
@RequiredArgsConstructor
public class CycleOptionController {

    private final CycleOptionService cycleOptionService;

    @GetMapping
    public ResponseEntity<CycleOptionFindResponse> findAllCycleOptions(@AuthDevice DeviceIdentifier identifier) {
        CycleOptionFindOutput output = cycleOptionService.findAllCycleOptions(identifier);
        CycleOptionFindResponse response = CycleOptionFindResponse.from(output);
        return ResponseEntity.ok(response);
    }
}
