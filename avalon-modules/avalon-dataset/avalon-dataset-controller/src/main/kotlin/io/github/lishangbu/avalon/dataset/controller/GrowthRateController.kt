package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.GrowthRate
import io.github.lishangbu.avalon.dataset.entity.dto.GrowthRateSpecification
import io.github.lishangbu.avalon.dataset.service.GrowthRateService
import org.springframework.web.bind.annotation.*

/** 成长速率控制器 */
@RestController
@RequestMapping("/growth-rate")
class GrowthRateController(
    /** 成长速率服务 */
    private val growthRateService: GrowthRateService,
) {
    /** 保存成长速率 */
    @PostMapping
    fun save(
        @RequestBody growthRate: GrowthRate,
    ): GrowthRate = growthRateService.save(growthRate)

    /** 更新成长速率 */
    @PutMapping
    fun update(
        @RequestBody growthRate: GrowthRate,
    ): GrowthRate = growthRateService.update(growthRate)

    /** 按 ID 删除成长速率 */
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        growthRateService.removeById(id)
    }

    /** 查询成长速率列表 */
    @GetMapping("/list")
    fun listGrowthRates(
        @ModelAttribute specification: GrowthRateSpecification,
    ): List<GrowthRate> = growthRateService.listByCondition(specification)
}
