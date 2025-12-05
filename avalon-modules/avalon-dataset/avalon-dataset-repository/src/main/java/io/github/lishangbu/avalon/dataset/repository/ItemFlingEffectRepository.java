package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.ItemFlingEffect;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * 道具投掷效果(ItemFlingEffect)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface ItemFlingEffectRepository
    extends ListCrudRepository<ItemFlingEffect, Integer>,
        ListPagingAndSortingRepository<ItemFlingEffect, Integer> {}
