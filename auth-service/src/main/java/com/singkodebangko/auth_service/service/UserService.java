package com.singkodebangko.auth_service.service;

import com.singkodebangko.auth_service.model.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    private final Map<String, User> users = new HashMap<>();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService() {
        users.put(
                "juan.delacruz@neobank.com",
                new User(
                        1001L,
                        "juan.delacruz@neobank.com",
                        passwordEncoder.encode("Password123!")
                )
        );

        users.put(
            "maria.santos@neobank.com",
            new User(
                1002L,
                "maria.santos@neobank.com",
                passwordEncoder.encode("Password456!")
            )
        );
    }

    public User findByEmail(String email) {
        return users.get(email);
    }

    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}