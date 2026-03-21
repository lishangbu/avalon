package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.service.StatService
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

/** 能力控制器。 */
@RestController
@RequestMapping("/stat")
class StatController(
    private val statService: StatService,
) {
    @GetMapping("/page")
    fun getStatPage(
        pageable: Pageable,
        @RequestParam(required = false) id: Long?,
        @RequestParam(required = false) internalName: String?,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) gameIndex: Int?,
        @RequestParam(required = false) isBattleOnly: Boolean?,
        @RequestParam(required = false) moveDamageClassId: Long?,
    ): Page<Stat> =
        statService.getPageByCondition(
            Stat {
                id?.let { this.id = it }
                internalName?.let { this.internalName = it }
                name?.let { this.name = it }
                gameIndex?.let { this.gameIndex = it }
                isBattleOnly?.let { this.isBattleOnly = it }
                moveDamageClassId?.let { this.moveDamageClass = MoveDamageClass { this.id = it } }
            },
            pageable,
        )

    @PostMapping
    fun save(
        @RequestBody stat: Stat,
    ): Stat = statService.save(stat)

    @PutMapping
    fun update(
        @RequestBody stat: Stat,
    ): Stat = statService.update(stat)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        statService.removeById(id)
    }

    @GetMapping("/list")
    fun listStats(
        @RequestParam(required = false) id: Long?,
        @RequestParam(required = false) internalName: String?,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) gameIndex: Int?,
        @RequestParam(required = false) isBattleOnly: Boolean?,
        @RequestParam(required = false) moveDamageClassId: Long?,
    ): List<Stat> =
        statService.listByCondition(
            Stat {
                id?.let { this.id = it }
                internalName?.let { this.internalName = it }
                name?.let { this.name = it }
                gameIndex?.let { this.gameIndex = it }
                isBattleOnly?.let { this.isBattleOnly = it }
                moveDamageClassId?.let { this.moveDamageClass = MoveDamageClass { this.id = it } }
            },
        )
}
