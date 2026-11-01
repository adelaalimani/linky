package com.adela.linky.service;

import com.adela.linky.dto.request.LoginRequest;
import com.adela.linky.dto.response.AuthenticationResponse;
import com.adela.linky.dto.request.RegistrationRequest;
import com.adela.linky.entity.Role;
import com.adela.linky.entity.User;
import com.adela.linky.exception.BadRequestException;
import com.adela.linky.exception.ConflictException;
import com.adela.linky.mapper.UserMapper;
import com.adela.linky.repository.RoleRepository;
import com.adela.linky.repository.UserRepository;
import com.adela.linky.security.AuthUser;
import com.adela.linky.security.jwt.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Handles login and registration of users.
 */
@Service
@Slf4j
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    public AuthenticationService(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider, UserRepository userRepository, RoleRepository roleRepository, UserMapper userMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
    }

    /**
     * Checks email and password and returns an access token with the user's id and roles.
     */
    public AuthenticationResponse login(LoginRequest request){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );
        User user = (User) authentication.getPrincipal();
        String token = jwtTokenProvider.generateAccessToken(AuthUser.from(user));
        log.info("User {} logged in", user.getUsername());
        return new AuthenticationResponse(token);
    }

    /**
     * Validates the request and saves a new user with the CUSTOMER role.
     */
    @Transactional
    public void register(RegistrationRequest request){
        checkPasswords(request.password(), request.confirmPassword());
        checkUserEmail(request.email());
        checkUserPhoneNumber(request.phoneNumber());

        User user = userMapper.toUser(request);
        user.setRoles(List.of(getCustomerRole()));
        userRepository.save(user);
        log.info("User {} registered with id {}", user.getUsername(), user.getId());
    }

    private void checkPasswords(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new BadRequestException("Passwords do not match");
        }
    }

    private void checkUserPhoneNumber(String phoneNumber) {
        boolean userExists = userRepository.existsByPhoneNumber(phoneNumber);
        if (userExists) {
            throw new ConflictException("User with phone number " + phoneNumber + " already exists");
        }
    }

    private void checkUserEmail(String email) {
        boolean userExists = userRepository.existsByEmailIgnoreCase(email);
        if (userExists) {
            throw new ConflictException("User with email " + email + " already exists");
        }
    }

    private Role getCustomerRole() {
        return roleRepository.findByName("CUSTOMER").orElseThrow(() ->
                        new IllegalStateException("CUSTOMER role not found in database"));
    }


}
