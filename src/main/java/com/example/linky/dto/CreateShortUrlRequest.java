package com.example.linky.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateShortUrlRequest(
        @NotBlank(message = "longUrl is required")
        @Size(max = 2048, message = "longUrl must be at most 2048 characters")
        @Pattern(regexp = "^https?://\\S+$", message = "longUrl must be a valid http or https URL")
        String longUrl
) {
}
