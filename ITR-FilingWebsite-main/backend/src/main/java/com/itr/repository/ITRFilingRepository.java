package com.itr.repository;

import com.itr.entity.ITRFiling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ITRFilingRepository extends JpaRepository<ITRFiling, Long> {

    @Query("SELECT f FROM ITRFiling f WHERE f.client.user.id = :userId")
    List<ITRFiling> findByUserId(@Param("userId") Long userId);

    @Query("SELECT f FROM ITRFiling f WHERE f.client.user.id = :userId AND f.status = :status")
    List<ITRFiling> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    @Query("SELECT f FROM ITRFiling f WHERE f.client.id = :clientId AND f.assessmentYear = :year")
    Optional<ITRFiling> findByClientIdAndYear(@Param("clientId") Long clientId, @Param("year") String year);

    @Query("SELECT COUNT(f) FROM ITRFiling f WHERE f.client.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM ITRFiling f WHERE f.client.user.id = :userId AND f.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);
}
