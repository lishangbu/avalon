package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.GenderSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.GenderView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveGenderInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateGenderInput
import io.github.lishangbu.avalon.dataset.repository.GenderRepository
import jakarta.validation.Valid
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 性别控制器 */
@RestController
@RequestMapping("/gender")
class GenderController(
    /** 性别仓储 */
    private val genderRepository: GenderRepository,
) {
    /** 保存性别 */
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveGenderInput,
    ): GenderView = GenderView(genderRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    /** 更新性别 */
    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateGenderInput,
    ): GenderView = GenderView(genderRepository.save(command.toEntity(), SaveMode.UPSERT))

    /** 按 ID 删除性别 */
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        genderRepository.deleteById(id)
    }

    /** 查询性别列表 */
    @GetMapping("/list")
    fun listGenders(
        @ModelAttribute specification: GenderSpecification,
    ): List<GenderView> = genderRepository.listViews(specification)
}
