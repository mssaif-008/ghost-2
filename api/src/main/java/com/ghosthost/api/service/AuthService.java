package com.ghosthost.api.service;

import com.ghosthost.api.dto.AuthRequest;
import com.ghosthost.api.entity.User;
import com.ghosthost.api.repository.UserRepository;
import com.ghosthost.api.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Authentication service — handles register and login.
 *
 * HOW IT WORKS:
 * 1. Register: hash the password with BCrypt, save user, return JWT
 * 2. Login: find user by email, verify password, return JWT
 *
 * WHY BCrypt?
 * BCrypt is a slow hashing algorithm ON PURPOSE.
 * If someone steals your database, they can't quickly
 * brute-force the passwords because each hash attempt is slow.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Register a new user.
     * 
     * @return JWT token
     * @throws IllegalArgumentException if email already exists
     */
    public String register(AuthRequest request) {
        // Check if email is already taken
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Hash the password (NEVER store plaintext!)
        String hash = passwordEncoder.encode(request.getPassword());

        // Save the user
        User user = new User(request.getEmail(), hash);
        user = userRepository.save(user);

        // Generate and return JWT
        return jwtUtil.generateToken(user.getId(), user.getEmail());
    }

    /**
     * Login an existing user.
     * 
     * @return JWT token
     * @throws IllegalArgumentException if credentials are invalid
     */
    public String login(AuthRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Generate and return JWT
        return jwtUtil.generateToken(user.getId(), user.getEmail());
    }
}
