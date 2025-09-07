package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 树果风味(BerryFlavor)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface BerryFlavorRepository extends JpaRepository<BerryFlavor, Long> {}
