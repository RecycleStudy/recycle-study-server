package com.recyclestudy.cycle.util;

import com.recyclestudy.exception.BadRequestException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class CycleDurationParser {

    private CycleDurationParser() {
    }

    public static List<Duration> parseList(final List<String> formats) {
        return formats.stream()
                .map(CycleDurationParser::parse)
                .toList();
    }

    private static Duration parse(final String format) {
        try {
            return Duration.parse(format);
        } catch (final DateTimeParseException e) {
            throw new BadRequestException("잘못된 주기 형식입니다: %s".formatted(format));
        }
    }
}
