package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.ItemAttributeRelation;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * 道具属性关联(ItemAttributeRelation)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface ItemAttributeRelationRepository
    extends ListCrudRepository<ItemAttributeRelation, Integer>,
        ListPagingAndSortingRepository<ItemAttributeRelation, Integer> {}
