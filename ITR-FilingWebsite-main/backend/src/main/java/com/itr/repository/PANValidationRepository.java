package com.itr.repository;

import com.itr.entity.PANValidationCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for PAN Validation Cache.
 */
@Repository
public interface PANValidationRepository extends JpaRepository<PANValidationCache, String> {
    
    Optional<PANValidationCache> findByPan(String pan);
    
    Optional<PANValidationCache> findByEntityType(Character entityType);
}
