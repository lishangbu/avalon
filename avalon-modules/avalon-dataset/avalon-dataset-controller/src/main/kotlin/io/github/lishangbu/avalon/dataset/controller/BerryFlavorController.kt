package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.service.BerryFlavorService
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

/** 树果风味控制器。 */
@RestController
@RequestMapping("/berry-flavor")
class BerryFlavorController(
    private val berryFlavorService: BerryFlavorService,
) {
    @GetMapping("/page")
    fun getBerryFlavorPage(
        pageable: Pageable,
        @RequestParam(required = false) id: Long?,
        @RequestParam(required = false) internalName: String?,
        @RequestParam(required = false) name: String?,
    ): Page<BerryFlavor> =
        berryFlavorService.getPageByCondition(
            BerryFlavor {
                id?.let { this.id = it }
                internalName?.let { this.internalName = it }
                name?.let { this.name = it }
            },
            pageable,
        )

    @PostMapping
    fun save(
        @RequestBody berryFlavor: BerryFlavor,
    ): BerryFlavor = berryFlavorService.save(berryFlavor)

    @PutMapping
    fun update(
        @RequestBody berryFlavor: BerryFlavor,
    ): BerryFlavor = berryFlavorService.update(berryFlavor)

    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        berryFlavorService.removeById(id)
    }
}
