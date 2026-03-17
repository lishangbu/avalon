package io.github.lishangbu.avalon.authorization.controller;

import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient;
import io.github.lishangbu.avalon.authorization.service.OauthRegisteredClientService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// OAuth2 注册客户端控制器
///
/// 提供 OAuth2 注册客户端管理相关接口
///
/// @author lishangbu
/// @since 2026/3/19
@RestController
@RequestMapping("/oauth-registered-client")
@RequiredArgsConstructor
public class OauthRegisteredClientController {

    private final OauthRegisteredClientService oauthRegisteredClientService;

    /// 分页条件查询注册客户端
    ///
    /// @param pageable         分页参数
    /// @param registeredClient 查询条件
    /// @return 注册客户端分页结果
    @GetMapping("/page")
    public Page<OauthRegisteredClient> getPage(
            Pageable pageable, OauthRegisteredClient registeredClient) {
        return oauthRegisteredClientService.getPageByCondition(registeredClient, pageable);
    }

    /// 条件查询注册客户端列表
    ///
    /// @param registeredClient 查询条件
    /// @return 注册客户端列表
    @GetMapping("/list")
    public List<OauthRegisteredClient> list(OauthRegisteredClient registeredClient) {
        return oauthRegisteredClientService.listByCondition(registeredClient);
    }

    /// 根据 ID 查询注册客户端
    ///
    /// @param id 主键
    /// @return 注册客户端
    @GetMapping("/{id}")
    public OauthRegisteredClient getById(@PathVariable String id) {
        return oauthRegisteredClientService.getById(id).orElse(null);
    }

    /// 新增注册客户端
    ///
    /// @param registeredClient 注册客户端实体
    /// @return 保存后的注册客户端
    @PostMapping
    public OauthRegisteredClient save(@RequestBody OauthRegisteredClient registeredClient) {
        return oauthRegisteredClientService.save(registeredClient);
    }

    /// 更新注册客户端
    ///
    /// @param registeredClient 注册客户端实体
    /// @return 更新后的注册客户端
    @PutMapping
    public OauthRegisteredClient update(@RequestBody OauthRegisteredClient registeredClient) {
        return oauthRegisteredClientService.update(registeredClient);
    }

    /// 根据 ID 删除注册客户端
    ///
    /// @param id 主键
    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable String id) {
        oauthRegisteredClientService.removeById(id);
    }
}
