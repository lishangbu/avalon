package io.github.lishangbu.avalon.admin.service.dataset.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lishangbu.avalon.admin.model.dataset.TypeDamageRelationMatrixResponse;
import io.github.lishangbu.avalon.admin.service.dataset.TypeDamageRelationService;
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation;
import io.github.lishangbu.avalon.dataset.mapper.TypeDamageRelationMapper;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
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
  private final TypeDamageRelationMapper typeDamageRelationMapper;

  @Override
  public IPage<TypeDamageRelation> getTypeDamageRelationPage(
      Page<TypeDamageRelation> page, TypeDamageRelation typeDamageRelation) {
    return typeDamageRelationMapper.selectList(page, typeDamageRelation);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TypeDamageRelation save(TypeDamageRelation typeDamageRelation) {
    typeDamageRelationMapper.insert(typeDamageRelation);
    return typeDamageRelation;
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void removeByAttackingTypeIdAndDefendingTypeId(
      Long attackingTypeId, Long defendingTypeId) {
    typeDamageRelationMapper.deleteByAttackingTypeIdAndDefendingTypeId(
        attackingTypeId, defendingTypeId);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public TypeDamageRelation update(TypeDamageRelation typeDamageRelation) {
    typeDamageRelationMapper.update(typeDamageRelation);
    return typeDamageRelation;
  }

  @Override
  public Optional<TypeDamageRelation> getByAttackingTypeIdAndDefendingTypeId(
      Long attackingTypeId, Long defendingTypeId) {
    return typeDamageRelationMapper.selectByAttackingTypeIdAndDefendingTypeId(
        attackingTypeId, defendingTypeId);
  }

  /** {@inheritDoc} */
  @Override
  public TypeDamageRelationMatrixResponse getMatrix() {
    TypeDamageRelationMatrixResponse response = new TypeDamageRelationMatrixResponse();
    Map<Long, TypeDamageRelationMatrixResponse.Row> rowMap = new TreeMap<>();
    typeDamageRelationMapper
        .selectList(null)
        .forEach(
            entity -> {
              rowMap
                  .computeIfAbsent(
                      entity.getAttackingTypeId(), TypeDamageRelationMatrixResponse.Row::new)
                  .getCells()
                  .add(
                      new TypeDamageRelationMatrixResponse.Cell(
                          entity.getDefendingTypeId(), entity.getMultiplier()));
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
}
