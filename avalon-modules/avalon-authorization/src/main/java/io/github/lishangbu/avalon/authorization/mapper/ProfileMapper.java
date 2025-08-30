package io.github.lishangbu.avalon.authorization.mapper;

import io.github.lishangbu.avalon.authorization.entity.Profile;
import java.util.Optional;

/**
 * 用户个人资料表(profile)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/30
 */
public interface ProfileMapper {

  /**
   * 通过id查询单条用户个人资料表数据
   *
   * @param id 主键
   * @return 可选的用户个人资料表
   */
  Optional<Profile> selectById(Long id);

  /**
   * 新增用户个人资料表
   *
   * @param profile 实例对象
   * @return 影响行数
   */
  int insert(Profile profile);

  /**
   * 修改用户个人资料表
   *
   * @param profile 实例对象
   * @return 影响行数
   */
  int updateById(Profile profile);

  /**
   * 通过id删除用户个人资料表
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(Long id);
}
