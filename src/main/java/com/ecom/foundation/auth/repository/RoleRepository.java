package com.ecom.foundation.auth.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecom.foundation.auth.Entity.Role;

public interface RoleRepository extends JpaRepository<Role, Short> {
    Optional<Role> findByCode(String code);
    boolean existsByCode(String code);
}