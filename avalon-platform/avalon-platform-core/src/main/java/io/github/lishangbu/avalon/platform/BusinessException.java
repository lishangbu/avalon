package io.github.lishangbu.avalon.platform;

/**
 * 可公开的业务错误。消息由服务端控制，禁止放入密码、Token 或 SQL 内容。
 */
public class BusinessException extends RuntimeException {

	private final int status;

	public BusinessException(int status, String message) {
		super(message);
		this.status = status;
	}

	public int status() {
		return status;
	}

}
