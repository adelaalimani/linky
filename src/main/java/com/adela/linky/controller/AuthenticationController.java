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

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService service;

    public AuthenticationController(AuthenticationService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(this.service.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody  RegistrationRequest request) {
        this.service.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
