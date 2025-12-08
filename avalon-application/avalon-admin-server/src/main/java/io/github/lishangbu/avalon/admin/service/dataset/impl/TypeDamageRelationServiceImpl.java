package io.github.lishangbu.avalon.admin.service.dataset.impl;

import io.github.lishangbu.avalon.admin.model.dataset.TypeDamageRelationMatrixResponse;
import io.github.lishangbu.avalon.admin.service.dataset.TypeDamageRelationService;
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation;
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation.TypeDamageRelationId;
import io.github.lishangbu.avalon.dataset.repository.TypeDamageRelationRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
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

  /** {@inheritDoc} */
  @Override
  public TypeDamageRelationMatrixResponse getMatrix() {
    TypeDamageRelationMatrixResponse response = new TypeDamageRelationMatrixResponse();
    Map<Integer, TypeDamageRelationMatrixResponse.Row> rowMap = new TreeMap<>();
    typeDamageRelationRepository
        .findAll()
        .forEach(
            entity -> {
              rowMap
                  .computeIfAbsent(
                      entity.getAttackingTypeId(), TypeDamageRelationMatrixResponse.Row::new)
                  .getCells()
                  .add(
                      new TypeDamageRelationMatrixResponse.Cell(
                          entity.getDefendingTypeId(), toMultiplier(entity.getMultiplier())));
            });
    rowMap
        .values()
        .forEach(
            row ->
                row.getCells()
                    .sort(
                        Comparator.comparing(
                            TypeDamageRelationMatrixResponse.Cell::getDefendingTypeId)));
    response.getRows().addAll(rowMap.values());
    return response;
  }

  private BigDecimal toMultiplier(Float multiplier) {
    return multiplier == null ? BigDecimal.ONE : BigDecimal.valueOf(multiplier);
  }
}
