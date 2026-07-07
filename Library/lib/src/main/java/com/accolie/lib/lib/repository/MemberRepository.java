package com.accolie.lib.lib.repository;

import com.accolie.lib.lib.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
