package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.EggGroup;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * 蛋组(EggGroup)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface EggGroupRepository
    extends ListCrudRepository<EggGroup, Integer>,
        ListPagingAndSortingRepository<EggGroup, Integer> {}
