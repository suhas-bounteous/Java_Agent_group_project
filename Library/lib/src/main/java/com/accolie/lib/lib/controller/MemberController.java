package com.accolie.lib.lib.controller;

import com.accolie.lib.lib.dto.MemberDTO;
import com.accolie.lib.lib.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    public ResponseEntity<MemberDTO> create(@Valid @RequestBody MemberDTO dto) {
        return ResponseEntity.ok(memberService.create(dto));
    }

    @GetMapping
    public ResponseEntity<List<MemberDTO>> getAll() {
        return ResponseEntity.ok(memberService.getAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        memberService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
