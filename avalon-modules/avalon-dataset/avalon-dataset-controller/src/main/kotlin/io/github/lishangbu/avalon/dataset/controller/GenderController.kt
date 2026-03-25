package io.github.lishangbu.avalon.dataset.controller

import io.github.lishangbu.avalon.dataset.entity.Gender
import io.github.lishangbu.avalon.dataset.entity.dto.GenderSpecification
import io.github.lishangbu.avalon.dataset.service.GenderService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** 性别控制器 */
@RestController
@RequestMapping("/gender")
class GenderController(
    /** 性别服务 */
    private val genderService: GenderService,
) {
    /** 保存性别 */
    @PostMapping
    fun save(
        @RequestBody gender: Gender,
    ): Gender = genderService.save(gender)

    /** 更新性别 */
    @PutMapping
    fun update(
        @RequestBody gender: Gender,
    ): Gender = genderService.update(gender)

    /** 按 ID 删除性别 */
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        genderService.removeById(id)
    }

    /** 查询性别列表 */
    @GetMapping("/list")
    fun listGenders(
        @ModelAttribute specification: GenderSpecification,
    ): List<Gender> = genderService.listByCondition(specification)
}
