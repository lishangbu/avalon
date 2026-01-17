package io.github.lishangbu.avalon.authorization.model;

import io.github.lishangbu.avalon.authorization.entity.Role;
import io.github.lishangbu.avalon.authorization.entity.User;
import java.util.List;
import lombok.Data;

/// 用户视图对象，包含用户基本信息及其角色列表
///
/// 用于在服务层或控制层返回包含角色信息的用户视图
///
/// @author lishangbu
/// @since 2025/11/21
@Data
public class UserVO extends User {
  private List<Role> roles;
}
