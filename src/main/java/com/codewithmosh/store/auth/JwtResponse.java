package com.codewithmosh.store.auth;

import com.codewithmosh.store.users.User;
import com.codewithmosh.store.users.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;

@AllArgsConstructor
@Data
public class JwtResponse {
    private String token;

    @AllArgsConstructor
    @Service
    public static class AuthService {

        private final UserRepository userRepository;

        public User getCurrentUser(){
            //get the principal
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            var userId = (Long) authentication.getPrincipal();

            //find user by email
            return userRepository.findById(userId).orElse(null);
        }
    }

    @AllArgsConstructor
    @Service
    public static class JwtService {
        private final JwtConfig jwtConfig;

        public Jwt generateAccessToken(User user) {
            System.out.println(jwtConfig.getAccessTokenExpiration());
            return generateToken(user, jwtConfig.getAccessTokenExpiration());
        }

        public Jwt generateRefreshToken(User user) {
            System.out.println(jwtConfig.getRefreshTokenExpiration());
            return generateToken(user, jwtConfig.getRefreshTokenExpiration());
        }

        private Jwt generateToken(User user, long tokenExpiration) {
            var claims = Jwts.claims()
                            .setSubject(user.getId().toString())
                            .add("email", user.getEmail())
                            .add("name", user.getName())
                            .add("role", user.getRole())
                            .issuedAt(new Date())
                            .expiration(new Date(System.currentTimeMillis() + 1000 * tokenExpiration))
                            .build();

            return new Jwt(claims, jwtConfig.getSecretKey());
        }

        public Jwt parseToken(String token) {
            try{
                var claims = getClaims(token);
                return new Jwt(claims, jwtConfig.getSecretKey());
            }catch (JwtException e) {
                return null;
            }
        }

        private Claims getClaims(String token) {
            return Jwts.parser()
                    .verifyWith(jwtConfig.getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        }

    }
}
