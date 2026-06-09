package com.itr.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Entity for TDS deductor master data.
 */
@Entity
@Table(name = "tds_deductors")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TDSDeductor {
    
    @Id
    @Column(name = "tan", length = 10)
    private String tan;
    
    @Column(name = "deductor_name", nullable = false, length = 200)
    private String deductorName;
    
    @Column(name = "deductor_address", columnDefinition = "TEXT")
    private String deductorAddress;
    
    @Column(name = "deductor_type", length = 50)
    private String deductorType;
    
    @Column(name = "deductor_pan", length = 10)
    private String deductorPan;
    
    @Column(name = "city", length = 100)
    private String city;
    
    @Column(name = "state", length = 50)
    private String state;
    
    @Column(name = "pin_code", length = 6)
    private String pinCode;
    
    @Column(name = "is_verified")
    private Boolean isVerified;
    
    @Column(name = "verification_source", length = 50)
    private String verificationSource;
    
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    
    @Column(name = "last_updated")
    private OffsetDateTime lastUpdated;
    
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        lastUpdated = OffsetDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = OffsetDateTime.now();
    }
}
