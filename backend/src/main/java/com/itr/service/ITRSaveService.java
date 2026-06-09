package com.itr.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ITRSaveService {
    
    public void saveITR1(Long clientId, String year, Object formData, Long userId) {
        log.warn("ITRSaveService.saveITR1 - stub implementation");
    }
    
    public void saveITR(Long clientId, String year, Object formData, String itrType, Long userId) {
        log.warn("ITRSaveService.saveITR - stub implementation for ITR type: {}", itrType);
    }
    
    public Object loadITR(Long clientId, String year, String itrType, Long userId) {
        log.warn("ITRSaveService.loadITR - stub implementation for ITR type: {}", itrType);
        return null;
    }
}
