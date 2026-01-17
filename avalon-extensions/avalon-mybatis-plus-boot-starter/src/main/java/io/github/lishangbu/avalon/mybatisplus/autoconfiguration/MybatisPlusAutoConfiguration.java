package io.github.lishangbu.avalon.mybatisplus.autoconfiguration;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/// # MyBatis-Plus 基础配置
/// 为项目统一配置 MyBatis-Plus 的基础能力
/// - 包含分页拦截器等常用扩展
/// - 通过程序化方式注册 Mapper 接口扫描，支持按模块结构组织 mapper 包
///
/// 注意：在模块化多 module 项目中，使用通配包路径（如 {@code io.github.lishangbu.avalon.**.mapper}）可以方便统一扫描
/// 部分 IDE 对此类通配路径的静态检查可能会报“包不存在”的提示，这属于 IDE 检查问题，运行时 Spring Boot 与 MyBatis 会正确加载 mapper 接口
/// 如需在 IDE 中消除警告可以按团队约定添加相应的忽略注释
///
/// @author avalon-team
/// @since 2025/12/11
@AutoConfiguration
public class MybatisPlusAutoConfiguration {

  /// 添加 MyBatis-Plus 拦截器配置
  ///
  /// @return 配置好的 MybatisPlusInterceptor，包含分页拦截器
  @Bean
  public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    // 添加分页拦截器，支持自动识别数据库类型并分页
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
    return interceptor;
  }

  /// MapperScannerConfigurer 扫描 Mapper 接口
  ///
  /// @return MapperScannerConfigurer 用于扫描 mapper 接口
  @Bean
  public MapperScannerConfigurer mapperScannerConfigurer() {
    MapperScannerConfigurer scannerConfigurer = new MapperScannerConfigurer();
    // 使用通配路径统一扫描模块下的 mapper 包
    scannerConfigurer.setBasePackage("io.github.lishangbu.avalon.**.mapper");
    return scannerConfigurer;
  }
}
