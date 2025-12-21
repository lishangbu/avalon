package io.github.lishangbu.avalon.admin.controller.dataset;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.lishangbu.avalon.admin.model.dataset.TypeDamageRelationMatrixResponse;
import io.github.lishangbu.avalon.admin.service.dataset.TypeDamageRelationService;
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
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
   * @param page 分页参数
   * @param typeDamageRelation 查询条件
   * @return 分页结果
   */
  @GetMapping("/page")
  public IPage<TypeDamageRelation> getTypeDamageRelationPage(
      Page<TypeDamageRelation> page, TypeDamageRelation typeDamageRelation) {
    return typeDamageRelationService.getTypeDamageRelationPage(page, typeDamageRelation);
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
      @PathVariable Long attackingTypeId, @PathVariable Long defendingTypeId) {
    return typeDamageRelationService.getByAttackingTypeIdAndDefendingTypeId(
        attackingTypeId, defendingTypeId);
  }

  /**
   * 根据复合主键删除实体
   *
   * @param attackingTypeId 攻击方类型内部名称
   * @param defendingTypeId 防御方类型内部名称
   */
  @DeleteMapping("/{attackingTypeId:\\d+}/{defendingTypeId:\\d+}")
  public void removeByAttackingTypeIdAndDefendingTypeId(
      @PathVariable Long attackingTypeId, @PathVariable Long defendingTypeId) {
    typeDamageRelationService.removeByAttackingTypeIdAndDefendingTypeId(
        attackingTypeId, defendingTypeId);
  }

  /**
   * 查询整个属性克制二维矩阵供前端表格直接渲染
   *
   * @return 按攻击属性分组的矩阵数据
   */
  @GetMapping("/matrix")
  public TypeDamageRelationMatrixResponse matrix() {
    return typeDamageRelationService.getMatrix();
  }
}
