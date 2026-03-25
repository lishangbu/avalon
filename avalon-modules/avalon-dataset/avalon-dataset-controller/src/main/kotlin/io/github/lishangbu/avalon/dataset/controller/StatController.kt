package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass
import io.github.lishangbu.avalon.dataset.entity.Stat
import io.github.lishangbu.avalon.dataset.service.StatService
import org.springframework.web.bind.annotation.*

/** 能力值控制器 */
@RestController
@RequestMapping("/stat")
class StatController(
    /** 能力值服务*/
    private val statService: StatService,
) {
    /** 保存能力值*/
    @PostMapping
    fun save(
        @RequestBody stat: Stat,
    ): Stat = statService.save(stat)

    /** 更新能力值*/
    @PutMapping
    fun update(
        @RequestBody stat: Stat,
    ): Stat = statService.update(stat)

    /** 按 ID 删除能力值*/
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        statService.removeById(id)
    }

    /** 查询能力值列表*/
    @GetMapping("/list")
    fun listStats(
        @RequestParam(required = false) id: Long?,
        @RequestParam(required = false) internalName: String?,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) gameIndex: Int?,
        @RequestParam(required = false) battleOnly: Boolean?,
        @RequestParam(required = false) moveDamageClassId: Long?,
    ): List<Stat> =
        statService.listByCondition(
            Stat {
                id?.let { this.id = it }
                internalName?.let { this.internalName = it }
                name?.let { this.name = it }
                gameIndex?.let { this.gameIndex = it }
                battleOnly?.let { this.battleOnly = it }
                moveDamageClassId?.let { this.moveDamageClass = MoveDamageClass { this.id = it } }
            },
        )
}
