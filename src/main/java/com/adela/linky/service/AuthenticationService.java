package com.adela.linky.service;

import com.adela.linky.dto.request.LoginRequest;
import com.adela.linky.dto.response.AuthenticationResponse;
import com.adela.linky.dto.request.RegistrationRequest;
import com.adela.linky.entity.Role;
import com.adela.linky.entity.User;
import com.adela.linky.exception.BadRequestException;
import com.adela.linky.mapper.UserMapper;
import com.adela.linky.repository.RoleRepository;
import com.adela.linky.repository.UserRepository;
import com.adela.linky.security.jwt.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    public AuthenticationService(AuthenticationManager authenticationManager, JwtService jwtService, UserRepository userRepository, RoleRepository roleRepository, UserMapper userMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
    }

    public AuthenticationResponse login(LoginRequest request){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );
        User user = (User) authentication.getPrincipal();
        String token = jwtService.generateAccessToken(user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());
        return new AuthenticationResponse(token, refreshToken);
    }

    @Transactional
    public void register(RegistrationRequest request){
        checkUserEmail(request.email());
        checkUserPhoneNumber(request.phoneNumber());
        checkPasswords(request.password(), request.confirmPassword());

        User user = userMapper.toUser(request);
        user.setRoles(List.of(getCustomerRole()));
        log.info("Registering user: {}", user.getUsername());
        userRepository.save(user);
    }

    private void checkPasswords(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new BadRequestException("Passwords do not match");
        }
    }

    private void checkUserPhoneNumber(String phoneNumber) {
        boolean userExists = userRepository.existsByPhoneNumber(phoneNumber);
        if (userExists) {
            throw new BadRequestException("User with phone number " + phoneNumber + " already exists");
        }
    }

    private void checkUserEmail(String email) {
        boolean userExists = userRepository.existsByEmailIgnoreCase(email);
        if (userExists) {
            throw new BadRequestException("User with email " + email + " already exists");
        }
    }

    private Role getCustomerRole() {
        return roleRepository.findByName("CUSTOMER").orElseThrow(() ->
                        new IllegalStateException("CUSTOMER role not found in database"));
    }


}
