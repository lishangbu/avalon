package io.github.lishangbu.avalon.admin.controller.dataset;

import io.github.lishangbu.avalon.admin.service.dataset.TypeDamageRelationService;
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

/**
 * 属性克制关系控制器
 *
 * <p>提供属性克制关系的 REST API，直接通过 Service/Repository 操作数据
 *
 * @author lishangbu
 * @since 2025/12/06
 */
@RestController
@RequestMapping("/type-damage-relation")
@RequiredArgsConstructor
public class TypeDamageRelationController {
  private final TypeDamageRelationService typeDamageRelationService;

  /**
   * 分页查询属性克制关系
   *
   * @param pageable 分页参数
   * @param probe 查询条件
   * @return 分页结果
   */
  @GetMapping("/page")
  public Page<TypeDamageRelation> page(Pageable pageable, TypeDamageRelation probe) {
    return typeDamageRelationService.getPageByCondition(probe, pageable);
  }

  /**
   * 新增属性克制关系
   *
   * @param entity 要保存的实体
   * @return 保存后的实体
   */
  @PostMapping
  public TypeDamageRelation save(@RequestBody TypeDamageRelation entity) {
    return typeDamageRelationService.save(entity);
  }

  /**
   * 更新属性克制关系
   *
   * @param entity 要更新的实体
   * @return 更新后的实体
   */
  @PutMapping
  public TypeDamageRelation update(@RequestBody TypeDamageRelation entity) {
    return typeDamageRelationService.update(entity);
  }

  /**
   * 根据复合主键查询实体
   *
   * @param attackingTypeId 攻击方类型内部名称
   * @param defendingTypeId 防御方类型内部名称
   * @return 可选的结果
   */
  @GetMapping("/{attackingTypeId:\\d+}/{defendingTypeId:\\d+}")
  public Optional<TypeDamageRelation> getById(
      @PathVariable Integer attackingTypeId, @PathVariable Integer defendingTypeId) {
    return typeDamageRelationService.getById(attackingTypeId, defendingTypeId);
  }

  /**
   * 根据复合主键删除实体
   *
   * @param attackingTypeId 攻击方类型内部名称
   * @param defendingTypeId 防御方类型内部名称
   */
  @DeleteMapping("/{attackingTypeId:\\d+}/{defendingTypeId:\\d+}")
  public void deleteById(
      @PathVariable Integer attackingTypeId, @PathVariable Integer defendingTypeId) {
    typeDamageRelationService.removeById(attackingTypeId, defendingTypeId);
  }
}
