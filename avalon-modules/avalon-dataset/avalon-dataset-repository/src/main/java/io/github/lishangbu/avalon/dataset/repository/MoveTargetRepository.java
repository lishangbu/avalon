package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.MoveTarget;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 战斗目标数据存储
 *
 * @author lishangbu
 * @since 2025/6/9
 */
@Repository
public interface MoveTargetRepository extends JpaRepository<MoveTarget, Integer> {
  /**
   * 根据内部名称查找并返回对应的战斗目标数据
   *
   * @param internalName 要查找的内部名称，不能为空。
   * @return 如果找到与指定类型匹配的战斗目标对象，返回一个包含该对象的Optional；如果没有找到，返回一个空的Optional。
   */
  Optional<MoveTarget> findByInternalName(String internalName);

  /**
   * 根据名称查找并返回对应的战斗目标数据
   *
   * @param name 要查找的名称，不能为空。
   * @return 如果找到与指定类型匹配的战斗目标对象，返回一个包含该对象的Optional；如果没有找到，返回一个空的Optional。
   */
  Optional<MoveTarget> findByName(String name);
}
