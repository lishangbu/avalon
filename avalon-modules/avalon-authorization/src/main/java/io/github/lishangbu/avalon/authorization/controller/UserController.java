package io.github.lishangbu.avalon.authorization.controller;

import io.github.lishangbu.avalon.authorization.entity.User;
import io.github.lishangbu.avalon.authorization.model.UserWithRoles;
import io.github.lishangbu.avalon.authorization.service.UserService;
import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// 用户控制器
///
/// @author lishangbu
/// @since 2025/8/30
@RequestMapping("/user")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /// 获取当前用户信息
    ///
    /// @param user 当前用户信息
    /// @return 用户信息
    @GetMapping("/info")
    public UserWithRoles getUserInfo(@AuthenticationPrincipal UserInfo user) {
        return userService.getUserByUsername(user.getUsername()).orElse(null);
    }

    /// 分页条件查询用户
    ///
    /// @param pageable 分页参数
    /// @param user     查询条件
    /// @return 用户分页结果
    @GetMapping("/page")
    public Page<User> getUserPage(Pageable pageable, User user) {
        return userService.getPageByCondition(user, pageable);
    }

    /// 条件查询用户列表
    ///
    /// @param user 查询条件
    /// @return 用户列表
    @GetMapping("/list")
    public List<User> listUsers(User user) {
        return userService.listByCondition(user);
    }

    /// 根据 ID 查询用户
    ///
    /// @param id 用户 ID
    /// @return 用户信息
    @GetMapping("/{id:\\d+}")
    public User getById(@PathVariable Long id) {
        return userService.getById(id).orElse(null);
    }

    /// 新增用户
    ///
    /// @param user 用户实体
    /// @return 保存后的用户
    @PostMapping
    public User save(@RequestBody User user) {
        return userService.save(user);
    }

    /// 更新用户
    ///
    /// @param user 用户实体
    /// @return 更新后的用户
    @PutMapping
    public User update(@RequestBody User user) {
        return userService.update(user);
    }

    /// 根据 ID 删除用户
    ///
    /// @param id 用户 ID
    @DeleteMapping("/{id:\\d+}")
    public void deleteById(@PathVariable Long id) {
        userService.removeById(id);
    }
}
