package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.GrowthRateSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.GrowthRateView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveGrowthRateInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateGrowthRateInput
import io.github.lishangbu.avalon.dataset.repository.GrowthRateRepository
import jakarta.validation.Valid
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.web.bind.annotation.*

/** 成长速率控制器 */
@RestController
@RequestMapping("/growth-rate")
class GrowthRateController(
    /** 成长速率仓储 */
    private val growthRateRepository: GrowthRateRepository,
) {
    /** 保存成长速率 */
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveGrowthRateInput,
    ): GrowthRateView = GrowthRateView(growthRateRepository.save(command.toEntity(), SaveMode.INSERT_ONLY))

    /** 更新成长速率 */
    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateGrowthRateInput,
    ): GrowthRateView = GrowthRateView(growthRateRepository.save(command.toEntity(), SaveMode.UPSERT))

    /** 按 ID 删除成长速率 */
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        growthRateRepository.deleteById(id)
    }

    /** 查询成长速率列表 */
    @GetMapping("/list")
    fun listGrowthRates(
        @ModelAttribute specification: GrowthRateSpecification,
    ): List<GrowthRateView> = growthRateRepository.listViews(specification)
}
