package com.itr.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Audit Trail Entity - Section 7 ITR_ERP_FINAL_COMPLETION_DIRECTIVE.md
 * Persistent audit log for all ITR operations
 */
@Entity
@Table(name = "audit_trail", indexes = {
    @Index(name = "idx_audit_pan_ay", columnList = "taxpayer_pan, assessment_year"),
    @Index(name = "idx_audit_event_type", columnList = "event_type, created_at")
})
@Data
@NoArgsConstructor
public class AuditTrail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "taxpayer_pan", length = 10)
    private String taxpayerPAN;

    @Column(name = "assessment_year", length = 7)
    private String assessmentYear;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "event_description", nullable = false, columnDefinition = "TEXT")
    private String eventDescription;

    @Column(name = "data_source", length = 100)
    private String dataSource;

    @Column(name = "field_name", length = 200)
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "validation_rule_id", length = 30)
    private String validationRuleId;

    @Column(name = "validation_result", length = 10)
    private String validationResult;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by", length = 100)
    private String createdBy;
    
    @Column(name = "entry_hash", length = 64)
    private String entryHash;
    
    @Column(name = "previous_hash", length = 64)
    private String previousHash;

    public static AuditTrail of(String pan, String ay, String eventType, String description, String source) {
        AuditTrail at = new AuditTrail();
        at.taxpayerPAN = pan;
        at.assessmentYear = ay;
        at.eventType = eventType;
        at.eventDescription = description;
        at.dataSource = source;
        at.createdBy = "SYSTEM";
        return at;
    }
}
