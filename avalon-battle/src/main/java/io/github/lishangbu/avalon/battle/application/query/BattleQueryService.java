package io.github.lishangbu.avalon.battle.application.query;

import io.github.lishangbu.avalon.battle.application.model.BattleLobby;
import io.github.lishangbu.avalon.battle.application.model.BattleLobbyEncounter;
import io.github.lishangbu.avalon.battle.application.model.BattleLobbyProduct;
import io.github.lishangbu.avalon.battle.application.model.BattleLobbyVitals;
import io.github.lishangbu.avalon.battle.application.model.BattleView;
import io.github.lishangbu.avalon.battle.application.model.BattleViews;
import io.github.lishangbu.avalon.battle.application.model.CharacterCombatProfile;
import io.github.lishangbu.avalon.battle.application.model.CharacterCombatProfileSkill;
import io.github.lishangbu.avalon.battle.application.model.PartyFactory;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.persistence.repository.BattleRecordRepository;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsRepository;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.battle.WildCatalog;
import io.github.lishangbu.avalon.content.battle.model.BattleEncounter;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.world.application.query.QuestQueryService;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 普通 SELECT 恢复最近战局与当前地点服务，不生成遭遇、不抽随机数、不创建资源存档。
 */
@Component
@RequiredArgsConstructor
public class BattleQueryService {

	private final CharacterQueryService characters;

	private final CharacterFactQueryService facts;

	private final QuestQueryService quests;

	private final EncounterAccess encounterAccess;

	private final CreatureQueryService creatures;

	private final BattleRecordRepository battleRecordRepository;

	private final CombatVitalsRepository combatVitalsRepository;

	private final ObjectMapper codec;

	private final BattleViews views;

	private final PartyFactory party;

	private final BattleCatalog catalog;

	private final ItemCatalog items;

	private final WorldCatalog world;

	private final WildCatalog wilds;

	private final CreatureCatalog species;

	@Transactional
	public CharacterCombatProfile characterProfile(long owner) {
		characters.requireId(owner);
		// 与正式入场复用同一构建逻辑；不建立资源存档，也不改变当前生命、魔力或战局。
		var hero = party.create(owner, false).getFirst();
		var skills = hero.getSkills()
			.stream()
			.map(catalog::skill)
			.map(skill -> new CharacterCombatProfileSkill(skill.id(), skill.element(), skill.mana(), skill.cooldown()))
			.toList();
		return new CharacterCombatProfile(hero.getStats(), skills);
	}

	@Transactional
	public BattleView current(long owner) {
		var battle = battleRecordRepository.findFirstByCharacterIdOrderByIdDesc(characters.requireId(owner));
		return battle == null ? null
				: views.view(owner, battle.getId(), codec.readValue(battle.getState(), BattleState.class));
	}

	@Transactional
	public BattleLobby lobby(long owner) {
		var c = characters.find(owner);
		characters.requireId(owner);
		boolean outside = c.activeBattleId() == null;
		var encounters = new ArrayList<>(catalog.definition()
			.encounters()
			.stream()
			.filter(e -> outside && e.location().equals(c.location()) && encounterAccess.allowed(owner, e)
					&& (e.wild() == null || c.level() >= wilds.profile(e.wild()).minPlayerLevel())
					&& (e.boss() == null || !facts.codes(owner).contains("VICTORY:" + e.id()))
					&& (!e.tutorial() || !creatures.hasSourcePrefix(owner, e.id() + ":")))
			.map(this::encounterView)
			.toList());
		var products = outside ? catalog.productsAt(c.location(), facts.codes(owner))
			.stream()
			.map(p -> new BattleLobbyProduct(p.code(), items.item(p.code()).name(), p.price()))
			.toList() : List.<BattleLobbyProduct>of();
		var vitals = party.create(owner, false).stream().map(u -> {
			var v = combatVitalsRepository.get(Long.parseLong(u.getId()));
			return new BattleLobbyVitals(u.getId(), u.getName(), v == null ? u.getHp() : Math.min(u.getHp(), v.getHp()),
					u.getStats().health(), v == null ? u.getMp() : Math.min(u.getMp(), v.getMp()), u.getStats().mana());
		}).toList();
		return new BattleLobby(encounters, outside && world.provides(c.location(), "RECOVERY"), products, vitals);
	}

	/** 入口只投影池与等级，不生成个体；点击探索后才固定具体敌人。 */
	private BattleLobbyEncounter encounterView(BattleEncounter encounter) {
		if (encounter.wild() == null)
			return new BattleLobbyEncounter(encounter.id(), encounter.name());
		var profile = wilds.profile(encounter.wild());
		String description = encounter.count() + "只" + (profile.elite() ? "精英" : "普通幻兽") + " · "
				+ new TreeMap<>(profile.species()).entrySet()
					.stream()
					.map(entry -> species.pet(entry.getKey()).name() + " Lv." + entry.getValue().minLevel()
							+ (entry.getValue().minLevel() == entry.getValue().maxLevel() ? ""
									: "～" + entry.getValue().maxLevel()))
					.collect(Collectors.joining("、"));
		return new BattleLobbyEncounter(encounter.id(), encounter.name(), description);
	}

}
