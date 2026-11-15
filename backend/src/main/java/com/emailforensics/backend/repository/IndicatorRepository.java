package com.emailforensics.backend.repository;

import com.emailforensics.backend.entity.Indicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IndicatorRepository extends JpaRepository<Indicator, Long> {

    List<Indicator> findByEmailId(Long emailId);

    List<Indicator> findByTypeAndNormalizedValue(String type, String normalizedValue);
}
