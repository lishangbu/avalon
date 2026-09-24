package io.github.lishangbu.avalon.character.persistence.mapper;

import io.github.lishangbu.avalon.character.application.model.CharacterView;
import io.github.lishangbu.avalon.character.persistence.entity.PlayerCharacter;
import io.github.lishangbu.avalon.content.battle.GrowthRules;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 持久化实体到用例结果的唯一转换入口；不依赖 HTTP Resource。
 */
@Component
@RequiredArgsConstructor
public class CharacterViews {

	private final GrowthRules growth;

	public CharacterView from(PlayerCharacter c) {
		return new CharacterView(c.getId().toString(), c.getName(), c.getProfession(), c.getLocation(), c.getVersion(),
				c.getExperience(), growth.level(c.getExperience()),
				c.getActiveBattleId() == null ? null : c.getActiveBattleId().toString(), c.getGold(),
				c.getActiveCourseId() == null ? null : c.getActiveCourseId().toString());
	}

}
