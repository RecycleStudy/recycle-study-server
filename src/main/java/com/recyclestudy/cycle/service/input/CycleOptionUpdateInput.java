package com.recyclestudy.cycle.service.input;

import com.recyclestudy.cycle.domain.CycleOptionTitle;
import java.time.Duration;
import java.util.List;

public record CycleOptionUpdateInput(CycleOptionTitle title, List<Duration> durations) {

    public static CycleOptionUpdateInput of(final String title, final List<String> durations) {
        final CycleOptionTitle cycleOptionTitle = CycleOptionTitle.from(title);
        final List<Duration> cycleOptionDurations = durations.stream()
                .map(Duration::parse)
                .toList();
        return new CycleOptionUpdateInput(cycleOptionTitle, cycleOptionDurations);
    }
}
