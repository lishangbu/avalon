package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.service.BerryService
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

/** 树果控制器。 */
@RestController
@RequestMapping("/berry")
class BerryController(
    private val berryService: BerryService,
) {
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

    @PostMapping
    fun save(
        @RequestBody berry: Berry,
    ): Berry = berryService.save(berry)

    @PutMapping
    fun update(
        @RequestBody berry: Berry,
    ): Berry = berryService.update(berry)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        berryService.removeById(id)
    }
}
