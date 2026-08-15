package com.ecom.foundation.auth.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class AuthController {
    @GetMapping("/auth/otp") 
    public String getOtp(@RequestParam String mobile) {
        String Otp = "0";
        return Otp;    
    }
    
}
