package com.recyclestudy.cycle.domain.selection;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DefaultCycleSelection.class, name = "DEFAULT"),
        @JsonSubTypes.Type(value = CustomCycleSelection.class, name = "CUSTOM")
})
public sealed interface CycleSelection permits DefaultCycleSelection, CustomCycleSelection {
}
