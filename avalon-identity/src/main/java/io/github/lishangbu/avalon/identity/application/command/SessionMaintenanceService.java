package io.github.lishangbu.avalon.identity.application.command;

import io.github.lishangbu.avalon.identity.persistence.repository.RefreshReceiptRepository;

import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;

import lombok.RequiredArgsConstructor;

import java.time.Instant;

/**
 * 仅清理已到期重放凭证。会话保留策略保持不变，不能提前删除用于重放检测的记录。
 */
@Component
@RequiredArgsConstructor
public class SessionMaintenanceService {

	private final RefreshReceiptRepository receipts;

	@Transactional
	public void clean() {
		receipts.deleteExpired(Instant.now());
	}

}
