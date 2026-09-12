package com.ecom.foundation.auth.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.foundation.auth.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Short> {
    Role findByCode(String code);
    boolean existsByCode(String code);
}