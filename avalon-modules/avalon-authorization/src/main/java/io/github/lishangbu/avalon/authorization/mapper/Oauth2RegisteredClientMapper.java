package io.github.lishangbu.avalon.authorization.mapper;

import io.github.lishangbu.avalon.authorization.entity.Oauth2RegisteredClient;
import java.util.Optional;
import org.apache.ibatis.annotations.Param;

/**
 * Oauth2注册客户端(oauth2_registered_client)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/19
 */
public interface Oauth2RegisteredClientMapper {

  /**
   * 通过id查询单条Oauth2注册客户端数据
   *
   * @param id 主键
   * @return Oauth2注册客户端
   */
  Optional<Oauth2RegisteredClient> selectById(String id);

  /**
   * 新增Oauth2注册客户端
   *
   * @param oauth2RegisteredClient 实例对象
   * @return 影响行数
   */
  int insert(Oauth2RegisteredClient oauth2RegisteredClient);

  /**
   * 修改Oauth2注册客户端
   *
   * @param oauth2RegisteredClient 实例对象
   * @return 影响行数
   */
  int updateById(Oauth2RegisteredClient oauth2RegisteredClient);

  /**
   * 通过id删除Oauth2注册客户端
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(String id);

  /**
   * 通过客户端Id查找Oauth2注册客户端
   *
   * @param clientId 客户端Id
   * @return 影响行数
   */
  Optional<Oauth2RegisteredClient> selectByClientId(@Param("clientId") String clientId);
}
