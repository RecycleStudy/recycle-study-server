package com.recyclestudy.cycle.domain;

import java.time.Duration;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DefaultCycleOption {

    EBBINGHAUS("에빙하우스 망각곡선", List.of(
            Duration.ofMinutes(10),
            Duration.ofHours(1),
            Duration.ofDays(1),
            Duration.ofDays(7),
            Duration.ofDays(30)
    )),
    ;

    private final String title;
    private final List<Duration> durations;

    public static List<DefaultCycleOption> getAll() {
        return List.of(values());
    }
}
