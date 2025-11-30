package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.ItemAttribute;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * 道具属性(ItemAttribute)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface ItemAttributeRepository
    extends ListCrudRepository<ItemAttribute, Long>,
        ListPagingAndSortingRepository<ItemAttribute, Long> {}
