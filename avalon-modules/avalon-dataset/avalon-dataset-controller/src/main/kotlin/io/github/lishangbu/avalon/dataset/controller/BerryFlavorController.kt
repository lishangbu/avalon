package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFlavorSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFlavorView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveBerryFlavorInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateBerryFlavorInput
import io.github.lishangbu.avalon.dataset.repository.BerryFlavorRepository
import jakarta.validation.Valid
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.springframework.web.bind.annotation.*

/** 树果风味控制器*/
@RestController
@RequestMapping("/berry-flavor")
class BerryFlavorController(
    /** 树果风味仓储 */
    private val berryFlavorRepository: BerryFlavorRepository,
) {
    /** 查询树果风味列表 */
    @GetMapping("/list")
    fun listBerryFlavors(
        @ModelAttribute specification: BerryFlavorSpecification,
    ): List<BerryFlavorView> = berryFlavorRepository.listViews(specification)

    /** 保存树果风味 */
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveBerryFlavorInput,
    ): BerryFlavorView = berryFlavorRepository.save(command.toEntity(), SaveMode.INSERT_ONLY).let(::reloadView)

    /** 更新树果风味 */
    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateBerryFlavorInput,
    ): BerryFlavorView = berryFlavorRepository.save(command.toEntity(), SaveMode.UPSERT).let(::reloadView)

    /** 按 ID 删除树果风味 */
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        berryFlavorRepository.deleteById(id)
    }

    private fun reloadView(berryFlavor: BerryFlavor): BerryFlavorView = requireNotNull(berryFlavorRepository.loadViewById(berryFlavor.id)) { "未找到 ID=${berryFlavor.id} 对应的树果风味" }
}
