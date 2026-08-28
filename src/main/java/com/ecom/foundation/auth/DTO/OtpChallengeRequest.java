package com.ecom.foundation.auth.DTO;

import java.time.Instant;
import java.util.UUID;


public class OtpChallengeRequest {
    public UUID UUID;
    public String mobile;
    public String IsdCode;
    public String purpose;
    public Short attempts;
    public Instant createdAt;
    public Short expiry;
    public Instant lastRetry;
    public boolean revocation;

    public OtpChallengeRequest(String mobile, String purpose, String isdCode, String attempt, Instant lastRetry) {
        this.mobile = mobile.substring(1);
        this.purpose = purpose;
        this.IsdCode = isdCode;
        this.lastRetry = lastRetry;
    }

}
