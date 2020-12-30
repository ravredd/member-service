package com.sams.merch.engenable.memberservice.repository;

import java.util.List;

import com.sams.merch.engenable.memberservice.domain.Member;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends CrudRepository<Member, Long> {
    @Override
    List<Member> findAll();
}
