package com.itr.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "itr_filings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ITRFiling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "assessment_year", nullable = false, length = 10)
    private String assessmentYear;

    @Column(name = "itr_type", nullable = false, length = 10)
    private String itrType;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "draft";

    @Column(name = "filing_date")
    private LocalDate filingDate;

    @Column(name = "acknowledgement_number", length = 50)
    private String acknowledgementNumber;

    @Column(name = "total_income")
    private Long totalIncome;

    @Column(name = "tax_payable")
    private Long taxPayable;

    @Column(name = "refund_amount")
    private Long refundAmount;

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
