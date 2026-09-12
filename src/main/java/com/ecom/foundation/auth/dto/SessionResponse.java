package com.ecom.foundation.auth.dto;

import java.util.List;
import java.util.UUID;

public record SessionResponse(
        boolean authenticated,
        UUID accountPublicId,
) {
}