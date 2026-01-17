package io.github.lishangbu.avalon.authorization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient;
import java.util.Optional;

/// OauthRegisteredClient 数据访问 Mapper
///
/// 提供对 OauthRegisteredClient 实体的查询方法
///
/// @author lishangbu
/// @date 2023-10-08
public interface OauthRegisteredClientMapper extends BaseMapper<OauthRegisteredClient> {
  /// 根据 client_id 查询注册客户端实体
  ///
  /// @param clientId 客户端标识
  /// @return 查询到的 OauthRegisteredClient，未找到返回 Optional.empty()
  Optional<OauthRegisteredClient> selectByClientId(String clientId);
}
