package io.github.lishangbu.avalon.authorization.controller

import io.github.lishangbu.avalon.authorization.entity.dto.OauthRegisteredClientSpecification
import io.github.lishangbu.avalon.authorization.entity.dto.OauthRegisteredClientView
import io.github.lishangbu.avalon.authorization.entity.dto.SaveOauthRegisteredClientInput
import io.github.lishangbu.avalon.authorization.entity.dto.UpdateOauthRegisteredClientInput
import io.github.lishangbu.avalon.authorization.service.OauthRegisteredClientService
import jakarta.validation.Valid
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*

/**
 * OAuth2 注册客户端控制器
 *
 * 提供 OAuth2 注册客户端管理接口
 *
 * @author lishangbu
 * @since 2026/3/19
 */
@RestController
@RequestMapping("/oauth-registered-client")
class OauthRegisteredClientController(
    /** OAuth2 注册客户端服务 */
    private val oauthRegisteredClientService: OauthRegisteredClientService,
) {
    /**
     * 分页条件查询注册客户端
     *
     * @param pageable 分页参数
     * @return 注册客户端分页结果
     */
    @GetMapping("/page")
    fun getPage(
        pageable: Pageable,
        @ModelAttribute specification: OauthRegisteredClientSpecification,
    ): Page<OauthRegisteredClientView> = oauthRegisteredClientService.getPageByCondition(specification, pageable)

    /**
     * 条件查询注册客户端列表
     *
     * @return 注册客户端列表
     */
    @GetMapping("/list")
    fun list(
        @ModelAttribute specification: OauthRegisteredClientSpecification,
    ): List<OauthRegisteredClientView> = oauthRegisteredClientService.listByCondition(specification)

    /**
     * 根据 ID 查询注册客户端
     *
     * @param id 主键
     * @return 注册客户端
     */
    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: String,
    ): OauthRegisteredClientView? = oauthRegisteredClientService.getById(id)

    /**
     * 新增注册客户端
     *
     * @param input 注册客户端写入请求
     * @return 保存后的注册客户端
     */
    @PostMapping
    fun save(
        @RequestBody @Valid input: SaveOauthRegisteredClientInput,
    ): OauthRegisteredClientView = oauthRegisteredClientService.save(input)

    /**
     * 更新注册客户端
     *
     * @param input 注册客户端写入请求
     * @return 更新后的注册客户端
     */
    @PutMapping
    fun update(
        @RequestBody @Valid input: UpdateOauthRegisteredClientInput,
    ): OauthRegisteredClientView = oauthRegisteredClientService.update(input)

    /**
     * 根据 ID 删除注册客户端
     *
     * @param id 主键
     */
    @DeleteMapping("/{id}")
    fun deleteById(
        @PathVariable id: String,
    ) {
        oauthRegisteredClientService.removeById(id)
    }
}
