package com.codewithmosh.store.auth;

import com.codewithmosh.store.common.SecurityRules;
import com.codewithmosh.store.users.Role;
import com.codewithmosh.store.users.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@AllArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final List<SecurityRules> featureSecurityRules;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        //Stateless sessions (token-based authentication)
        //Disable CSRF (Cross-Site Request Forgery)
        //Authorize (what endpoint public or private)

        http
                .sessionManagement(c->c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(c->{
                    featureSecurityRules.forEach(r->r.configure(c));
                    c.anyRequest().authenticated();
                        }
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(c -> {
                    c.authenticationEntryPoint(
                            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)); //401
                    c.accessDeniedHandler((request, response, accessDeniedException)
                            -> response.setStatus(HttpStatus.FORBIDDEN.value()));
                });
        return http.build();

    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        var provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder());
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }

    public static class Jwt {

        private final Claims claims;
        private final SecretKey secretKey;

        public Jwt(Claims claims, SecretKey secretKey) {
            this.claims = claims;
            this.secretKey = secretKey;
        }

        public boolean isExpired() {
            return claims.getExpiration().before(new Date());
        }

        public Long getUserId() {
            return  Long.valueOf(claims.getSubject());
        }

        public Role getRole() {
            return Role.valueOf(claims.get("role", String.class));
        }

        public String toString(){
            return Jwts.builder().claims(claims).signWith(secretKey).compact();
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
