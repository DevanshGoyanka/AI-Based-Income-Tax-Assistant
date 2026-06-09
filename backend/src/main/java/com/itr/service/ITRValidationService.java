package com.itr.service;

import com.itr.dto.Itr1FormData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ITRValidationService {

    public List<String> validateItr1Form(Itr1FormData formData) {
        List<String> errors = new ArrayList<>();
        
        // Validate Personal Info
        if (formData.getPersonalInfo() == null) {
            errors.add("Personal information is required");
            return errors;
        }
        
        Itr1FormData.PersonalInfo info = formData.getPersonalInfo();
        
        if (info.getPan() == null || !info.getPan().matches("[A-Z]{5}[0-9]{4}[A-Z]")) {
            errors.add("Invalid PAN format. Expected: AAAAA9999A");
        }
        
        if (info.getAadhaar() != null && !info.getAadhaar().matches("\\d{12}")) {
            errors.add("Invalid Aadhaar format. Expected: 12 digits");
        }
        
        if (info.getAssesseeName() == null || info.getAssesseeName().trim().isEmpty()) {
            errors.add("Assessee name is required");
        }
        
        if (info.getDateOfBirth() == null) {
            errors.add("Date of birth is required");
        }
        
        if (info.getEmail() != null && !info.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            errors.add("Invalid email format");
        }
        
        if (info.getMobile() != null && !info.getMobile().matches("^[+]?[0-9]{10,15}$")) {
            errors.add("Invalid mobile number format");
        }
        
        // Validate regime selection
        if (info.getRegime() == null || (!info.getRegime().equals("OLD") && !info.getRegime().equals("NEW"))) {
            errors.add("Tax regime must be either OLD or NEW");
        }
        
        // Validate bank IFSC if provided
        if (info.getBankIFSC() != null && !info.getBankIFSC().isEmpty() && 
            !info.getBankIFSC().matches("^[A-Z]{4}0[A-Z0-9]{6}$")) {
            errors.add("Invalid IFSC code format");
        }
        
        log.info("Validation completed with {} errors", errors.size());
        return errors;
    }
    
    public List<String> validateBeforeFiling(Itr1FormData formData) {
        List<String> errors = validateItr1Form(formData);
        
        // Additional checks before filing
        if (formData.getPersonalInfo() != null) {
            if (!formData.getPersonalInfo().isPanAadhaarLinked()) {
                errors.add("PAN and Aadhaar must be linked before filing");
            }
            
            if (formData.getPersonalInfo().getBankAccountNo() == null) {
                errors.add("Bank account details are mandatory for e-filing");
            }
        }
        
        return errors;
    }
}
