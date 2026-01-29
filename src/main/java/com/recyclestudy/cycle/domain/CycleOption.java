package com.recyclestudy.cycle.domain;

import com.recyclestudy.common.BaseEntity;
import com.recyclestudy.common.NullValidator;
import com.recyclestudy.member.domain.Member;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(name = "cycle_option")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldNameConstants(level = AccessLevel.PRIVATE)
@Getter
public class CycleOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "title", nullable = false))
    private CycleOptionTitle title;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "option_type", nullable = false)
    private OptionType optionType;

    @OneToMany(mappedBy = "id.cycleOption", cascade = CascadeType.ALL, orphanRemoval = true)
    @Column(name = "durations", nullable = false)
    private List<CycleOptionDuration> durations;

    public static CycleOption withoutId(
            final Member member,
            final CycleOptionTitle title,
            final OptionType optionType
    ) {
        validateNotNull(member, title, optionType);
        return new CycleOption(member, title, optionType, new ArrayList<>());
    }

    private static void validateNotNull(
            final Member member,
            final CycleOptionTitle title,
            final OptionType optionType
    ) {
        NullValidator.builder()
                .add(Fields.member, member)
                .add(Fields.title, title)
                .add(Fields.optionType, optionType)
                .validate();
    }
}
