package com.accolie.lib.lib.controller;

import com.accolie.lib.lib.entity.BorrowRecord;
import com.accolie.lib.lib.service.BorrowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/borrow")
public class BorrowController {

    private final BorrowService borrowService;

    public BorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @PostMapping
    public ResponseEntity<BorrowRecord> borrow(
            @RequestParam Long bookId,
            @RequestParam Long memberId) {
        return ResponseEntity.ok(borrowService.borrow(bookId, memberId));
    }

    @PutMapping("/return/{id}")
    public ResponseEntity<BorrowRecord> returnBook(@PathVariable Long id) {
        return ResponseEntity.ok(borrowService.returnBook(id));
    }

    @GetMapping
    public ResponseEntity<List<BorrowRecord>> getAll() {
        return ResponseEntity.ok(borrowService.getAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        borrowService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
