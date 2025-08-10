package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.Move;
import java.util.Optional;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * 招式数据存储
 *
 * @author lishangbu
 * @since 2025/4/14
 */
@Repository
public interface MoveRepository extends ListPagingAndSortingRepository<Move, Long> {

  /**
   * 根据分类查找并返回对应的招式
   *
   * @param internalName 要查找的内部名称，不能为空。
   * @return 如果找到与指定类型匹配的招式对象，返回一个包含该对象的Optional；如果没有找到，返回一个空的Optional。
   */
  Optional<Move> findByInternalName(String internalName);
}
