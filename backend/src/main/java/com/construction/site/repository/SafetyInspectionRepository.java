package com.construction.site.repository;

import com.construction.site.entity.SafetyInspection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SafetyInspectionRepository extends JpaRepository<SafetyInspection, Long> {

    Optional<SafetyInspection> findByNo(String no);

    List<SafetyInspection> findAllByOrderByIdAsc();

    List<SafetyInspection> findByYardIdOrderByIdDesc(Long yardId);
}
