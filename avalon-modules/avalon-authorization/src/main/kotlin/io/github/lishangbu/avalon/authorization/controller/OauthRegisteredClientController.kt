package io.github.lishangbu.avalon.authorization.controller

import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient
import io.github.lishangbu.avalon.authorization.service.OauthRegisteredClientService
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*

/**
 * OAuth2 注册客户端控制器
 *
 * 提供 OAuth2 注册客户端管理相关接口
 *
 * @author lishangbu
 * @since 2026/3/19
 */
@RestController
@RequestMapping("/oauth-registered-client")
class OauthRegisteredClientController(
    private val oauthRegisteredClientService: OauthRegisteredClientService,
) {
    /**
     * 分页条件查询注册客户端
     *
     * @param pageable 分页参数
     * @param registeredClient 查询条件
     * @return 注册客户端分页结果
     */
    @GetMapping("/page")
    fun getPage(
        pageable: Pageable,
        registeredClient: OauthRegisteredClient,
    ): Page<OauthRegisteredClient> = oauthRegisteredClientService.getPageByCondition(registeredClient, pageable)

    /**
     * 条件查询注册客户端列表
     *
     * @param registeredClient 查询条件
     * @return 注册客户端列表
     */
    @GetMapping("/list")
    fun list(registeredClient: OauthRegisteredClient): List<OauthRegisteredClient> = oauthRegisteredClientService.listByCondition(registeredClient)

    /**
     * 根据 ID 查询注册客户端
     *
     * @param id 主键
     * @return 注册客户端
     */
    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: String,
    ): OauthRegisteredClient? = oauthRegisteredClientService.getById(id)

    /**
     * 新增注册客户端
     *
     * @param registeredClient 注册客户端实体
     * @return 保存后的注册客户端
     */
    @PostMapping
    fun save(
        @RequestBody registeredClient: OauthRegisteredClient,
    ): OauthRegisteredClient = oauthRegisteredClientService.save(registeredClient)

    /**
     * 更新注册客户端
     *
     * @param registeredClient 注册客户端实体
     * @return 更新后的注册客户端
     */
    @PutMapping
    fun update(
        @RequestBody registeredClient: OauthRegisteredClient,
    ): OauthRegisteredClient = oauthRegisteredClientService.update(registeredClient)

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
