package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.EggGroup;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 蛋群数据存储
 *
 * @author lishangbu
 * @since 2025/4/15
 */
public interface EggGroupRepository extends JpaRepository<EggGroup, String> {
  /**
   * 根据分组查找并返回对应的蛋群数据
   *
   * @param group 要查找的分组，不能为空。
   * @return 如果找到与指定类型匹配的对象，返回一个包含该对象的Optional；如果没有找到，返回一个空的Optional。
   * @see EggGroupRepository#findById(Object)
   */
  Optional<EggGroup> findByGroup(String group);
}
