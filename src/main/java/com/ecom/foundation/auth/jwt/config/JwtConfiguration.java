package com.ecom.foundation.auth.jwt.config;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.util.Objects;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration(proxyBeanMethods = false)
public class JwtConfiguration {

        private static final int MINIMUM_RSA_BITS = 2048;

        @Bean
        public Clock jwtClock() {

                return Clock.systemUTC();
        }

        @Bean
        public KeyPair otpProofKeyPair(JwtProperties properties) {
                try (
                        InputStream privateKeyStream = properties.privateKey().getInputStream();
                        InputStream publicKeyStream = properties.publicKey().getInputStream()) {
                        RSAPrivateKey privateKey = Objects.requireNonNull(RsaKeyConverters.pkcs8().convert(privateKeyStream),"Could not read JWT private key");

                        RSAPublicKey publicKey = Objects.requireNonNull(RsaKeyConverters.x509().convert(publicKeyStream),"Could not read JWT public key");

                        validateKeyPair(privateKey, publicKey);

                        return new KeyPair(publicKey, privateKey);

                } catch (IOException exception) {
                        throw new IllegalStateException("Could not load OTP-proof RSA keys",exception);
                }
        }

        @Bean
        public JwtEncoder jwtEncoder(KeyPair otpProofKeyPair, JwtProperties properties) {

                RSAPublicKey publicKey = (RSAPublicKey) otpProofKeyPair.getPublic();

                RSAPrivateKey privateKey = (RSAPrivateKey) otpProofKeyPair.getPrivate();

                RSAKey rsaKey = new RSAKey.Builder(publicKey)
                                .privateKey(privateKey)
                                .keyID(properties.kid())
                                .keyUse(KeyUse.SIGNATURE)
                                .algorithm(JWSAlgorithm.RS256)
                                .build();

                return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(rsaKey)));
        }

        @Bean
        public JwtDecoder jwtDecoder(KeyPair otpProofKeyPair, JwtProperties properties, Clock jwtClock) {

                RSAPublicKey publicKey = (RSAPublicKey) otpProofKeyPair.getPublic();

                NimbusJwtDecoder decoder = NimbusJwtDecoder
                                .withPublicKey(publicKey)
                                .signatureAlgorithm(SignatureAlgorithm.RS256)
                                .build();

                JwtTimestampValidator timestampValidator = new JwtTimestampValidator(
                                properties.clockSkew());

                timestampValidator.setClock(jwtClock);

                JwtIssuerValidator issuerValidator = new JwtIssuerValidator(
                                properties.issuer());

                decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestampValidator,issuerValidator));

                return decoder;
        }

        private void validateKeyPair(RSAPrivateKey privateKey, RSAPublicKey publicKey) {

                if (!privateKey.getModulus().equals(publicKey.getModulus())) {
                        throw new IllegalStateException("JWT private and public keys do not match");
                }

                if (publicKey.getModulus().bitLength() < MINIMUM_RSA_BITS) {
                        throw new IllegalStateException("JWT RSA key must be at least 2048 bits");
                }
        }
}