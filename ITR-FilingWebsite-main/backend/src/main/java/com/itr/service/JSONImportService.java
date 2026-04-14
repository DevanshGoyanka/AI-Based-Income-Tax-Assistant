package com.itr.service;

import com.itr.dto.Itr1FormData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JSONImportService {
    
    public Itr1FormData importITR1FromJson(String jsonContent) {
        log.warn("JSONImportService.importITR1FromJson - stub implementation");
        return Itr1FormData.builder().build();
    }
    
    public Object importFromJson(String jsonContent, String itrType) {
        log.warn("JSONImportService.importFromJson - stub implementation for ITR type: {}", itrType);
        return null;
    }
}
