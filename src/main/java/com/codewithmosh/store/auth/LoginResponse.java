package com.codewithmosh.store.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class LoginResponse {
    private Jwt accessToken;
    private Jwt refreshToken;
}
