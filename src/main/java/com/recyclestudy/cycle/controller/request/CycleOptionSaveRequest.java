package com.recyclestudy.cycle.controller.request;

import com.recyclestudy.cycle.service.input.CycleOptionSaveInput;
import java.util.List;

public record CycleOptionSaveRequest(String title, List<String> durations) {

    public CycleOptionSaveInput toInput() {
        return CycleOptionSaveInput.of(title, durations);
    }
}
