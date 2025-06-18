package com.codewithmosh.store.auth;

import com.codewithmosh.store.users.UserDto;
import com.codewithmosh.store.users.UserMapper;
import com.codewithmosh.store.users.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtConfig jwtConfig;
    private final AuthService authService;

    @PostMapping("/login")
    public JwtResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        var loginResult = authService.login(request);
        var cookie = new Cookie("refreshToken", loginResult.getRefreshToken().toString());
        cookie.setHttpOnly(true);
        cookie.setPath("/auth/refresh");
        cookie.setMaxAge(jwtConfig.getRefreshTokenExpiration()); //7days
        cookie.setSecure(true);
        response.addCookie(cookie);

        return new JwtResponse(loginResult.getAccessToken().toString());
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(){
        //get the principal
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = (Long) authentication.getPrincipal();

        //find user by email
        var user = userRepository.findById(userId).orElse(null);
        if(user == null) {
            return ResponseEntity.notFound().build();
        }

        //map the user to dto
        var userDto = userMapper.toDto(user);
        return ResponseEntity.ok(userDto);
    }

    @PostMapping("/refresh")
    public JwtResponse refresh(
            @CookieValue(value ="refreshToken")String refreshToken){
        var accessToken = authService.refreshAccessToken(refreshToken);
        return new JwtResponse(accessToken.toString());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Void> handleBadCredentialsException() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
