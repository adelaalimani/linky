package com.example.linky.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateShortUrlRequest(
        @Schema(
                description = "Original URL to shorten",
                example = "https://training.linuxfoundation.org",
                defaultValue = "https://training.linuxfoundation.org"
        )
        @NotBlank(message = "longUrl is required")
        @Size(max = 2048, message = "longUrl must be at most 2048 characters")
        @Pattern(regexp = "^https?://\\S+$", message = "longUrl must be a valid http or https URL")
        String longUrl
) {
}
