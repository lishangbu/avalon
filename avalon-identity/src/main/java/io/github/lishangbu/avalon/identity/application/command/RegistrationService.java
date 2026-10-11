package io.github.lishangbu.avalon.identity.application.command;

import io.github.lishangbu.avalon.identity.application.model.SessionTokens;
import io.github.lishangbu.avalon.content.text.ForbiddenWordsCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 注册事务同时创建账号和首次会话；数据库唯一约束负责并发注册冲突。
 */
@Component
@RequiredArgsConstructor
public class RegistrationService {

	private final PasswordEncoder passwords;

	private final RegistrationWriter writer;

	/** 只用于新账号注册；与登录共用的validate方法不能加入违禁词检查。 */
	private final ForbiddenWordsCatalog forbiddenWords;

	/** 不在外层事务内执行 bcrypt，防止并发注册时占用数据库连接等待 CPU 计算。 */

	/**
	 * 统一验证注册和登录的格式。用户名限制为字母数字下划线；密码除最短字符数外还按UTF-8字节限制72字节，防止 bcrypt 静默截断造成不同输入等价。
	 */
	public static void validate(String username, String password) {
		if (username == null || !username.matches("[a-zA-Z0-9_]{3,32}"))
			throw new BusinessException(400, "账号需为 3～32 位字母、数字或下划线");
		if (password == null || password.length() < 10 || password.getBytes(StandardCharsets.UTF_8).length > 72)
			throw new BusinessException(400, "密码至少 10 位且 UTF-8 长度不超过 72 字节");
	}

	/** bcrypt 的输入按 UTF-8 字节限制，防止静默截断导致不同密码相同。 */

	/**
	 * NOT_SUPPORTED 明确挂起外层事务，使 bcrypt 不占用数据库写事务。校验格式后计算摘要，委派 RegistrationWriter
	 * 原子创建账号与设备；不能把明文密码传入持久化实体。
	 */
	@Transactional(Transactional.TxType.NOT_SUPPORTED)
	public SessionTokens register(String username, String password, String label) {
		validate(username, password);
		// 在bcrypt及任何写入前拒绝新违规账号；密码仍只检查格式，不进入词表。
		// 旧用户名登录仍调用纯格式validate，不会因新增词条被锁在认证之外。
		if (forbiddenWords.contains(username))
			throw new BusinessException(400, "账号包含不允许使用的内容，请修改后重试");
		String hash = passwords.encode(password);
		return writer.create(username, hash, label);
	}

}
