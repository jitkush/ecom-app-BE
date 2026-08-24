package com.ecom.foundation.auth.otpSetup.dto;

import java.util.UUID;

import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OtpRequestModel(
    @NotBlank
    @Pattern(
        regexp = "^[1-9]\\d{0-3}$",
        message="isd code is invalid"
    )
    String isd,

    @NotBlank
    @Pattern(
        regexp = "^[1-9]\\d{0-3}$",
        message="mobile number is invalid"
    )
    String mobile,

    @Enumerated
    String context

) {
    @Override
    public String toString () {
        return "{isd=%s, mobile=%s, context=%s}"
        .formatted(isd, mobile, context);  
    }
}
