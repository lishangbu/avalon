package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.MoveAilment;
import java.util.Optional;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * 招式导致的状态异常数据存储
 *
 * @author lishangbu
 * @since 2025/6/9
 */
@Repository
public interface MoveAilmentRepository extends ListPagingAndSortingRepository<MoveAilment, Long> {
  /**
   * 根据内部名称查找并返回招式导致的状态异常
   *
   * @param internalName 要查找的内部名称，不能为空
   * @return 如果找到与指定类型匹配的招式状态异常对象，返回一个包含该对象的Optional；如果没有找到，返回一个空的Optional
   */
  Optional<MoveAilment> findByInternalName(String internalName);
}
