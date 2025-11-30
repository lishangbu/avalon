package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.Type;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.ListQueryByExampleExecutor;
import org.springframework.stereotype.Repository;

/**
 * 属性(Type)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface TypeRepository
  extends ListCrudRepository<Type, Long>,
  ListPagingAndSortingRepository<Type, Long>,
  ListQueryByExampleExecutor<Type> {
}
