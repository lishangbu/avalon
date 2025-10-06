package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.Type;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 属性(Type)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface TypeRepository extends JpaRepository<Type, Long> {
}
