package io.github.lishangbu.avalon.authorization.mapper;

import io.github.lishangbu.avalon.authorization.entity.User;
import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo;
import java.util.Optional;
import org.apache.ibatis.annotations.Param;

/**
 * 用户信息(user)表数据库访问层
 *
 * <p>提供对用户信息的增删改查等操作
 *
 * @author lishangbu
 * @since 2025/08/19
 */
public interface UserMapper {

  /**
   * 根据用户主键ID查询用户信息。
   *
   * @param id 用户主键ID
   * @return 查询到的用户信息对象，未找到时返回null
   */
  User selectById(Long id);

  /**
   * 根据条件统计用户信息总数。
   *
   * @param user 查询条件封装的用户对象
   * @return 满足条件的用户总数
   */
  long count(User user);

  /**
   * 新增一条用户信息记录。
   *
   * @param user 待新增的用户对象
   * @return 插入成功的记录数
   */
  int insert(User user);

  /**
   * 根据主键ID修改用户信息。
   *
   * @param user 待修改的用户对象，需包含主键ID
   * @return 更新成功的记录数
   */
  int updateById(User user);

  /**
   * 根据主键ID删除用户信息。
   *
   * @param id 用户主键ID
   * @return 删除成功的记录数
   */
  int deleteById(Long id);

  /**
   * 根据用户名查询用户详细信息。
   *
   * @param username 用户名
   * @return 查询到的用户信息，未找到时返回Optional.empty()
   */
  Optional<UserInfo> selectByUsername(@Param("username") String username);
}
