package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.Type;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 属性数据存储
 *
 * @author lishangbu
 * @since 2025/4/14
 */
@Repository
public interface TypeRepository extends JpaRepository<Type, String> {

  /**
   * 根据属性名查找并返回对应的属性数据
   *
   * @param type 要查找的类型，不能为空。
   * @return 如果找到与指定类型匹配的对象，返回一个包含该对象的Optional；如果没有找到，返回一个空的Optional。
   * @see TypeRepository#findById(Object)
   */
  Optional<Type> findByType(String type);
}
