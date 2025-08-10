package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.MoveLearnMethod;
import java.util.Optional;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * 招式学习方法数据存储
 *
 * @author lishangbu
 * @since 2025/6/9
 */
@Repository
public interface MoveLearnMethodRepository
    extends ListPagingAndSortingRepository<MoveLearnMethod, Long> {
  /**
   * 根据内部名称查找并返回对应的招式学习方法数据
   *
   * @param internalName 要查找的内部名称，不能为空。
   * @return 如果找到与指定类型匹配的招式学习方法对象，返回一个包含该对象的Optional；如果没有找到，返回一个空的Optional。
   */
  Optional<MoveLearnMethod> findByInternalName(String internalName);

  /**
   * 根据名称查找并返回对应的招式学习方法数据
   *
   * @param name 要查找的名称，不能为空。
   * @return 如果找到与指定类型匹配的招式学习方法对象，返回一个包含该对象的Optional；如果没有找到，返回一个空的Optional。
   */
  Optional<MoveLearnMethod> findByName(String name);
}
