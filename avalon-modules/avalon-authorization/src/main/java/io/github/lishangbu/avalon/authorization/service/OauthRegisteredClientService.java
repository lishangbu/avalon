package io.github.lishangbu.avalon.authorization.service;

import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/// OAuth2 注册客户端服务
///
/// 提供 OAuth2 注册客户端的增删改查能力
///
/// @author lishangbu
/// @since 2026/3/19
public interface OauthRegisteredClientService {

    /// 根据条件分页查询注册客户端。
    Page<OauthRegisteredClient> getPageByCondition(
            OauthRegisteredClient registeredClient, Pageable pageable);

    /// 根据条件查询注册客户端列表。
    List<OauthRegisteredClient> listByCondition(OauthRegisteredClient registeredClient);

    /// 根据 ID 查询注册客户端。
    Optional<OauthRegisteredClient> getById(String id);

    /// 新增注册客户端。
    OauthRegisteredClient save(OauthRegisteredClient registeredClient);

    /// 更新注册客户端。
    OauthRegisteredClient update(OauthRegisteredClient registeredClient);

    /// 根据 ID 删除注册客户端。
    void removeById(String id);
}
