package com.adela.linky.controller;

import com.adela.linky.service.AuthenticationService;
import com.adela.linky.dto.request.LoginRequest;
import com.adela.linky.dto.response.AuthenticationResponse;
import com.adela.linky.dto.request.RegistrationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints for logging in and registering; no token needed.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService authenticateService;

    public AuthenticationController(AuthenticationService authenticateService) {
        this.authenticateService = authenticateService;
    }

    /**
     * Checks the email and password and returns a JWT access token.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticateService.login(request));
    }

    /**
     * Creates a new user with the CUSTOMER role.
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegistrationRequest request) {
        authenticateService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

}
