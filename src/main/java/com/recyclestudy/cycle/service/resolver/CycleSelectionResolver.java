package com.recyclestudy.cycle.service.resolver;

import com.recyclestudy.cycle.domain.selection.CycleSelection;
import java.time.Duration;
import java.util.List;

public interface CycleSelectionResolver<T extends CycleSelection> {

    Class<T> getSupportedType();

    List<Duration> resolve(T selection);
}
