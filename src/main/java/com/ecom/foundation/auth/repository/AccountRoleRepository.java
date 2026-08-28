package com.ecom.foundation.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.foundation.auth.entity.AccountRole;
import com.ecom.foundation.auth.entity.AccountRoleId;

public interface AccountRoleRepository extends JpaRepository<AccountRole, AccountRoleId> {
}