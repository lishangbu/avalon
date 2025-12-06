package io.github.lishangbu.avalon.admin.service.dataset.impl;

import io.github.lishangbu.avalon.admin.service.dataset.TypeDamageRelationService;
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation;
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation.TypeDamageRelationId;
import io.github.lishangbu.avalon.dataset.repository.TypeDamageRelationRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 属性克制关系服务实现
 *
 * @author lishangbu
 * @since 2025/12/06
 */
@Service
@RequiredArgsConstructor
public class TypeDamageRelationServiceImpl implements TypeDamageRelationService {
  private final TypeDamageRelationRepository typeDamageRelationRepository;

  private final JdbcAggregateTemplate jdbcAggregateTemplate;

  @Override
  public Page<TypeDamageRelation> getPageByCondition(TypeDamageRelation probe, Pageable pageable) {
    return typeDamageRelationRepository.findAll(Example.of(probe), pageable);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TypeDamageRelation save(TypeDamageRelation entity) {
    TypeDamageRelationId id = new TypeDamageRelationId();
    id.setAttackingTypeId(entity.getAttackingTypeId());
    id.setDefendingTypeId(entity.getDefendingTypeId());
    entity.setId(id);
    return jdbcAggregateTemplate.insert(entity);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void removeById(Integer attackingTypeId, Integer defendingTypeId) {
    TypeDamageRelationId id = new TypeDamageRelationId();
    id.setAttackingTypeId(attackingTypeId);
    id.setDefendingTypeId(defendingTypeId);
    typeDamageRelationRepository.deleteById(id);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TypeDamageRelation update(TypeDamageRelation entity) {
    TypeDamageRelationId id = new TypeDamageRelationId();
    id.setAttackingTypeId(entity.getAttackingTypeId());
    id.setDefendingTypeId(entity.getDefendingTypeId());
    entity.setId(id);
    return typeDamageRelationRepository.save(entity);
  }

  @Override
  public Optional<TypeDamageRelation> getById(Integer attackingTypeId, Integer defendingTypeId) {
    TypeDamageRelationId id = new TypeDamageRelationId();
    id.setAttackingTypeId(attackingTypeId);
    id.setDefendingTypeId(defendingTypeId);
    return typeDamageRelationRepository.findById(id);
  }
}
