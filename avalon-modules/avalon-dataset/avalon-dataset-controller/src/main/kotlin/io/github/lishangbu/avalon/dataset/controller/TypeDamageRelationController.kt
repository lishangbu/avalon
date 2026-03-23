package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelation
import io.github.lishangbu.avalon.dataset.entity.TypeDamageRelationId
import io.github.lishangbu.avalon.dataset.service.TypeDamageRelationService
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*

/** 属性克制关系控制器 */
@RestController
@RequestMapping("/type-damage-relation")
class TypeDamageRelationController(
    /** 属性克制关系服务*/
    private val typeDamageRelationService: TypeDamageRelationService,
) {
    /** 获取属性克制关系分页结果*/
    @GetMapping("/page")
    fun getTypeDamageRelationPage(
        pageable: Pageable,
        @RequestParam(required = false) attackingTypeId: Long?,
        @RequestParam(required = false) defendingTypeId: Long?,
        @RequestParam(required = false) multiplier: Float?,
    ): Page<TypeDamageRelation> =
        typeDamageRelationService.getPageByCondition(
            attackingTypeId = attackingTypeId,
            defendingTypeId = defendingTypeId,
            multiplier = multiplier,
            pageable = pageable,
        )

    /** 查询属性伤害关系列表*/
    @GetMapping("/list")
    fun listTypeDamageRelations(
        @RequestParam(required = false) attackingTypeId: Long?,
        @RequestParam(required = false) defendingTypeId: Long?,
        @RequestParam(required = false) multiplier: Float?,
    ): List<TypeDamageRelation> =
        typeDamageRelationService.listByCondition(
            attackingTypeId = attackingTypeId,
            defendingTypeId = defendingTypeId,
            multiplier = multiplier,
        )

    /** 保存属性克制关系*/
    @PostMapping
    fun save(
        @RequestBody relation: TypeDamageRelation,
    ): TypeDamageRelation = typeDamageRelationService.save(relation)

    /** 更新属性克制关系*/
    @PutMapping
    fun update(
        @RequestBody relation: TypeDamageRelation,
    ): TypeDamageRelation = typeDamageRelationService.update(relation)

    /** 按 ID 删除属性克制关系*/
    @DeleteMapping("/{attackingTypeId:\\d+}/{defendingTypeId:\\d+}")
    fun deleteById(
        @PathVariable attackingTypeId: Long,
        @PathVariable defendingTypeId: Long,
    ) {
        typeDamageRelationService.removeById(
            TypeDamageRelationId {
                this.attackingTypeId = attackingTypeId
                this.defendingTypeId = defendingTypeId
            },
        )
    }
}
