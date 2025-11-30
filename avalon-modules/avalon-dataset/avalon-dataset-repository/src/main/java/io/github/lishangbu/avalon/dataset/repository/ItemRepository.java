package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.Item;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * 道具(Item)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface ItemRepository
    extends ListCrudRepository<Item, Long>, ListPagingAndSortingRepository<Item, Long> {}
