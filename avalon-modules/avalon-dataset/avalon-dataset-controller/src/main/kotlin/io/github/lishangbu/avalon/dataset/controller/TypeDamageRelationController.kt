package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.service.TypeDamageRelationService
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 属性克制关系控制器。 */
@RestController
@RequestMapping("/type-damage-relation")
class TypeDamageRelationController(
    private val typeDamageRelationService: TypeDamageRelationService,
) {
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

    @PostMapping
    fun save(
        @RequestBody relation: TypeDamageRelation,
    ): TypeDamageRelation = typeDamageRelationService.save(relation)

    @PutMapping
    fun update(
        @RequestBody relation: TypeDamageRelation,
    ): TypeDamageRelation = typeDamageRelationService.update(relation)

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
