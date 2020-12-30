package com.sams.merch.engenable.memberservice.service;

import java.util.List;
import java.util.Optional;

import com.sams.merch.engenable.memberservice.domain.Member;

public interface MemberService {
    List<Member> findAll();

    Optional<Member> findById(Long id);

    Member save(Member member);

    void deleteById(Long id);
}
