package com.accolie.lib.lib.service;

import com.accolie.lib.lib.dto.BookDTO;
import com.accolie.lib.lib.entity.Book;
import com.accolie.lib.lib.repository.BookRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    public BookDTO create(BookDTO dto) {
        try {
            Book book = new Book();
            book.setBookName(dto.getBookName());
            book.setGenre(dto.getGenre());
            book.setAuthor(dto.getAuthor());
            return mapToDTO(bookRepository.save(book));
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("A book with this name already exists.");
        }
    }

    public List<BookDTO> getAll() {
        return bookRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<BookDTO> slowQuery() {
        return bookRepository.slowQuery()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<BookDTO> errorQuery() {
        return bookRepository.errorQuery()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public BookDTO getById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));

        return mapToDTO(book);
    }

    public void delete(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new RuntimeException("Book not found with id: " + id);
        }
        bookRepository.deleteById(id);
    }

    private BookDTO mapToDTO(Book book) {
        BookDTO dto = new BookDTO();
        dto.setId(book.getId());
        dto.setBookName(book.getBookName());
        dto.setGenre(book.getGenre());
        dto.setAuthor(book.getAuthor());
        return dto;
    }
}
