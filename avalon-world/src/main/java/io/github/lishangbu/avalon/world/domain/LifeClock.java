package io.github.lishangbu.avalon.world.domain;

import java.time.Instant;
import org.springframework.stereotype.Component;

/** 生活实时会话仅使用服务端时间；独立边界供断线与重放测试推进时钟。 */
@Component
public class LifeClock {

	public Instant now() {
		return Instant.now();
	}

}
