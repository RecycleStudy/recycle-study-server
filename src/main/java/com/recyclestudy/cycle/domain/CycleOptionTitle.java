package com.recyclestudy.cycle.domain;

import com.recyclestudy.common.NullValidator;
import com.recyclestudy.exception.BadRequestException;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldNameConstants(level = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode
public class CycleOptionTitle {

    private static final int MAX_LENGTH = 30;

    private String value;

    public static CycleOptionTitle from(final String value) {
        validateNotNull(value);
        if (value.isBlank() || value.trim().length() > MAX_LENGTH) {
            throw new BadRequestException("유효하지 않은 제목의 길이입니다.(1 이상 30 이하)");
        }

        return new CycleOptionTitle(value);
    }

    private static void validateNotNull(final String value) {
        NullValidator.builder()
                .add(Fields.value, value)
                .validate();
    }
}
