package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.stereotype.Repository;

/**
 * 树果风味(BerryFlavor)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface BerryFlavorRepository
    extends ListCrudRepository<BerryFlavor, Long>,
        ListPagingAndSortingRepository<BerryFlavor, Long>,
        QueryByExampleExecutor<BerryFlavor> {}
