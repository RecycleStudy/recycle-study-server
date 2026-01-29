package com.recyclestudy.cycle.controller.response;

import com.recyclestudy.cycle.service.output.CycleOptionSaveOutput;
import java.util.List;

public record CycleOptionSaveResponse(Long id, String title, List<String> durations) {

    public static CycleOptionSaveResponse from(final CycleOptionSaveOutput output) {
        return new CycleOptionSaveResponse(
                output.id(),
                output.title().getValue(),
                output.durations().stream()
                        .map(Object::toString)
                        .toList()
        );

    }
}
