package com.accolie.lib.lib.service;

import com.accolie.lib.lib.dto.MemberDTO;
import com.accolie.lib.lib.entity.Member;
import com.accolie.lib.lib.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberDTO create(MemberDTO dto) {
        try {
            Member member = new Member();
            member.setName(dto.getName());
            member.setEmail(dto.getEmail());
            member.setAge(dto.getAge());
            return mapToDTO(memberRepository.save(member));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("A member with this email already exists.");
        }
    }

    public List<MemberDTO> getAll() {
        return memberRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public void delete(Long id) {
        if (!memberRepository.existsById(id)) {
            throw new RuntimeException("Member not found with id: " + id);
        }
        memberRepository.deleteById(id);
    }

    private MemberDTO mapToDTO(Member member) {
        MemberDTO dto = new MemberDTO();
        dto.setId(member.getId());
        dto.setName(member.getName());
        dto.setEmail(member.getEmail());
        dto.setAge(member.getAge());
        return dto;
    }
}
