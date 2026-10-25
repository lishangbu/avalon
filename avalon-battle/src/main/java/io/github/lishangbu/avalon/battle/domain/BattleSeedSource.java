package io.github.lishangbu.avalon.battle.domain;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** 战斗初始种子的生成边界；具体回合仍使用战斗状态中的种子。 */
@Component
public class BattleSeedSource {

	private final SecureRandom random = new SecureRandom();

	public long nextLong() {
		return random.nextLong();
	}

}
