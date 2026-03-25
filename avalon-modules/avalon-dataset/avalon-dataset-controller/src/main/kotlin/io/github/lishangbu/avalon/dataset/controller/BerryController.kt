package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.Berry
import io.github.lishangbu.avalon.dataset.entity.dto.BerrySpecification
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
        @ModelAttribute specification: BerrySpecification,
    ): Page<Berry> = berryService.getPageByCondition(specification, pageable)

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
