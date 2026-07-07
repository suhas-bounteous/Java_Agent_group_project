package com.accolie.lib.lib.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.accolie.lib.lib.repository.*;
import com.accolie.lib.lib.entity.*;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BorrowService {

    private final BorrowRecordRepository borrowRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;

    public BorrowRecord borrow(Long bookId, Long memberId) {

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        BorrowRecord record = new BorrowRecord();
        record.setBook(book);
        record.setMember(member);
        record.setBorrowTime(LocalDateTime.now());

        return borrowRepository.save(record);
    }

    public BorrowRecord returnBook(Long borrowId) {

        BorrowRecord record = borrowRepository.findById(borrowId)
                .orElseThrow(() -> new RuntimeException("Borrow record not found"));

        record.setReturnTime(LocalDateTime.now());

        return borrowRepository.save(record);
    }

    public List<BorrowRecord> getAll() {
        return borrowRepository.findAll();
    }

    public void delete(Long id) {
        if (!borrowRepository.existsById(id)) {
            throw new RuntimeException("Borrow record not found with id: " + id);
        }
        borrowRepository.deleteById(id);
    }
}
