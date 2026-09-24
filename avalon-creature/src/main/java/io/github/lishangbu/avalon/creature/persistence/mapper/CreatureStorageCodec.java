package io.github.lishangbu.avalon.creature.persistence.mapper;

import tools.jackson.databind.ObjectMapper;
import io.github.lishangbu.avalon.creature.application.model.CreatureView;
import io.github.lishangbu.avalon.creature.persistence.entity.OwnedCreature;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * 现有数据库 JSON 字符串适配器。损坏数据作为内部故障上报，绝不伪造空属性或空技能。
 */
@Component
@RequiredArgsConstructor
public class CreatureStorageCodec {

	private final ObjectMapper json;

	public String encode(Object value) {
		return json.writeValueAsString(value);
	}

	/** 读取已提交的学习结果；损坏凭证是内部故障，不能通过重新抽签掩盖。 */
	public <T> T decode(String value, Class<T> type) {
		return json.readValue(value, type);
	}

	/**
	 * 只在 HTTP 展示边界转换雪花 ID；持久化数据异常不能静默丢弃。
	 */
	public CreatureView view(OwnedCreature p) {
		return new CreatureView(p.getId().toString(), p.getSpecies(), p.getName(), p.getElement(), p.getLevel(),
				p.getExperience(), p.getAbilitySlot(), p.getAbility(),
				List.of(json.readValue(p.getSkills(), String[].class)), p.getPartyPosition(), p.isLocked());
	}

}
