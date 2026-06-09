package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilingRequest {
    private Long clientId;
    private String assessmentYear;
    private String itrType;
    private String status;
    private LocalDate filingDate;
    private String acknowledgementNumber;
}
