package com.recyclestudy.cycle.controller.response;

import com.recyclestudy.cycle.service.output.CycleOptionFindOutput;
import java.util.List;

public record CycleOptionFindResponse(List<CycleOptionElement> options) {

    public static CycleOptionFindResponse from(final CycleOptionFindOutput output) {
        final List<CycleOptionElement> optionElements = output.options().stream()
                .map(option -> new CycleOptionElement(
                        option.id(),
                        option.title().getValue(),
                        option.durations().stream()
                                .map(Object::toString)
                                .toList()
                ))
                .toList();
        return new CycleOptionFindResponse(optionElements);
    }

    public record CycleOptionElement(Long id, String title, List<String> durations) {
    }
}
