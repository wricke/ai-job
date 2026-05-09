package com.hpc.jobagent.dto;

public record AuthResponse(
        boolean authenticated,
        Long id,
        String username,
        String displayName
) {
    public static AuthResponse anonymous() {
        return new AuthResponse(false, null, null, null);
    }
}
