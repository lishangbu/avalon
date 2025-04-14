package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.Move;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 招式数据存储
 *
 * @author lishangbu
 * @since 2025/4/14
 */
@Repository
public interface MoveRepository extends JpaRepository<Move, Long> {}
