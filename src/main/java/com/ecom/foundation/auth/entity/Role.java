package com.ecom.foundation.auth.Entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "role", schema = "auth")
public class Role{
    @Id
    @Column(name = "id")
    private Short id;
    
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "description", nullable = true, length = 255)
    private String description; 

    @Column(name = "system_role", nullable = false)
    private Boolean systemRole;

    @Column(name= "created_at", nullable = false)
    private Instant createdAt;

    protected Role() {
        // Default constructor for JPA
    }

    public Role(
        Short id,
        String code,
        String displayName,
        String description,
        Boolean systemRole
    ) {
        this.id = id;
        this.code = code;
        this.displayName = displayName;
        this.description = description;
        this.systemRole = systemRole;
        this.createdAt = Instant.now();
    }

    public Short getId() {
        return id;
    }
}