package io.github.lishangbu.avalon.admin.service.dataset.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lishangbu.avalon.admin.service.dataset.TypeDamageRelationService;
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation;
import io.github.lishangbu.avalon.dataset.mapper.TypeDamageRelationMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/// 属性克制关系服务实现
///
/// 提供属性间伤害倍率（克制）关系的分页查询与 CRUD 操作
///
/// @author lishangbu
/// @since 2025/12/06
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
}
