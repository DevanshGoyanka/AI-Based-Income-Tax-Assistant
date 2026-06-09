package com.itr.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * Entity for storing ITR form data as JSON.
 */
@Entity
@Table(name = "itr_form_data")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ITRFormData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "client_id", nullable = false)
    private Long clientId;
    
    @Column(name = "assessment_year", nullable = false, length = 10)
    private String assessmentYear;
    
    @Column(name = "itr_form_type", nullable = false, length = 10)
    private String itrFormType;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "form_data", columnDefinition = "TEXT", nullable = false)
    private String formData;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "computation_data", columnDefinition = "TEXT")
    private String computationData;
    
    @Column(name = "validation_status", length = 20)
    private String validationStatus;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_errors", columnDefinition = "TEXT")
    private String validationErrors;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_warnings", columnDefinition = "TEXT")
    private String validationWarnings;
    
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
