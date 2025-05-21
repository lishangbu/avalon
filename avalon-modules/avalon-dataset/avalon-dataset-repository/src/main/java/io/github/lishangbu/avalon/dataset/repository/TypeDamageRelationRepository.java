package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.Type;
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 属性伤害关系数据存储
 *
 * @author lishangbu
 * @since 2025/5/21
 */
@Repository
public interface TypeDamageRelationRepository extends JpaRepository<TypeDamageRelation, Integer> {

  // @Query("SELECT tdr FROM TypeDamageRelation tdr WHERE tdr.attackerType = :attackerType AND
  // tdr.defenderType = :defenderType")
  Optional<TypeDamageRelation> findTypeDamageRelationByAttackerTypeAndDefenderType(
      Type attackerType, Type defenderType);
}
