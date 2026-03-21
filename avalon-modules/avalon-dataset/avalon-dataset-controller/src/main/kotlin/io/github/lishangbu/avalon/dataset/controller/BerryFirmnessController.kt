package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.service.BerryFirmnessService
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

/** 树果坚硬度控制器。 */
@RestController
@RequestMapping("/berry-firmness")
class BerryFirmnessController(
    private val berryFirmnessService: BerryFirmnessService,
) {
    @GetMapping("/page")
    fun getBerryFirmnessPage(
        pageable: Pageable,
        @RequestParam(required = false) id: Long?,
        @RequestParam(required = false) internalName: String?,
        @RequestParam(required = false) name: String?,
    ): Page<BerryFirmness> =
        berryFirmnessService.getPageByCondition(
            BerryFirmness {
                id?.let { this.id = it }
                internalName?.let { this.internalName = it }
                name?.let { this.name = it }
            },
            pageable,
        )

    @PostMapping
    fun save(
        @RequestBody berryFirmness: BerryFirmness,
    ): BerryFirmness = berryFirmnessService.save(berryFirmness)

    @PutMapping
    fun update(
        @RequestBody berryFirmness: BerryFirmness,
    ): BerryFirmness = berryFirmnessService.update(berryFirmness)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        berryFirmnessService.removeById(id)
    }

    @GetMapping("/list")
    fun listBerryFirmnesses(
        @RequestParam(required = false) id: Long?,
        @RequestParam(required = false) internalName: String?,
        @RequestParam(required = false) name: String?,
    ): List<BerryFirmness> =
        berryFirmnessService.listByCondition(
            BerryFirmness {
                id?.let { this.id = it }
                internalName?.let { this.internalName = it }
                name?.let { this.name = it }
            },
        )
}
