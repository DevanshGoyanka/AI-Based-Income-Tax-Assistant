package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilingResponse {
    private Long id;
    private Long clientId;
    private String clientName;
    private String clientPan;
    private String assessmentYear;
    private String itrType;
    private String status;
    private LocalDate filingDate;
    private String acknowledgementNumber;
    private Long totalIncome;
    private Long taxPayable;
    private Long refundAmount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
