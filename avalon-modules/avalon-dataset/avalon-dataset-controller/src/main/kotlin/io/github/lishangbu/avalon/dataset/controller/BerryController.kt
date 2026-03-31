package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.dto.BerrySpecification
import io.github.lishangbu.avalon.dataset.entity.dto.BerryView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveBerryInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateBerryInput
import io.github.lishangbu.avalon.dataset.service.BerryService
import jakarta.validation.Valid
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
        @ModelAttribute specification: BerrySpecification,
    ): Page<BerryView> = berryService.getPageByCondition(specification, pageable)

    /** 创建树果 */
    @PostMapping
    fun save(
        @Valid
        @RequestBody command: SaveBerryInput,
    ): BerryView = berryService.save(command)

    /** 更新树果 */
    @PutMapping
    fun update(
        @Valid
        @RequestBody command: UpdateBerryInput,
    ): BerryView = berryService.update(command)

    /** 删除指定 ID 的树果*/
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        berryService.removeById(id)
    }
}
