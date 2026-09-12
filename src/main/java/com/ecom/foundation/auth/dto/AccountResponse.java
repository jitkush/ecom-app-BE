package com.ecom.foundation.auth.dto;

import java.util.List;
import java.util.UUID;

public record AccountResponse(
        UUID publicId,
        String email,
        String mobile,
        List<String> role
) {
}