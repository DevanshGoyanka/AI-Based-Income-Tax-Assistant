package com.itr.config;

import com.itr.entity.User;
import com.itr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("test@test.com")) {
            User user = User.builder()
                    .email("test@test.com")
                    .password(passwordEncoder.encode("password"))
                    .build();
            userRepository.save(user);
            log.info("Test user created: test@test.com");
        } else {
            log.info("Test user already exists: test@test.com");
        }

        // Additional test user
        if (!userRepository.existsByEmail("test1@test.com")) {
            User user1 = User.builder()
                    .email("test1@test.com")
                    .password(passwordEncoder.encode("pokemon123"))
                    .build();
            userRepository.save(user1);
            log.info("Test user created: test1@test.com");
        } else {
            log.info("Test user already exists: test1@test.com");
        }
    }
}
