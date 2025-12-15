package com.postura.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class OAuthLoginRequest {

    @NotBlank
    private String code;
}
