package io.github.lishangbu.avalon.dataset.controller;

import io.github.lishangbu.avalon.dataset.entity.Type;
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation;
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation.TypeDamageRelationId;
import io.github.lishangbu.avalon.dataset.service.TypeDamageRelationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/// 属性克制关系控制器
///
/// @author lishangbu
/// @since 2026/3/11
@RestController
@RequestMapping("/type-damage-relation")
@RequiredArgsConstructor
public class TypeDamageRelationController {
    private final TypeDamageRelationService typeDamageRelationService;

    /// 分页条件查询属性克制关系
    ///
    /// @param pageable         分页参数
    /// @param attackingTypeId  攻击方属性 ID
    /// @param defendingTypeId  防御方属性 ID
    /// @param multiplier       伤害倍率
    /// @return 属性克制关系分页结果
    @GetMapping("/page")
    public Page<TypeDamageRelation> getTypeDamageRelationPage(
            Pageable pageable,
            @RequestParam(required = false) Long attackingTypeId,
            @RequestParam(required = false) Long defendingTypeId,
            @RequestParam(required = false) Float multiplier) {
        return typeDamageRelationService.getPageByCondition(
                buildCondition(attackingTypeId, defendingTypeId, multiplier), pageable);
    }

    /// 条件查询属性克制关系列表
    ///
    /// @param attackingTypeId  攻击方属性 ID
    /// @param defendingTypeId  防御方属性 ID
    /// @param multiplier       伤害倍率
    /// @return 属性克制关系列表
    @GetMapping("/list")
    public List<TypeDamageRelation> listTypeDamageRelations(
            @RequestParam(required = false) Long attackingTypeId,
            @RequestParam(required = false) Long defendingTypeId,
            @RequestParam(required = false) Float multiplier) {
        return typeDamageRelationService.listByCondition(
                buildCondition(attackingTypeId, defendingTypeId, multiplier));
    }

    /// 新增属性克制关系
    ///
    /// @param relation 属性克制关系实体
    /// @return 保存后的属性克制关系
    @PostMapping
    public TypeDamageRelation save(@RequestBody TypeDamageRelation relation) {
        return typeDamageRelationService.save(relation);
    }

    /// 更新属性克制关系
    ///
    /// @param relation 属性克制关系实体
    /// @return 更新后的属性克制关系
    @PutMapping
    public TypeDamageRelation update(@RequestBody TypeDamageRelation relation) {
        return typeDamageRelationService.update(relation);
    }

    /// 根据主键删除属性克制关系
    ///
    /// @param attackingTypeId 攻击方属性 ID
    /// @param defendingTypeId 防御方属性 ID
    @DeleteMapping("/{attackingTypeId:\\d+}/{defendingTypeId:\\d+}")
    public void deleteById(@PathVariable Long attackingTypeId, @PathVariable Long defendingTypeId) {
        typeDamageRelationService.removeById(buildId(attackingTypeId, defendingTypeId));
    }

    private static TypeDamageRelation buildCondition(
            Long attackingTypeId, Long defendingTypeId, Float multiplier) {
        TypeDamageRelation relation = new TypeDamageRelation();
        if (attackingTypeId != null || defendingTypeId != null) {
            relation.setId(buildId(attackingTypeId, defendingTypeId));
        }
        if (multiplier != null) {
            relation.setMultiplier(multiplier);
        }
        return relation;
    }

    private static TypeDamageRelationId buildId(Long attackingTypeId, Long defendingTypeId) {
        TypeDamageRelationId id = new TypeDamageRelationId();
        if (attackingTypeId != null) {
            Type attackingType = new Type();
            attackingType.setId(attackingTypeId);
            id.setAttackingType(attackingType);
        }
        if (defendingTypeId != null) {
            Type defendingType = new Type();
            defendingType.setId(defendingTypeId);
            id.setDefendingType(defendingType);
        }
        return id;
    }
}
