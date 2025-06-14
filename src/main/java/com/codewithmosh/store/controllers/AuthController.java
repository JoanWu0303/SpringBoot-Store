package com.codewithmosh.store.controllers;

import com.codewithmosh.store.dtos.LoginRequest;
import lombok.AllArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Void> handleBadCredentialsException() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
