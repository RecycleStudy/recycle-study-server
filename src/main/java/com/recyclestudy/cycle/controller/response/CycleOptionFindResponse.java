package com.recyclestudy.cycle.controller.response;

import com.recyclestudy.cycle.service.output.CycleOptionFindOutput;
import java.util.List;

public record CycleOptionFindResponse(
        List<DefaultOptionElement> defaultOptions,
        List<CustomOptionElement> customOptions
) {

    public static CycleOptionFindResponse from(final CycleOptionFindOutput output) {
        final List<DefaultOptionElement> defaultElements = output.defaultOptions().stream()
                .map(option -> new DefaultOptionElement(
                        option.code(),
                        option.title(),
                        option.durations().stream()
                                .map(Object::toString)
                                .toList()
                ))
                .toList();

        final List<CustomOptionElement> customElements = output.customOptions().stream()
                .map(option -> new CustomOptionElement(
                        option.id(),
                        option.title(),
                        option.durations().stream()
                                .map(Object::toString)
                                .toList()
                ))
                .toList();

        return new CycleOptionFindResponse(defaultElements, customElements);
    }

    private record DefaultOptionElement(String code, String title, List<String> durations) {
    }

    private record CustomOptionElement(Long id, String title, List<String> durations) {
    }
}
