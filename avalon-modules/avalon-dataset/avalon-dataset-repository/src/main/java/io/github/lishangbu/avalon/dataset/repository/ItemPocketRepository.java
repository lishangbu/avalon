package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.ItemPocket;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * 道具口袋(ItemPocket)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface ItemPocketRepository
    extends ListCrudRepository<ItemPocket, Integer>,
        ListPagingAndSortingRepository<ItemPocket, Integer> {}
