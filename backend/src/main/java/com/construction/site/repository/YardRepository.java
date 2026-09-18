package com.construction.site.repository;

import com.construction.site.entity.Yard;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface YardRepository extends JpaRepository<Yard, Long> {

    Optional<Yard> findByNo(String no);

    List<Yard> findAllByOrderByIdAsc();

    /** 这个堆场下还堆着多少条材料 */
    @Query("select count(m) from Material m where m.yardId = :yardId")
    long countMaterialsIn(@Param("yardId") Long yardId);
}
