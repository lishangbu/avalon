package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.ItemCategory;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * 道具类别(ItemCategory)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface ItemCategoryRepository
    extends ListCrudRepository<ItemCategory, Integer>,
        ListPagingAndSortingRepository<ItemCategory, Integer> {}
