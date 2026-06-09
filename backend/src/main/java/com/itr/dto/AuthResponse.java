package com.itr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data @Builder @AllArgsConstructor
public class AuthResponse {
    @JsonProperty("accessToken")
    private String token;

    @JsonProperty("refreshToken")
    private String refreshToken;

    private String email;
    private long expiresIn;
}
