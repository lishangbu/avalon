package io.github.lishangbu.avalon.inventory.persistence.mapper;

import io.github.lishangbu.avalon.inventory.domain.DungeonChestRulesLoot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** 宝箱存档格式只由持久化适配器处理；损坏存档阻止领取，不能默认为空奖励。 */
@Component
@RequiredArgsConstructor
public class DungeonChestCodec {

	private final ObjectMapper json;

	public String encode(DungeonChestRulesLoot loot) {
		return json.writeValueAsString(loot);
	}

	public DungeonChestRulesLoot decode(String value) {
		return json.readValue(value, DungeonChestRulesLoot.class);
	}

}
