package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.Berry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 树果(Berry)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface BerryRepository extends JpaRepository<Berry, Long> {}
