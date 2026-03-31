package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.SaveTypeInput
import io.github.lishangbu.avalon.dataset.entity.dto.TypeSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.TypeView
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateTypeInput
import io.github.lishangbu.avalon.dataset.repository.TypeRepository
import jakarta.validation.Valid
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.web.bind.annotation.*

/** 属性控制器 */
@RestController
@RequestMapping("/type")
class TypeController(
    /** 属性仓储 */
    private val typeRepository: TypeRepository,
) {
    /** 保存属性 */
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveTypeInput,
    ): TypeView = TypeView(typeRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    /** 更新属性 */
    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateTypeInput,
    ): TypeView = TypeView(typeRepository.save(command.toEntity(), SaveMode.UPSERT))

    /** 按 ID 删除属性 */
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        typeRepository.deleteById(id)
    }

    /** 查询属性列表 */
    @GetMapping("/list")
    fun listTypes(
        @ModelAttribute specification: TypeSpecification,
    ): List<TypeView> = typeRepository.listViews(specification)
}
