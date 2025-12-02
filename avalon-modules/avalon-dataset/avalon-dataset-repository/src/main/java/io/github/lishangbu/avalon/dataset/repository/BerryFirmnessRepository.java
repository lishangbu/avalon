package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.ListQueryByExampleExecutor;
import org.springframework.stereotype.Repository;

/**
 * 树果硬度(BerryFirmness)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface BerryFirmnessRepository
    extends ListCrudRepository<BerryFirmness, Long>,
        ListPagingAndSortingRepository<BerryFirmness, Long>,
        ListQueryByExampleExecutor<BerryFirmness> {}
