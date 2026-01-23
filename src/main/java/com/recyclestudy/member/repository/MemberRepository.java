package com.recyclestudy.member.repository;

import com.recyclestudy.member.domain.DeviceIdentifier;
import com.recyclestudy.member.domain.Email;
import com.recyclestudy.member.domain.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(Email email);

    boolean existsByEmail(Email email);

    @Query(value = """
            select m from Member m
            join Device d on m.id = d.member.id
            where d.identifier = :identifier
            """)
    Optional<Member> findByIdentifier(@Param("identifier") DeviceIdentifier identifier);
}
