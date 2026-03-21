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
 * IP 搜索器 封装 IP2Location 的查询与资源加载逻辑，支持初始化与销毁钩子 This function to query IP2Location data.
 *
 * @param ipAddress IP Address you wish to query
 * @return IP2Location data
 * @throws IOException If an input or output exception occurred
 */

/**
 * IP 搜索器。
 *
 * 封装 IP2Location 的查询与资源加载逻辑，支持初始化与销毁钩子。
 *
 * @author lishangbu
 * @since 2025/4/12
 */
class IpToLocationSearcher(
    private val ipToLocationProperties: IpToLocationProperties,
    private val resourceLoader: ResourceLoader,
) : InitializingBean,
    DisposableBean {
    private var loc: IP2Location? = null

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

    override fun destroy() {
        loc?.Close()
    }

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
        private val log = LoggerFactory.getLogger(IpToLocationSearcher::class.java)
    }
}
