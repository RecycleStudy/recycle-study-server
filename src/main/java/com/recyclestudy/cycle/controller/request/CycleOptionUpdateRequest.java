package com.recyclestudy.cycle.controller.request;

import com.recyclestudy.cycle.service.input.CycleOptionUpdateInput;
import java.util.List;

public record CycleOptionUpdateRequest(String title, List<String> durations) {

    public CycleOptionUpdateInput toInput() {
        return CycleOptionUpdateInput.of(title, durations);
    }
}
