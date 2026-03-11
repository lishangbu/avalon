package io.github.lishangbu.avalon.dataset.service;

import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation;
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation.TypeDamageRelationId;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/// 属性克制关系服务。
public interface TypeDamageRelationService {

    /// 根据条件分页查询属性克制关系。
    Page<TypeDamageRelation> getPageByCondition(TypeDamageRelation condition, Pageable pageable);

    /// 新增属性克制关系。
    TypeDamageRelation save(TypeDamageRelation relation);

    /// 更新属性克制关系。
    TypeDamageRelation update(TypeDamageRelation relation);

    /// 根据主键删除属性克制关系。
    void removeById(TypeDamageRelationId id);

    /// 根据条件查询属性克制关系列表。
    List<TypeDamageRelation> listByCondition(TypeDamageRelation condition);
}
