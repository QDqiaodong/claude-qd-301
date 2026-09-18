package com.construction.site.repository;

import com.construction.site.entity.Material;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    Optional<Material> findByNo(String no);

    List<Material> findAllByOrderByIdAsc();

    List<Material> findByYardIdOrderByIdAsc(Long yardId);
}
