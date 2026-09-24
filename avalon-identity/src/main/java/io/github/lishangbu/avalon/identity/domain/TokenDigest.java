package io.github.lishangbu.avalon.identity.domain;

import io.github.lishangbu.avalon.platform.BusinessException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 不透明令牌格式和摘要规则；纯计算无数据库副作用，命令和认证查询共同使用。
 */
public final class TokenDigest {

	private TokenDigest() {
	}

	public static String hash(String token) {
		if (token == null || !token.matches("[A-Za-z0-9_-]{43}"))
			throw new BusinessException(401, "会话已失效，请重新登录");
		try {
			return HexFormat.of()
				.formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException error) {
			throw new IllegalStateException(error);
		}
	}

}
