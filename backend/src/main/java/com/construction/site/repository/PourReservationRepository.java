package com.construction.site.repository;

import com.construction.site.entity.PourReservation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PourReservationRepository extends JpaRepository<PourReservation, Long> {

    Optional<PourReservation> findByNo(String no);

    /** 预扣单按时间倒着看，最近开的在最上面 */
    List<PourReservation> findAllByOrderByIdDesc();

    List<PourReservation> findByState(String state);

    /** 某批材料在某个状态下的预扣 —— 算「占用中」合计就靠它 */
    List<PourReservation> findByMaterialIdAndState(Long materialId, String state);
}
