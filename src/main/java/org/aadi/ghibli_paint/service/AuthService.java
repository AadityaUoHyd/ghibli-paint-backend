// Backend: Update src/main/java/org/aadi/ghibli_paint/service/AuthService.java
package org.aadi.ghibli_paint.service;

import lombok.RequiredArgsConstructor;
import org.aadi.ghibli_paint.dto.AuthRequest;
import org.aadi.ghibli_paint.dto.AuthResponse;
import org.aadi.ghibli_paint.dto.SignupRequest;
import org.aadi.ghibli_paint.entity.User;
import org.aadi.ghibli_paint.repository.UserRepository;
import org.aadi.ghibli_paint.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setIsActive(true);

        User savedUser = userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getUsername());
        String token = jwtUtil.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .userId(savedUser.getId())
                .build();
    }

    public AuthResponse login(AuthRequest request) {
        String input = request.getUsername();
        Optional<User> userOpt = userRepository.findByUsername(input);

        // If not found by username, try email
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(input);
        }

        if (userOpt.isEmpty()) {
            throw new UsernameNotFoundException("User not found with provided credentials");
        }

        User user = userOpt.get();

        // Authenticate using the actual username and provided password
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtUtil.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .userId(user.getId())
                .build();
    }
}