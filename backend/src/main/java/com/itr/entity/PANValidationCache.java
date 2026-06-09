package com.itr.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Entity for PAN validation cache.
 */
@Entity
@Table(name = "pan_validation_cache")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PANValidationCache {
    
    @Id
    @Column(name = "pan", length = 10)
    private String pan;
    
    @Column(name = "entity_type", nullable = false, length = 1)
    private Character entityType;
    
    @Column(name = "entity_description", length = 100)
    private String entityDescription;
    
    @Column(name = "is_valid")
    private Boolean isValid;
    
    @Column(name = "eligible_itr_forms", length = 50)
    private String eligibleItrForms;
    
    @Column(name = "validated_at")
    private OffsetDateTime validatedAt;
    
    @Column(name = "last_accessed")
    private OffsetDateTime lastAccessed;
    
    @PrePersist
    protected void onCreate() {
        validatedAt = OffsetDateTime.now();
        lastAccessed = OffsetDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        lastAccessed = OffsetDateTime.now();
    }
}
