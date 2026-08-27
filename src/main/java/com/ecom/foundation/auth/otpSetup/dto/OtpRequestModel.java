package com.ecom.foundation.auth.otpSetup.dto;

import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OtpRequestModel(
    @NotBlank
    @Pattern(
        regexp = "^91$",
        message="isd code is invalid"
    )
    String isd,

    @NotBlank
    @Pattern(
        regexp = "^[6-9]\\d{9}$",
        message="mobile number is invalid"
    )
    String mobile,

    String context,

    String otp

) {
    @Override
    public String toString () {
        return "{isd=%s, mobile=%s}"
        .formatted(isd, mobile);  
    }
}
