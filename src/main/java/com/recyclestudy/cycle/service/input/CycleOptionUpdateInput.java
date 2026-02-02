package com.recyclestudy.cycle.service.input;

import com.recyclestudy.cycle.domain.CycleOptionTitle;
import java.time.Duration;
import java.util.List;

public record CycleOptionUpdateInput(CycleOptionTitle title, List<Duration> durations) {
}
