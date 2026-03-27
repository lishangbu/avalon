package io.github.lishangbu.avalon.jimmer.id

import org.babyfish.jimmer.sql.meta.UserIdGenerator
import java.lang.management.ManagementFactory
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.concurrent.ThreadLocalRandom

/**
 * Jimmer 主键生成器
 *
 * 雪花算法实现与 MyBatis-Plus Sequence 保持一致
 */
class SnowflakeIdGenerator : UserIdGenerator<Long> {
    /** 生成雪花 ID */
    override fun generate(entityType: Class<*>): Long = sequence.nextId()

    companion object {
        /** 序列 */
        private val sequence = Sequence()
    }

    private class Sequence(
        inetAddress: InetAddress? = null,
    ) {
        // 与 MyBatis-Plus Sequence 一致的起始时间戳
        private val twepoch = 1288834974657L

        // 机器与数据中心位宽：5 + 5
        private val workerIdBits = 5L

        /** 数据中心 ID 位数 */
        private val datacenterIdBits = 5L

        /** 最大工作节点 ID */
        private val maxWorkerId = -1L xor (-1L shl workerIdBits.toInt())

        /** 最大数据中心 ID */
        private val maxDatacenterId = -1L xor (-1L shl datacenterIdBits.toInt())

        // 序列位宽：12
        private val sequenceBits = 12L

        /** 工作节点 ID 偏移 */
        private val workerIdShift = sequenceBits

        /** 数据中心 ID 偏移 */
        private val datacenterIdShift = sequenceBits + workerIdBits

        /** 时间戳左偏移 */
        private val timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits

        /** 序列掩码 */
        private val sequenceMask = -1L xor (-1L shl sequenceBits.toInt())

        /** 数据中心 ID */
        private val datacenterId: Long

        /** 工作节点 ID */
        private val workerId: Long

        /** 序列 */
        private var sequence = 0L

        /** 最后时间戳 */
        private var lastTimestamp = -1L

        init {
            datacenterId = getDatacenterId(maxDatacenterId, inetAddress)
            workerId = getMaxWorkerId(datacenterId, maxWorkerId)
        }

        /** 生成下一个 ID */
        @Synchronized
        fun nextId(): Long {
            var timestamp = timeGen()
            // 时钟回拨时，按 MP 逻辑：小回拨等待，大回拨直接失败
            if (timestamp < lastTimestamp) {
                val offset = lastTimestamp - timestamp
                if (offset <= 5) {
                    Thread.sleep(offset shl 1)
                    timestamp = timeGen()
                    check(timestamp >= lastTimestamp) {
                        "Clock moved backwards. Refusing to generate id for $offset milliseconds"
                    }
                } else {
                    error("Clock moved backwards. Refusing to generate id for $offset milliseconds")
                }
            }

            if (lastTimestamp == timestamp) {
                // 同毫秒内自增序列，溢出则等待到下一毫秒
                sequence = (sequence + 1) and sequenceMask
                if (sequence == 0L) {
                    timestamp = tilNextMillis(lastTimestamp)
                }
            } else {
                // 跨毫秒时将序列随机置为 1..2，降低固定尾号聚集
                sequence = ThreadLocalRandom.current().nextLong(1, 3)
            }

            lastTimestamp = timestamp
            return ((timestamp - twepoch) shl timestampLeftShift.toInt()) or
                (datacenterId shl datacenterIdShift.toInt()) or
                (workerId shl workerIdShift.toInt()) or
                sequence
        }

        /** 获取数据中心 ID */
        private fun getDatacenterId(
            maxDatacenterId: Long,
            inetAddress: InetAddress?,
        ): Long {
            return try {
                // 使用网卡 MAC 推导数据中心 ID
                val address = inetAddress ?: InetAddress.getLocalHost()
                val network = NetworkInterface.getByInetAddress(address) ?: return 1L
                val mac = network.hardwareAddress ?: return 0L
                val id =
                    (
                        (0x000000FFL and mac[mac.size - 2].toLong()) or
                            (0x0000FF00L and (mac[mac.size - 1].toLong() shl 8))
                    ) shr 6
                id % (maxDatacenterId + 1)
            } catch (_: Exception) {
                0L
            }
        }

        /** 获取最大工作节点 ID */
        private fun getMaxWorkerId(
            datacenterId: Long,
            maxWorkerId: Long,
        ): Long {
            // 使用 datacenterId + pid 哈希推导 workerId
            val runtimeName = ManagementFactory.getRuntimeMXBean().name
            val pid =
                runtimeName
                    .substringBefore('@')
                    .toIntOrNull()
                    ?.let { if (it < 10) ThreadLocalRandom.current().nextInt(10, 4194304) else it }
            val mpid =
                buildString {
                    append(datacenterId)
                    if (pid != null) {
                        append(pid)
                    }
                }
            return (mpid.hashCode().toLong() and 0xffff) % (maxWorkerId + 1)
        }

        /** 等待到下一毫秒 */
        private fun tilNextMillis(lastTimestamp: Long): Long {
            var timestamp = timeGen()
            while (timestamp <= lastTimestamp) {
                timestamp = timeGen()
            }
            return timestamp
        }

        /** 获取当前时间戳 */
        private fun timeGen(): Long = System.currentTimeMillis()
    }
}
