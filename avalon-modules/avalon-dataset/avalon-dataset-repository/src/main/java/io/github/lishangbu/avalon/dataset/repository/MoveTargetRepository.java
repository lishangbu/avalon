package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.MoveTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 招式目标(MoveTarget)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface MoveTargetRepository extends JpaRepository<MoveTarget, Long> {}
