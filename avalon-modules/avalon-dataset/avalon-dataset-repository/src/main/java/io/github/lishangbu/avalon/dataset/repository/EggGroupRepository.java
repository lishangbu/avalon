package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.EggGroup;
import java.util.Optional;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * 蛋群数据存储
 *
 * @author lishangbu
 * @since 2025/4/15
 */
@Repository
public interface EggGroupRepository
    extends ListCrudRepository<EggGroup, Long>, ListPagingAndSortingRepository<EggGroup, Long> {
  /**
   * 根据内部名称查找并返回对应的蛋群数据
   *
   * @param internalName 要查找的内部名称，不能为空。
   * @return 如果找到与指定类型匹配的对象，返回一个包含该对象的Optional；如果没有找到，返回一个空的Optional。
   */
  Optional<EggGroup> findByInternalName(String internalName);
}
