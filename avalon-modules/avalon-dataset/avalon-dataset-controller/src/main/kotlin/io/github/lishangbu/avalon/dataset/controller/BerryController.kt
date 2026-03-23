package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.Berry
import io.github.lishangbu.avalon.dataset.entity.BerryFirmness
import io.github.lishangbu.avalon.dataset.entity.Type
import io.github.lishangbu.avalon.dataset.service.BerryService
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*

/**
 * 树果管理控制器
 * 提供树果的分页查询、新增、更新和删除接口
 */
@RestController
@RequestMapping("/berry")
class BerryController(
    /** 树果应用服务 */
    private val berryService: BerryService,
) {
    /** 按筛选条件分页查询树果*/
    @GetMapping("/page")
    fun getBerryPage(
        pageable: Pageable,
        @RequestParam(required = false) id: Long?,
        @RequestParam(required = false) internalName: String?,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) growthTime: Int?,
        @RequestParam(required = false) maxHarvest: Int?,
        @RequestParam(required = false) bulk: Int?,
        @RequestParam(required = false) smoothness: Int?,
        @RequestParam(required = false) soilDryness: Int?,
        @RequestParam(required = false) berryFirmnessId: Long?,
        @RequestParam(required = false) naturalGiftTypeId: Long?,
        @RequestParam(required = false) naturalGiftPower: Int?,
    ): Page<Berry> =
        berryService.getPageByCondition(
            Berry {
                id?.let { this.id = it }
                internalName?.let { this.internalName = it }
                name?.let { this.name = it }
                growthTime?.let { this.growthTime = it }
                maxHarvest?.let { this.maxHarvest = it }
                bulk?.let { this.bulk = it }
                smoothness?.let { this.smoothness = it }
                soilDryness?.let { this.soilDryness = it }
                naturalGiftPower?.let { this.naturalGiftPower = it }
                berryFirmnessId?.let { this.berryFirmness = BerryFirmness { this.id = it } }
                naturalGiftTypeId?.let { this.naturalGiftType = Type { this.id = it } }
            },
            pageable,
        )

    /** 创建树果 */
    @PostMapping
    fun save(
        @RequestBody berry: Berry,
    ): Berry = berryService.save(berry)

    /** 更新树果 */
    @PutMapping
    fun update(
        @RequestBody berry: Berry,
    ): Berry = berryService.update(berry)

    /** 删除指定 ID 的树果*/
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        berryService.removeById(id)
    }
}
