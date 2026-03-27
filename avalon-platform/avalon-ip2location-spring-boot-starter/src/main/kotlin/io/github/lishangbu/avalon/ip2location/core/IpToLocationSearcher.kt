package io.github.lishangbu.avalon.ip2location.core

import io.github.lishangbu.avalon.ip2location.exception.*
import io.github.lishangbu.avalon.ip2location.properties.IpToLocationProperties
import net.renfei.ip2location.IP2Location
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.core.io.ResourceLoader
import org.springframework.util.StreamUtils
import java.io.IOException

/**
 * IP 定位查询器
 *
 * 负责加载 IP2Location 数据库并执行 IP 查询
 *
 * @author lishangbu
 * @since 2025/4/12
 */
class IpToLocationSearcher(
    /** IP 定位配置 */
    private val ipToLocationProperties: IpToLocationProperties,
    /** 资源加载器 */
    private val resourceLoader: ResourceLoader,
) : InitializingBean,
    DisposableBean {
    /** IP2Location 引擎 */
    private var loc: IP2Location? = null

    /** 查询 IP 对应的地理位置信息 */
    fun ipQuery(ipAddress: String): IpResult? {
        val engine = loc ?: return null
        try {
            val result = engine.IPQuery(ipAddress)
            return when (result.status) {
                "OK" -> {
                    IpResult(result)
                }

                "EMPTY_IP_ADDRESS" -> {
                    log.error("IP address cannot be blank.")
                    throw EmptyIpAddressException()
                }

                "INVALID_IP_ADDRESS" -> {
                    log.error("Invalid IP address. IpAddress:[{}] is invalid", ipAddress)
                    throw InvalidIpAddressException()
                }

                "MISSING_FILE" -> {
                    log.error(
                        "Invalid database path. current database path is :[{}]",
                        ipToLocationProperties.dbFileLocation,
                    )
                    throw MissingFileException()
                }

                "IPV6_NOT_SUPPORTED" -> {
                    log.error(
                        "This BIN does not contain IPv6 data. IpAddress:[{}] is a v6 format ip",
                        ipAddress,
                    )
                    throw Ipv6NotSupportException()
                }

                else -> {
                    log.error("Unknown error.[{}]", result.status)
                    throw IpToLocationException("Unknown error.${result.status}")
                }
            }
        } catch (ex: IOException) {
            log.error(
                "reading database path failed. current database path is :[{}]",
                ipToLocationProperties.dbFileLocation,
                ex,
            )
            throw MissingFileException()
        }
    }

    /** 关闭定位器资源 */
    override fun destroy() {
        loc?.Close()
    }

    /** 加载 IP 数据库资源 */
    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        if (loc == null) {
            val resource = resourceLoader.getResource(ipToLocationProperties.dbFileLocation)
            loc =
                IP2Location().also { ip2Location ->
                    resource.inputStream.use { inputStream ->
                        ip2Location.Open(StreamUtils.copyToByteArray(inputStream))
                    }
                }
        }
    }

    companion object {
        /** 日志记录器 */
        private val log = LoggerFactory.getLogger(IpToLocationSearcher::class.java)
    }
}
