package io.github.lishangbu.avalon.authorization.service;

import io.github.lishangbu.avalon.authorization.entity.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/// 角色服务
///
/// 定义与角色相关的业务契约
///
/// @author lishangbu
/// @since 2025/8/30
public interface RoleService {

    /// 根据条件分页查询角色。
    Page<Role> getPageByCondition(Role role, Pageable pageable);

    /// 根据条件查询角色列表。
    List<Role> listByCondition(Role role);

    /// 根据 ID 查询角色。
    Optional<Role> getById(Long id);

    /// 新增角色。
    Role save(Role role);

    /// 更新角色。
    Role update(Role role);

    /// 根据 ID 删除角色。
    void removeById(Long id);
}
