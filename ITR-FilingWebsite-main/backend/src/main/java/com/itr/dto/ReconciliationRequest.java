package com.itr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for reconciliation between 26AS, AIS, and TIS data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationRequest {
    private Form26ASData data26AS;
    private AISData aisData;
    private TISData tisData;
}
