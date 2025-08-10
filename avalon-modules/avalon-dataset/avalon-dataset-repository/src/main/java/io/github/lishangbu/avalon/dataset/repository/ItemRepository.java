package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.Item;
import java.util.Optional;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * 道具数据存储
 *
 * @author lishangbu
 * @since 2025/6/8
 */
@Repository
public interface ItemRepository extends ListPagingAndSortingRepository<Item, Long> {
  /**
   * 根据分类查找并返回对应的道具
   *
   * @param internalName 要查找的内部名称，不能为空。
   * @return 如果找到与指定类型匹配的道具对象，返回一个包含该对象的Optional；如果没有找到，返回一个空的Optional。
   */
  Optional<Item> findByInternalName(String internalName);

  /**
   * 根据分类名称查找并返回对应的道具
   *
   * @param name 要查找的名称，不能为空。
   * @return 如果找到与指定类型匹配的道具对象，返回一个包含该对象的Optional；如果没有找到，返回一个空的Optional。
   */
  Optional<Item> findByName(String name);
}
