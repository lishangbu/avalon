package io.github.lishangbu.avalon.authorization.controller

import io.github.lishangbu.avalon.authorization.entity.dto.CurrentUserView
import io.github.lishangbu.avalon.authorization.entity.dto.SaveUserInput
import io.github.lishangbu.avalon.authorization.entity.dto.UpdateUserInput
import io.github.lishangbu.avalon.authorization.entity.dto.UserSpecification
import io.github.lishangbu.avalon.authorization.entity.dto.UserView
import io.github.lishangbu.avalon.authorization.service.UserService
import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo
import jakarta.validation.Valid
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 用户控制器
 *
 * @author lishangbu
 * @since 2025/8/30
 */
@RequestMapping("/user")
@RestController
class UserController(
    /** 用户服务 */
    private val userService: UserService,
) {
    /**
     * 获取当前用户信息
     *
     * @param user 当前用户信息
     * @return 用户信息
     */
    @GetMapping("/info")
    fun getUserInfo(
        @AuthenticationPrincipal user: UserInfo,
    ): CurrentUserView? = userService.getUserByUsername(user.username)

    /**
     * 分页条件查询用户
     *
     * @param pageable 分页参数
     * @param user 查询条件
     * @return 用户分页结果
     */
    @GetMapping("/page")
    fun getUserPage(
        pageable: Pageable,
        @ModelAttribute specification: UserSpecification,
    ): Page<UserView> = userService.getPageByCondition(specification, pageable)

    /**
     * 条件查询用户列表
     *
     * @param user 查询条件
     * @return 用户列表
     */
    @GetMapping("/list")
    fun listUsers(
        @ModelAttribute specification: UserSpecification,
    ): List<UserView> = userService.listByCondition(specification)

    /**
     * 根据 ID 查询用户
     *
     * @param id 用户 ID
     * @return 用户信息
     */
    @GetMapping("/{id:\\d+}")
    fun getById(
        @PathVariable id: Long,
    ): UserView? = userService.getById(id)

    /**
     * 新增用户
     *
     * @param input 用户写入请求
     * @return 保存后的用户
     */
    @PostMapping
    fun save(
        @RequestBody @Valid input: SaveUserInput,
    ): UserView = userService.save(input)

    /**
     * 更新用户
     *
     * @param input 用户写入请求
     * @return 更新后的用户
     */
    @PutMapping
    fun update(
        @RequestBody @Valid input: UpdateUserInput,
    ): UserView = userService.update(input)

    /**
     * 根据 ID 删除用户
     *
     * @param id 用户 ID
     */
    @DeleteMapping("/{id:\\d+}")
    fun deleteById(
        @PathVariable id: Long,
    ) {
        userService.removeById(id)
    }
}
