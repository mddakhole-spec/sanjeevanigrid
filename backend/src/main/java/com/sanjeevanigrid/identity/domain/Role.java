package com.sanjeevanigrid.identity.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    protected Role() {}

    public Role(String name) {
        this.name = name;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }

    public static final String CITIZEN = "CITIZEN";
    public static final String DOCTOR = "DOCTOR";
    public static final String HOSPITAL_ADMIN = "HOSPITAL_ADMIN";
    public static final String DONOR = "DONOR";
    public static final String CAMP_WORKER = "CAMP_WORKER";
    public static final String CAMP_ORGANIZER = "CAMP_ORGANIZER";
    public static final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
}
