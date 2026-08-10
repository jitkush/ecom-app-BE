package com.ecom.foundation.auth.DTO;

import lombok.Getter;

public class OtpChallengeRequest {
    @Getter
    public String mobile;

    @Getter
    public String purpose;

    public OtpChallengeRequest(String mobile, String purpose) {
        this.mobile = mobile;
        this.purpose = purpose;
    }
}
