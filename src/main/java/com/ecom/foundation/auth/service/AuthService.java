package com.ecom.foundation.auth.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecom.foundation.auth.entity.Account;
import com.ecom.foundation.auth.repository.AccountRepository;


@Service
public class AuthService {

    @Autowired
    private AccountRepository accountRepository;

    @Transactional(readOnly =true)
    public Optional<Account> getAccountByMobile(String mobile) {
        return accountRepository.findByMobile(mobile);
    }
}