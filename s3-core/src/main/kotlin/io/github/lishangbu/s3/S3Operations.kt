package io.github.lishangbu.s3

/**
 * 对业务模块暴露的 S3 操作边界。
 *
 * 该接口只暴露 starter 的稳定领域类型，调用方不需要直接依赖 AWS SDK
 * 的 request/response 类型。
 */
interface S3Operations {
	/**
	 * 上传已经完整加载到内存中的对象内容。
	 *
	 * 小文件或已在内存中的内容可直接使用该方法；大文件应优先使用
	 * [putObject] 的流式命令重载，避免一次性分配过大的字节数组。
	 */
	fun putObject(command: S3PutObjectCommand): S3PutObjectResult

	/**
	 * 从输入流上传对象内容。
	 *
	 * 调用方负责关闭命令中的输入流；实现不会在上传完成后主动关闭它，
	 * 以免破坏调用方对上游资源生命周期的控制。
	 */
	fun putObject(command: S3PutObjectStreamCommand): S3PutObjectResult

	/**
	 * 下载对象全部内容到内存。
	 *
	 * 小文件读取可以使用该方法；大文件应优先使用 [getObjectStream]。
	 */
	fun getObject(key: S3ObjectKey): S3ObjectContent

	/**
	 * 打开对象内容流并返回元数据。
	 *
	 * 调用方必须关闭返回的 [S3ObjectStream]，否则底层 HTTP 连接可能无法及时释放。
	 */
	fun getObjectStream(key: S3ObjectKey): S3ObjectStream

	/**
	 * 读取对象元数据，不下载对象内容。
	 */
	fun headObject(key: S3ObjectKey): S3ObjectMetadata

	/**
	 * 删除指定对象。
	 */
	fun deleteObject(key: S3ObjectKey)

	/**
	 * 判断对象是否存在。
	 *
	 * 对象不存在时返回 `false`；权限不足或服务端异常仍会抛出 [S3StorageException]。
	 */
	fun objectExists(key: S3ObjectKey): Boolean

	/**
	 * 列举当前配置前缀下的第一页对象。
	 */
	fun listObjects(): S3ObjectPage

	/**
	 * 按命令参数列举对象。
	 */
	fun listObjects(command: S3ListObjectsCommand): S3ObjectPage

	/**
	 * 使用默认过期时间创建对象下载的预签名 URL。
	 */
	fun createPresignedGetUrl(key: S3ObjectKey): S3PresignedUrl

	/**
	 * 按命令参数创建对象下载的预签名 URL。
	 */
	fun createPresignedGetUrl(command: S3PresignGetObjectCommand): S3PresignedUrl

	/**
	 * 使用默认过期时间创建对象上传的预签名 URL。
	 */
	fun createPresignedPutUrl(key: S3ObjectKey): S3PresignedUrl

	/**
	 * 按命令参数创建对象上传的预签名 URL。
	 */
	fun createPresignedPutUrl(command: S3PresignPutObjectCommand): S3PresignedUrl
}
