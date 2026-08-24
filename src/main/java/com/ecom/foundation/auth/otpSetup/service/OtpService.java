package com.ecom.foundation.auth.otpSetup.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecom.foundation.auth.otpSetup.config.OtpContext;
import com.ecom.foundation.auth.otpSetup.config.OtpProperties;
import com.ecom.foundation.auth.otpSetup.dto.OtpChallengeCacheEntry;
import com.ecom.foundation.auth.otpSetup.dto.OtpChallengeResponse;
import com.ecom.foundation.auth.otpSetup.dto.OtpRequestModel;
import com.ecom.foundation.common.error.ApplicationException;
import com.ecom.foundation.common.error.ErrorCode;
import com.ecom.foundation.common.helper.RandomGenerator;
import com.ecom.foundation.common.redis.RedisKeyBuilder;
import com.ecom.foundation.common.redis.RedisValueStoreImpl;


@Service
public class OtpService {
    
    @Autowired
    RandomGenerator randomGenerator;

    @Autowired
    OtpHmacService otpHmacService;

    @Autowired
    RedisValueStoreImpl redisValueStoreImpl;

    @Autowired
    RedisKeyBuilder redisKeyBuilder;
    
    @Autowired
    OtpProperties otpProperties;

    public OtpChallengeResponse sendSignupOtp(OtpRequestModel request){
        //need to create a service that puts OTP into queue
        //need common integration setup for smse which will come when we are done with the OTPset up part
        // for this setup for this, 

        OtpChallengeResponse response = OtpGenerationAndCacheBuilding(request);
        return response;
    }

    public OtpChallengeResponse reSendOtp(OtpRequestModel request) {

        OtpChallengeResponse response = OtpGenerationAndCacheBuilding(request);
        return response;
    }

    private OtpChallengeResponse OtpGenerationAndCacheBuilding(OtpRequestModel request) {
        String mobileHmac = otpHmacService.generateMobileHmac(request.isd(), request.mobile());

        Optional<OtpChallengeCacheEntry> otpChallengeCacheEntry;
        try {
            otpChallengeCacheEntry = redisValueStoreImpl.find(mobileHmac, OtpChallengeCacheEntry.class);
        } catch (Exception exception) {
            throw new ApplicationException(ErrorCode.OTP_CHALLENGE_INVALID, "no challangeId found");
        } 

        String redisKey = redisKeyBuilder.build("OTP","challengId", mobileHmac);
        OtpContext otpFlow = Optional.ofNullable(request.context())
                                        .map(String::toUpperCase)
                                        .map(OtpContext::valueOf)
                                        .orElse(OtpContext.CUSTOMER_SIGNUP);

        String otp = (OtpContext.CUSTOMER_SIGNUP).equals(otpFlow) ? randomGenerator.OtpGenerator(6) : randomGenerator.OtpGenerator(8);
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(otpProperties.getValidity());
        Instant resendAvailableAt = issuedAt.plus(otpProperties.getResendDelay());

        String otpDigest = otpHmacService.generateDigest(
            otpFlow,
            request.isd(),
            request.mobile(),
            otp
        );
        int failedAttempts = 0 ;
        
        OtpChallengeCacheEntry newOtpChallengeCacheEntry = new OtpChallengeCacheEntry(
            request.isd(),
            request.mobile(),
            otpFlow,
            otpDigest,
            issuedAt,
            expiresAt,
            resendAvailableAt,
            failedAttempts
        );

        redisValueStoreImpl.save(redisKey, newOtpChallengeCacheEntry, Duration.between(Instant.now(), expiresAt));

        OtpChallengeResponse otpChallengeResponse = new OtpChallengeResponse(
            expiresAt,
            resendAvailableAt
        );

        return otpChallengeResponse;
    }
    
    
}
