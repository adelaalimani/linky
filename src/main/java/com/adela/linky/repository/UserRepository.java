package com.adela.linky.repository;

import com.adela.linky.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
}
