package com.construction.site.repository;

import com.construction.site.entity.SafetyInspection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SafetyInspectionRepository extends JpaRepository<SafetyInspection, Long> {

    Optional<SafetyInspection> findByNo(String no);

    List<SafetyInspection> findAllByOrderByIdAsc();

    List<SafetyInspection> findByYardIdOrderByIdDesc(Long yardId);

    /** 这个堆场是不是还挂着「不合格 + 待整改」的巡检 —— 挂着就不许新开预扣、不许把预扣改大 */
    boolean existsByYardIdAndVerdictAndState(Long yardId, String verdict, String state);
}
