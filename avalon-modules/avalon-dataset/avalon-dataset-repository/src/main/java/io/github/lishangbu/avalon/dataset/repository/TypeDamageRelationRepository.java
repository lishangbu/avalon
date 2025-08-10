package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * 属性伤害关系数据存储
 *
 * @author lishangbu
 * @since 2025/5/21
 */
@Repository
public interface TypeDamageRelationRepository
    extends ListPagingAndSortingRepository<TypeDamageRelation, Long> {}
