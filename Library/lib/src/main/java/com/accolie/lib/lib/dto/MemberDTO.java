package com.accolie.lib.lib.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class MemberDTO {

    private Long id;

    @NotBlank
    private String name;

    @Email
    private String email;

    @Positive
    @Min(1)
    private int age;
}
