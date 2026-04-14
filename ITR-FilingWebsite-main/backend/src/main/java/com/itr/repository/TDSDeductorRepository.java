package com.itr.repository;

import com.itr.entity.TDSDeductor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for TDS Deductor master data.
 */
@Repository
public interface TDSDeductorRepository extends JpaRepository<TDSDeductor, String> {
    
    Optional<TDSDeductor> findByTan(String tan);
    
    List<TDSDeductor> findByDeductorNameContainingIgnoreCase(String name);
    
    List<TDSDeductor> findByDeductorType(String deductorType);
    
    List<TDSDeductor> findByIsVerified(Boolean isVerified);
}
