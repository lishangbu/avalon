package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.service.TypeEffectivenessChart
import io.github.lishangbu.avalon.dataset.service.TypeEffectivenessResult
import io.github.lishangbu.avalon.dataset.service.TypeEffectivenessService
import io.github.lishangbu.avalon.dataset.service.UpsertTypeEffectivenessMatrixCommand
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 属性相克业务控制器 */
@RestController
@RequestMapping("/type-effectiveness")
class TypeEffectivenessController(
    private val typeEffectivenessService: TypeEffectivenessService,
) {
    /** 计算攻击属性对一个或两个防守属性的最终倍率 */
    @GetMapping
    fun calculate(
        @RequestParam attacking: String,
        @RequestParam defending: List<String>,
    ): TypeEffectivenessResult = typeEffectivenessService.calculate(attacking, defending)

    /** 获取前端可直接渲染的完整相克矩阵 */
    @GetMapping("/chart")
    fun getChart(): TypeEffectivenessChart = typeEffectivenessService.getChart()

    /** 批量更新相克矩阵中的若干单元格 */
    @PutMapping("/matrix")
    fun upsertMatrix(
        @RequestBody command: UpsertTypeEffectivenessMatrixCommand,
    ): TypeEffectivenessChart = typeEffectivenessService.upsertMatrix(command)
}
