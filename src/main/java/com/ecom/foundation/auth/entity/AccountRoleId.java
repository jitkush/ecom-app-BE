package com.ecom.foundation.auth.Entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AccountRoleId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "role_id")
    private Short roleId;

    protected AccountRoleId() {
        // Default constructor for JPA
    }

    public AccountRoleId(Long accountId, Short roleId) {
        this.accountId = accountId;
        this.roleId = roleId;
    }

    @Override
    public boolean equals(Object sharedClassObject) {
        if (this == sharedClassObject) return true;
        if (sharedClassObject == null || getClass() != sharedClassObject.getClass()) return false;
        AccountRoleId incomingClassObject = (AccountRoleId) sharedClassObject ;
        return Objects.equals(accountId, incomingClassObject.accountId) && Objects.equals(roleId, incomingClassObject.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, roleId);
    }

}