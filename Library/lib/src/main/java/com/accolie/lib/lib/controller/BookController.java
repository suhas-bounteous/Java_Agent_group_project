package com.accolie.lib.lib.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;

import com.accolie.lib.lib.dto.BookDTO;
import com.accolie.lib.lib.service.BookService;

import java.util.List;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    public ResponseEntity<BookDTO> create(@Valid @RequestBody BookDTO dto) {
        return ResponseEntity.ok(bookService.create(dto));
    }

    @GetMapping
    public ResponseEntity<List<BookDTO>> getAll() {
        return ResponseEntity.ok(bookService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // DB Monitor - Error API: runs a native query against a non-existent table, causing a real DB error
    @GetMapping("/monitor/error-test")
    public ResponseEntity<List<BookDTO>> errorTest() {
        return ResponseEntity.ok(bookService.errorQuery());
    }

    // DB Monitor - Slow Query API: uses pg_sleep(5) so the DB itself runs slow — visible in DB monitor
    @GetMapping("/monitor/slow-query")
    public ResponseEntity<List<BookDTO>> slowQuery() {
        return ResponseEntity.ok(bookService.slowQuery());
    }
}
