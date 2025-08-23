package io.github.lishangbu.avalon.mybatis.pagehelper;

import com.github.pagehelper.PageInterceptor;
import java.util.List;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Lazy;

/**
 * 自动注入分页插件
 *
 * @author lishangbu
 * @since 2025/8/20
 */
@AutoConfiguration(after = MybatisAutoConfiguration.class)
@ConditionalOnBean(SqlSessionFactory.class)
@EnableConfigurationProperties({PageHelperProperties.class, PageHelperStandardProperties.class})
@Lazy(false)
public class PageHelperAutoConfiguration implements InitializingBean {

  private final List<SqlSessionFactory> sqlSessionFactoryList;

  private final PageHelperProperties properties;

  public PageHelperAutoConfiguration(
      List<SqlSessionFactory> sqlSessionFactoryList,
      PageHelperStandardProperties standardProperties) {
    this.sqlSessionFactoryList = sqlSessionFactoryList;
    this.properties = standardProperties.getProperties();
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    PageInterceptor interceptor = new PageInterceptor();
    interceptor.setProperties(this.properties);
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
