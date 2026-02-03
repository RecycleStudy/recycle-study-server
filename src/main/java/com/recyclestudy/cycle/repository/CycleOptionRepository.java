package com.recyclestudy.cycle.repository;

import com.recyclestudy.cycle.domain.CycleOption;
import com.recyclestudy.member.domain.Member;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CycleOptionRepository extends JpaRepository<CycleOption, Long> {

    @Query("""
            select co
            from CycleOption co
            join fetch co.member m
            join fetch co.durations.values
            where m = :member
            """)
    List<CycleOption> findAllByMember(@Param("member") Member member);

    @Query("""
            select co
            from CycleOption co
            join fetch co.durations.values
            WHERE co.id = :id
            """)
    Optional<CycleOption> findByIdWithDurations(@Param("id") Long id);

    long countByMember(Member member);
}
