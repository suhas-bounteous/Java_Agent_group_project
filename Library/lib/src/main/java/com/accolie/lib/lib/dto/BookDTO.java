package com.accolie.lib.lib.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class BookDTO {

    private Long id;

    @NotBlank
    private String bookName;

    @NotBlank
    private String genre;

    @NotBlank
    private String author;
}
