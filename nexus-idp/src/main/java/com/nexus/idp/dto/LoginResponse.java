package com.nexus.idp.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    String accessToken;
    String sessionKey;
}
