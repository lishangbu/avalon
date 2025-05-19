package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.MoveCategory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 招式分类数据存储
 *
 * @author lishangbu
 * @since 2025/4/14
 */
@Repository
public interface MoveCategoryRepository extends JpaRepository<MoveCategory, Integer> {

  /**
   * 根据分类查找并返回对应的招式分类数据
   *
   * @param internalName 要查找的分类，不能为空。
   * @return 如果找到与指定类型匹配的招式分类对象，返回一个包含该对象的Optional；如果没有找到，返回一个空的Optional。
   */
  Optional<MoveCategory> findByInternalName(String internalName);

  /**
   * 根据分类名称查找并返回对应的招式分类数据
   *
   * @param name 要查找的名称，不能为空。
   * @return 如果找到与指定类型匹配的招式分类对象，返回一个包含该对象的Optional；如果没有找到，返回一个空的Optional。
   */
  Optional<MoveCategory> findByName(String name);
}
