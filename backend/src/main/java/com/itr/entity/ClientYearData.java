package com.itr.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "client_year_data",
       uniqueConstraints = @UniqueConstraint(columnNames = {"client_id", "assessment_year"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClientYearData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "assessment_year", nullable = false, length = 10)
    private String assessmentYear;

    @Column(name = "raw_prefill_json", columnDefinition = "TEXT")
    private String rawPrefillJson;

    @Column(name = "computed_itr1_json", columnDefinition = "TEXT")
    private String computedItr1Json;

    @Column(length = 20)
    @Builder.Default
    private String status = "draft";

    @Column(name = "itr_type", length = 10)
    @Builder.Default
    private String itrType = "ITR1";

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    @Column(name = "hp_loss_carry_forward")
    @Builder.Default
    private Long hpLossCarryForward = 0L;

    // Section 10: Imported document storage for reconciliation
    @Column(name = "ais_data", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    private String aisDataJson;

    @Column(name = "form26as_data", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    private String form26ASDataJson;

    @Column(name = "tis_data", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    private String tisDataJson;

    @Column(name = "import_timestamp")
    private LocalDateTime importTimestamp;

    @Column(name = "reconciliation_report", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    private String reconciliationReportJson;
}
