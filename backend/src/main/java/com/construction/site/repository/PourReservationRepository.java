package com.construction.site.repository;

import com.construction.site.entity.PourReservation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PourReservationRepository extends JpaRepository<PourReservation, Long> {

    Optional<PourReservation> findByNo(String no);

    /** 预扣单按时间倒着看，最近的在最上面 */
    List<PourReservation> findAllByOrderByIdDesc();

    /** 某条材料被「占用中」预扣占走的总量；一条都没有时 sum 出来是 null，用的人自己兜一下 */
    @Query("select sum(r.amount) from PourReservation r where r.materialId = :materialId and r.state = '占用中'")
    Long sumOccupiedByMaterialId(@Param("materialId") Long materialId);

    /** 全工地每种材料被「占用中」预扣占走的量，给材料台账一次算齐 */
    @Query("select r.materialId, sum(r.amount) from PourReservation r where r.state = '占用中' group by r.materialId")
    List<Object[]> sumOccupiedGroupByMaterial();
}
