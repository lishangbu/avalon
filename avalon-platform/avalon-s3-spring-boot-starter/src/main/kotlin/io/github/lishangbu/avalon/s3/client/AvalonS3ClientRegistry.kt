package io.github.lishangbu.avalon.s3.client

import io.github.lishangbu.avalon.s3.facade.S3Facade
import java.util.LinkedHashSet

/** 命名 S3 client 注册表。 */
class AvalonS3ClientRegistry(
    private val defaultClientName: String,
    private val bundles: Map<String, AvalonS3ClientBundle>,
) : AutoCloseable {
    init {
        require(bundles.isNotEmpty()) {
            "At least one enabled S3 client must be configured."
        }
        require(bundles.containsKey(defaultClientName)) {
            "Default S3 client '$defaultClientName' is not defined. Available clients: ${bundles.keys}."
        }
    }

    /** 获取默认 facade。 */
    fun defaultFacade(): S3Facade = facade(defaultClientName)

    /** 获取命名 facade。 */
    fun facade(clientName: String): S3Facade = bundle(clientName).facade

    /** 获取命名 bundle。 */
    fun bundle(clientName: String): AvalonS3ClientBundle =
        bundles[clientName]
            ?: throw IllegalArgumentException("Unknown S3 client '$clientName'. Available clients: ${bundles.keys}.")

    /** 获取所有已注册名称。 */
    fun clientNames(): Set<String> = bundles.keys

    /** 获取所有 facade。 */
    fun facades(): Map<String, S3Facade> = bundles.mapValues { (_, bundle) -> bundle.facade }

    override fun close() {
        val closeables = LinkedHashSet<AutoCloseable>()
        bundles.values.forEach { bundle ->
            closeables += bundle.s3TransferManager
            closeables += bundle.s3Presigner
            closeables += bundle.s3ControlAsyncClient
            closeables += bundle.s3ControlClient
            closeables += bundle.s3AsyncClient
            closeables += bundle.s3Client
        }
        closeables.forEach(AutoCloseable::close)
    }
}
