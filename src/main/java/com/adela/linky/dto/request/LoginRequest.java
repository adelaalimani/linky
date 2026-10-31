package com.adela.linky.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Schema(example = "adela@gmail.com")
        String email,

        @NotBlank(message = "Password is required")
        @Schema(example = "Password")
        String password
) {
}
