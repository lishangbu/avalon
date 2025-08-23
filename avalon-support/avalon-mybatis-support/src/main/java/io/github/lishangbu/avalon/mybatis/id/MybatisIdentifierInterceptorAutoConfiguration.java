package io.github.lishangbu.avalon.mybatis.id;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Lazy;

/**
 * 自动注入id生成插件
 *
 * @author lishangbu
 * @since 2025/8/20
 */
@AutoConfiguration(after = MybatisAutoConfiguration.class)
@ConditionalOnBean(SqlSessionFactory.class)
@Lazy(false)
@RequiredArgsConstructor
public class MybatisIdentifierInterceptorAutoConfiguration implements InitializingBean {

  private final List<SqlSessionFactory> sqlSessionFactoryList;

  @Override
  public void afterPropertiesSet() throws Exception {
    MybatisIdentifierInterceptor interceptor = new MybatisIdentifierInterceptor();
    for (SqlSessionFactory sqlSessionFactory : sqlSessionFactoryList) {
      org.apache.ibatis.session.Configuration configuration = sqlSessionFactory.getConfiguration();
      if (!containsInterceptor(configuration, interceptor)) {
        configuration.addInterceptor(interceptor);
      }
    }
  }

  /**
   * 是否已经存在相同的拦截器
   *
   * @param configuration
   * @param interceptor
   * @return
   */
  private boolean containsInterceptor(
      org.apache.ibatis.session.Configuration configuration, Interceptor interceptor) {
    try {
      return configuration.getInterceptors().stream()
          .anyMatch(config -> interceptor.getClass().isAssignableFrom(config.getClass()));
    } catch (Exception e) {
      return false;
    }
  }
}
