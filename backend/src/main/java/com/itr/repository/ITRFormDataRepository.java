package com.itr.repository;

import com.itr.entity.ITRFormData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ITR Form Data storage.
 */
@Repository
public interface ITRFormDataRepository extends JpaRepository<ITRFormData, Long> {
    
    Optional<ITRFormData> findByClientIdAndAssessmentYearAndItrFormType(
            Long clientId, String assessmentYear, String itrFormType);
    
    List<ITRFormData> findByClientIdAndAssessmentYear(Long clientId, String assessmentYear);
    
    List<ITRFormData> findByClientId(Long clientId);
    
    List<ITRFormData> findByValidationStatus(String validationStatus);
}
