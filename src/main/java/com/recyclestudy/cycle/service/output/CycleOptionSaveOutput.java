package com.recyclestudy.cycle.service.output;

import com.recyclestudy.cycle.domain.CycleOption;
import com.recyclestudy.cycle.domain.CycleOptionDuration;
import com.recyclestudy.cycle.domain.CycleOptionTitle;
import java.time.Duration;
import java.util.List;

public record CycleOptionSaveOutput(Long id, CycleOptionTitle title, List<Duration> durations) {

    public static CycleOptionSaveOutput from(final CycleOption cycleOption) {
        return new CycleOptionSaveOutput(
                cycleOption.getId(),
                cycleOption.getTitle(),
                cycleOption.getDurations().stream()
                        .map(CycleOptionDuration::getId)
                        .map(id -> id.getDuration()) // Access duration from CycleOptionDurationId
                        .toList()
        );
    }
}
