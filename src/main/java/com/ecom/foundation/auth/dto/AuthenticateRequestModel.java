package com.ecom.foundation.auth.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record AuthenticateRequestModel(

    @NotBlank 
    String isd,
    
    @NotBlank 
    String mobile,
    
    String email,
    
    @NotBlank 
    String otp,

    @NotBlank 
    String name,

    @NotBlank 
    String lastName,

    @NotBlank 
    String token,

    @NotBlank 
    String context,

    @NotBlank 
    UUID termId

) {}
