package com.recyclestudy.cycle.controller.request;

import com.recyclestudy.cycle.domain.CycleOptionTitle;
import com.recyclestudy.cycle.service.input.CycleOptionSaveInput;
import com.recyclestudy.cycle.util.CycleDurationParser;
import java.time.Duration;
import java.util.List;

public record CycleOptionSaveRequest(String title, List<String> durations) {

    public CycleOptionSaveInput toInput() {
        final CycleOptionTitle cycleOptionTitle = CycleOptionTitle.from(title);
        final List<Duration> cycleOptionDurations = CycleDurationParser.parseList(durations);

        return new CycleOptionSaveInput(cycleOptionTitle, cycleOptionDurations);
    }
}
