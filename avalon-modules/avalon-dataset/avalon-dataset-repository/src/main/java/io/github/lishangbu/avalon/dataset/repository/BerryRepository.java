package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.Berry;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.stereotype.Repository;

/**
 * 树果(Berry)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface BerryRepository
    extends ListCrudRepository<Berry, Long>,
        ListPagingAndSortingRepository<Berry, Long>,
        QueryByExampleExecutor<Berry> {}
