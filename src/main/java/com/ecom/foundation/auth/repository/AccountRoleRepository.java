package com.ecom.foundation.auth.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.foundation.auth.Entity.AccountRole;
import com.ecom.foundation.auth.Entity.AccountRoleId;

public interface AccountRoleRepository extends JpaRepository<AccountRole, AccountRoleId> {
}