package com.ecom.foundation.auth.dto;

import com.ecom.foundation.auth.entity.AuthenticationSession;

public record CreatedSession(
        String rawSecret,
        AuthenticationSession session
) {
}