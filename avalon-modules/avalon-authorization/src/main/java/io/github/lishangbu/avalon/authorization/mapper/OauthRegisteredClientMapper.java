package io.github.lishangbu.avalon.authorization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient;
import java.util.Optional;

/**
 * OauthRegisteredClientMapper
 *
 * @author lishangbu
 * @date 2023-10-08
 */
public interface OauthRegisteredClientMapper extends BaseMapper<OauthRegisteredClient> {
  /**
   * 根据 client_id 查询注册客户端实体
   *
   * <p>使用 XML 映射或注解都可，XML 已存在 select 映射，此处保留方法签名返回实体
   *
   * @param clientId 客户端标识
   * @return 查询到的 OauthRegisteredClient，未找到返回 null
   */
  Optional<OauthRegisteredClient> selectByClientId(String clientId);
}
