package io.github.lishangbu.avalon.ip2location.autoconfigure

import com.ip2location.IP2Location
import io.github.lishangbu.avalon.ip2location.properties.IpToLocationProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader
import org.springframework.util.StreamUtils

/**
 * IP2Location 自动配置
 *
 * 负责加载本地 BIN 数据库并注册官方 [IP2Location] Bean
 */
@AutoConfiguration
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(IP2Location::class)
@EnableConfigurationProperties(IpToLocationProperties::class)
class Ip2LocationAutoConfiguration {
    /** 创建并预热官方 IP2Location 查询器 */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(IP2Location::class)
    fun ip2Location(
        properties: IpToLocationProperties,
        resourceLoader: ResourceLoader,
    ): IP2Location {
        val resource = resourceLoader.getResource(properties.dbFileLocation)
        return IP2Location().apply {
            resource.inputStream.use { inputStream ->
                open(StreamUtils.copyToByteArray(inputStream))
            }
        }
    }
}
