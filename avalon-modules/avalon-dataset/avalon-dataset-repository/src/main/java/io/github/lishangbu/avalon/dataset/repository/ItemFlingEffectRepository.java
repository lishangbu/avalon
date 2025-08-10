package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.ItemFlingEffect;
import java.util.Optional;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * 道具投掷效果数据存储
 *
 * @author lishangbu
 * @since 2025/6/8
 */
@Repository
public interface ItemFlingEffectRepository
    extends ListPagingAndSortingRepository<ItemFlingEffect, Long> {
  /**
   * 根据分类查找并返回对应的道具投掷效果
   *
   * @param internalName 要查找的内部名称，不能为空。
   * @return 如果找到与指定类型匹配的道具投掷效果对象，返回一个包含该对象的Optional；如果没有找到，返回一个空的Optional。
   */
  Optional<ItemFlingEffect> findByInternalName(String internalName);

  /**
   * 根据分类名称查找并返回对应的道具投掷效果
   *
   * @param name 要查找的名称，不能为空。
   * @return 如果找到与指定类型匹配的道具投掷效果对象，返回一个包含该对象的Optional；如果没有找到，返回一个空的Optional。
   */
  Optional<ItemFlingEffect> findByName(String name);
}
