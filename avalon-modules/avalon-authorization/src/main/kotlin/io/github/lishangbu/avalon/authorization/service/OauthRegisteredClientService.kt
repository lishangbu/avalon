package io.github.lishangbu.avalon.authorization.service

import io.github.lishangbu.avalon.authorization.entity.dto.OauthRegisteredClientSpecification
import io.github.lishangbu.avalon.authorization.entity.dto.OauthRegisteredClientView
import io.github.lishangbu.avalon.authorization.entity.dto.SaveOauthRegisteredClientInput
import io.github.lishangbu.avalon.authorization.entity.dto.UpdateOauthRegisteredClientInput
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/**
 * OAuth2 注册客户端服务
 *
 * 定义 OAuth2 注册客户端的管理能力
 *
 * @author lishangbu
 * @since 2026/3/19
 */
interface OauthRegisteredClientService {
    /** 按条件分页查询 OAuth2 注册客户端 */
    fun getPageByCondition(
        specification: OauthRegisteredClientSpecification,
        pageable: Pageable,
    ): Page<OauthRegisteredClientView>

    /** 按条件查询 OAuth2 注册客户端列表 */
    fun listByCondition(specification: OauthRegisteredClientSpecification): List<OauthRegisteredClientView>

    /** 按 ID 查询 OAuth2 注册客户端 */
    fun getById(id: String): OauthRegisteredClientView?

    /** 保存 OAuth2 注册客户端 */
    fun save(command: SaveOauthRegisteredClientInput): OauthRegisteredClientView

    /** 更新 OAuth2 注册客户端 */
    fun update(command: UpdateOauthRegisteredClientInput): OauthRegisteredClientView

    /** 按 ID 删除 OAuth2 注册客户端 */
    fun removeById(id: String)
}
