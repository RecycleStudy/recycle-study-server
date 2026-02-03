package com.recyclestudy.cycle.controller;

import com.recyclestudy.common.annotation.AuthDevice;
import com.recyclestudy.cycle.controller.request.CycleOptionSaveRequest;
import com.recyclestudy.cycle.controller.request.CycleOptionUpdateRequest;
import com.recyclestudy.cycle.controller.response.CycleOptionFindResponse;
import com.recyclestudy.cycle.controller.response.CycleOptionSaveResponse;
import com.recyclestudy.cycle.service.CycleOptionService;
import com.recyclestudy.cycle.service.input.CycleOptionSaveInput;
import com.recyclestudy.cycle.service.input.CycleOptionUpdateInput;
import com.recyclestudy.cycle.service.output.CycleOptionFindOutput;
import com.recyclestudy.cycle.service.output.CycleOptionSaveOutput;
import com.recyclestudy.member.domain.DeviceIdentifier;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping
    public ResponseEntity<CycleOptionSaveResponse> saveCycleOption(
            @AuthDevice final DeviceIdentifier identifier,
            @RequestBody final CycleOptionSaveRequest request
    ) {
        final CycleOptionSaveInput input = request.toInput();
        final CycleOptionSaveOutput output = cycleOptionService.saveCycleOption(identifier, input);
        return ResponseEntity.created(URI.create("/api/v1/cycles/custom/" + output.id()))
                .body(CycleOptionSaveResponse.from(output));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CycleOptionSaveResponse> updateCycleOption(
            @AuthDevice final DeviceIdentifier identifier,
            @PathVariable("id") final Long cycleOptionId,
            @RequestBody final CycleOptionUpdateRequest request
    ) {
        final CycleOptionUpdateInput input = request.toInput();
        final CycleOptionSaveOutput output = cycleOptionService.updateCycleOption(identifier, cycleOptionId, input);
        return ResponseEntity.ok(CycleOptionSaveResponse.from(output));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCycleOption(
            @AuthDevice final DeviceIdentifier identifier,
            @PathVariable("id") final Long cycleOptionId
    ) {
        cycleOptionService.deleteCycleOption(identifier, cycleOptionId);
        return ResponseEntity.noContent().build();
    }
}
