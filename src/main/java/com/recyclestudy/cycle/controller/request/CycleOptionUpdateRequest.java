package com.recyclestudy.cycle.controller.request;

import com.recyclestudy.cycle.domain.CycleOptionTitle;
import com.recyclestudy.cycle.service.input.CycleOptionUpdateInput;
import com.recyclestudy.cycle.util.CycleDurationParser;
import java.time.Duration;
import java.util.List;

public record CycleOptionUpdateRequest(String title, List<String> durations) {

    public CycleOptionUpdateInput toInput() {
        final CycleOptionTitle cycleOptionTitle = CycleOptionTitle.from(title);
        final List<Duration> cycleOptionDurations = CycleDurationParser.parseList(durations);

        return new CycleOptionUpdateInput(cycleOptionTitle, cycleOptionDurations);
    }
}
