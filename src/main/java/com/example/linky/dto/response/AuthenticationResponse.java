package com.example.linky.dto.response;

public record AuthenticationResponse(
        String accessToken,
        String refreshToken
) {
}
