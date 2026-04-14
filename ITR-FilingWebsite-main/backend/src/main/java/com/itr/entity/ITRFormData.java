package com.itr.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;

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
    
    @Type(JsonBinaryType.class)
    @Column(name = "form_data", columnDefinition = "jsonb", nullable = false)
    private String formData;
    
    @Type(JsonBinaryType.class)
    @Column(name = "computation_data", columnDefinition = "jsonb")
    private String computationData;
    
    @Column(name = "validation_status", length = 20)
    private String validationStatus;
    
    @Type(JsonBinaryType.class)
    @Column(name = "validation_errors", columnDefinition = "jsonb")
    private String validationErrors;
    
    @Type(JsonBinaryType.class)
    @Column(name = "validation_warnings", columnDefinition = "jsonb")
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
