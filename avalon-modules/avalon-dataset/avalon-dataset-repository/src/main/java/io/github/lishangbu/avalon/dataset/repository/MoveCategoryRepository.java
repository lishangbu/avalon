package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.MoveCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 招式类别(MoveCategory)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface MoveCategoryRepository extends JpaRepository<MoveCategory, Long> {}
