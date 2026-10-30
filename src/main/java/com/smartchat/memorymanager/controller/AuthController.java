package com.smartchat.memorymanager.controller;

import com.smartchat.memorymanager.dto.AuthRequest;
import com.smartchat.memorymanager.dto.AuthResponse;
import com.smartchat.memorymanager.model.AppUser;
import com.smartchat.memorymanager.repository.UserRepository;
import com.smartchat.memorymanager.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(UserRepository users, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.users = users; this.passwordEncoder = passwordEncoder; this.authenticationManager = authenticationManager; this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        String username = normalize(request.username());
        validate(request, username);
        if (users.existsByUsername(username)) return ResponseEntity.badRequest().body(Map.of("error", "That username is already in use"));
        AppUser user = users.save(new AppUser(username, passwordEncoder.encode(request.password())));
        return ResponseEntity.ok(new AuthResponse(jwtService.generateToken(user), user.getUsername()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        String username = normalize(request.username());
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, request.password()));
        AppUser user = users.findByUsername(username).orElseThrow();
        return ResponseEntity.ok(new AuthResponse(jwtService.generateToken(user), user.getUsername()));
    }

    private void validate(AuthRequest request, String username) {
        if (!username.matches("[a-z0-9_.-]{3,60}")) throw new IllegalArgumentException("Use 3–60 lowercase letters, numbers, dots, dashes, or underscores for your username");
        if (request.password() == null || request.password().length() < 8) throw new IllegalArgumentException("Password must be at least 8 characters");
    }
    private String normalize(String username) { return username == null ? "" : username.trim().toLowerCase(Locale.ROOT); }
}
