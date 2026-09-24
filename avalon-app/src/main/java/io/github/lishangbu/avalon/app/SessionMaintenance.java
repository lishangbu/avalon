package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.identity.application.command.SessionMaintenanceService;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 定期清理过期的刷新重放记录；允许补执行，重复删除没有额外业务效果。
 */
@Component
@RequiredArgsConstructor
public class SessionMaintenance {

	private final SessionMaintenanceService maintenance;

	/**
	 * Spring 内存调度定期唤醒，数据库索引限定过期集合；无需保留失效 Token 的历史摘要。
	 */
	void clean() {
		maintenance.clean();
	}

}
