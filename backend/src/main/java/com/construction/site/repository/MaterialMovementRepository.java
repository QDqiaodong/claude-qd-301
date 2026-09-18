package com.construction.site.repository;

import com.construction.site.entity.MaterialMovement;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialMovementRepository extends JpaRepository<MaterialMovement, Long> {

    Optional<MaterialMovement> findByNo(String no);

    /** 流水按时间倒着看，最近的在最上面 */
    List<MaterialMovement> findAllByOrderByIdDesc();

    List<MaterialMovement> findByMaterialIdOrderByIdDesc(Long materialId);

    List<MaterialMovement> findByDirectionOrderByIdDesc(String direction);
}
