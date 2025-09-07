package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 属性克制关系(TypeDamageRelation)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface TypeDamageRelationRepository
    extends JpaRepository<TypeDamageRelation, TypeDamageRelation.TypeDamageRelationId> {}
