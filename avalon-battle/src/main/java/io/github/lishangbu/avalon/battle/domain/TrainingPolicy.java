package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.content.battle.AcademyTrainingCatalog;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.battle.TrainingCatalog;
import io.github.lishangbu.avalon.content.battle.model.AcademyTrainingPartnerConfig;
import io.github.lishangbu.avalon.content.creature.CreatureAbilityRules;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import static io.github.lishangbu.avalon.battle.domain.BattleState.*;

/** 固定试题只安排敌方动作与判定目标；玩家技能及伤害仍由正式引擎结算。 */
@Component
@RequiredArgsConstructor
public class TrainingPolicy {

	private final TrainingCatalog catalog;

	private final BattleRules rules;

	private final AcademyTrainingCatalog academy;

	private final BattleCatalog battles;

	private final CreatureCatalog creatures;

	private final CreatureAbilityRules abilities;

	/** 学院战局只使用借用数据；选择顺序即两出战、两替补，未选者仅携带。 */
	public BattleState createAcademy(String courseId, String profession, List<String> selection, long seed) {
		var course = academy.course(courseId);
		if (course == null)
			throw new BusinessException(404, "学院课程不存在");
		if (selection == null)
			selection = List.of();
		var available = academy.definition().partners().stream().map(AcademyTrainingPartnerConfig::id).toList();
		if (course.goal().equals("SWAP") ? selection.size() != 4 || selection.stream().distinct().count() != 4
				|| !available.containsAll(selection) : !selection.isEmpty())
			throw new BusinessException(400, "换位课须从五只借用伙伴中选择不同的四只，其他课程无需选择");
		var state = course.goal().equals("PROFESSION") ? create(profession, List.of(), seed) : new BattleState();
		if (!course.goal().equals("PROFESSION")) {
			state.setSeed(seed);
			state.setTraining(new BattleStateTraining());
			int level = course.goal().equals("CAPTURE") ? academy.definition().capture().level()
					: academy.definition().level();
			var hero = trainee("trainee", "借用人物", profession, level);
			hero.setHero(true);
			hero.setSkills(battles.growth(profession).skills());
			state.getUnits().add(hero);
			var chosen = course.goal().equals("SWAP") ? selection : available.subList(0, 2);
			for (var source : academy.definition().partners()) {
				if (!course.goal().equals("SWAP") && !chosen.contains(source.id()))
					continue;
				var pet = new BattleStateUnit();
				pet.setId(source.id());
				pet.setName(source.name());
				pet.setSpecies(source.species());
				pet.setElement(source.element());
				pet.setLevel(level);
				pet.setOwnerHero(hero.getId());
				pet.setPosition(chosen.contains(source.id()) ? chosen.indexOf(source.id()) : 4);
				var s = course.goal().equals("CAPTURE") ? academy.definition().capture().stats().get(source.id())
						: source.stats();
				pet.setStats(
						new BattleStateStats(s.get(0), s.get(1), s.get(2), s.get(3), s.get(4), s.get(5), s.get(6)));
				pet.setHp(s.get(0));
				pet.setMp(s.get(1));
				pet.setSkills(source.skills());
				state.getUnits().add(pet);
			}
			var enemy = new BattleStateUnit();
			enemy.setId("academy-target");
			enemy.setName("学院练习木靶");
			enemy.setEnemy(true);
			enemy.setLevel(academy.definition().level());
			enemy.setStats(new BattleStateStats(course.enemyHealth(), 0, course.enemyAttack(), course.enemyAttack(),
					course.enemyDefense(), course.enemyDefense(), course.enemySpeed()));
			enemy.setHp(course.enemyHealth());
			state.getUnits().add(enemy);
			if (course.goal().equals("CAPTURE")) {
				var capture = academy.definition().capture();
				var species = creatures.pet(capture.species());
				var growth = battles.growth(capture.species());
				enemy.setSpecies(capture.species());
				enemy.setName(species.name() + "投影");
				enemy.setElement(species.element());
				enemy.setLevel(capture.level());
				var s = capture.stats().get("T_STONE");
				enemy.setStats(
						new BattleStateStats(s.get(0), s.get(1), s.get(2), s.get(3), s.get(4), s.get(5), s.get(6)));
				enemy.setHp(s.get(0));
				enemy.setMp(s.get(1));
				enemy.setSkills(growth.skills());
				var selected = abilities.draw(capture.species(), new Random(seed));
				enemy.setAbilitySlot(selected.slot());
				enemy.setAbility(selected.ability());
				state.getTraining().setSupplies(new HashMap<>(capture.supplies()));
				state.setCaptureAllowed(true);
			}
		}
		state.setEncounter("ACADEMY:" + courseId);
		state.getTraining().setAcademyCourse(courseId);
		state.getTraining().setProfession(profession);
		state.getTraining().setSelectedPartners(List.copyOf(selection));
		state.getTraining().setSwapPractice(course.goal().equals("SWAP"));
		plan(state, new Random(state.getSeed() ^ (state.getRound() * 7919L)));
		return state;
	}

	/** 记录已执行的队伍指令；只用于指令课程，不将敌人或额外携带成员算入。 */
	public void acted(BattleState state, BattleStateUnit actor) {
		if (state.getTraining() != null && "BQ018".equals(state.getTraining().getAcademyCourse()) && !actor.isEnemy()
				&& !state.getTraining().getActedUnits().contains(actor.getId()))
			state.getTraining().getActedUnits().add(actor.getId());
	}

	public BattleState create(String profession, List<Long> rewardPets, long seed) {
		var state = new BattleState();
		state.setSeed(seed);
		state.setEncounter("BQ023_TRAINING");
		var progress = new BattleStateTraining();
		progress.setProfession(profession);
		progress.setRewardPets(List.copyOf(rewardPets));
		state.setTraining(progress);
		var course = catalog.course(profession);
		int level = catalog.definition().level();
		var hero = trainee("trainee", "训练人物", profession, level);
		hero.setHero(true);
		hero.setSkills(List.of(course.skill()));
		state.getUnits().add(hero);
		if (course.goal().equals("MARK")) {
			var pet = trainee("training-pet", "训练伙伴", "CLAW_CAT", level);
			pet.setOwnerHero(hero.getId());
			state.getUnits().add(pet);
		}
		if (course.goal().equals("RENEW")) {
			for (int i = 0; i < 2; i++) {
				var ally = trainee("patient-" + i, "受伤学员" + (i + 1), "WARRIOR", level);
				ally.setPosition(i);
				state.getUnits().add(ally);
			}
			// 每人至少缺三跳治疗量，避免正确操作被满血溢出判为失败。
			int missing = (int) Math.floor(hero.getStats().magicAttack() * .6) * 3 + 1;
			state.getUnits().forEach(unit -> unit.setHp(Math.max(1, unit.getStats().health() - missing)));
		}
		var template = course.opponent();
		for (int i = 0; i < template.count(); i++) {
			var enemy = new BattleStateUnit();
			enemy.setId("training-enemy-" + i);
			enemy.setName(template.name() + (template.count() > 1 ? i + 1 : ""));
			enemy.setEnemy(true);
			enemy.setLevel(level);
			enemy.setStats(new BattleStateStats(template.health(), 0, template.attack(), template.attack(),
					template.defense(), template.defense(), template.speed()));
			enemy.setHp(template.health());
			state.getUnits().add(enemy);
		}
		state.setTelegraph(course.objective());
		plan(state, new Random(state.getSeed() ^ (state.getRound() * 7919L)));
		return state;
	}

	private BattleStateUnit trainee(String id, String name, String profession, int level) {
		var unit = new BattleStateUnit();
		unit.setId(id);
		unit.setName(name);
		unit.setSpecies(profession);
		unit.setLevel(level);
		// 借用人物使用职业成长并补入固定初始装备预算；借用幻兽只读物种七维。
		// 不能再用hero=false绕过装备扣除，否则人物会被误当作幻兽查表。
		boolean hero = Set.of("WARRIOR", "MAGE", "PRIEST", "RANGER").contains(profession);
		var equipment = hero
				? Map.of("health", 40, "physicalAttack", Set.of("WARRIOR", "RANGER").contains(profession) ? 8 : 0,
						"magicAttack", Set.of("MAGE", "PRIEST").contains(profession) ? 8 : 0, "physicalDefense", 6,
						"magicDefense", 4, "speed", 2)
				: Map.<String, Integer>of();
		unit.setStats(rules.stats(profession, level, hero, equipment));
		unit.setHp(unit.getStats().health());
		unit.setMp(unit.getStats().mana());
		return unit;
	}

	public void plan(BattleState state, Random random) {
		var academyId = state.getTraining().getAcademyCourse();
		if (state.getTraining().capturePractice()) {
			var targets = state.getUnits().stream().filter(u -> !u.isEnemy() && u.alive() && u.active()).toList();
			state.setEnemyOrders(state.getUnits()
				.stream()
				.filter(u -> u.isEnemy() && u.alive())
				.map(u -> new BattleStateOrder(u.getId(), u.getMp() >= battles.skill("碎岩击").mana() ? "碎岩击" : "ATTACK",
						targets.isEmpty() ? u.getId() : targets.get(random.nextInt(targets.size())).getId()))
				.toList());
			state.setTelegraph(academy.course(academyId).objective());
			return;
		}
		if (academyId != null && !academy.course(academyId).goal().equals("PROFESSION")) {
			var course = academy.course(academyId);
			int phase = (state.getRound() - 1) % 3;
			if (phase == 0)
				state.getTraining()
					.setAnnouncedTarget(state.getUnits()
						.stream()
						.filter(u -> !u.isEnemy() && u.alive() && u.active()
								&& (course.goal().equals("DEFENSE") ? u.isHero() : !u.isHero() && u.getPosition() == 0))
						.map(BattleStateUnit::getId)
						.findFirst()
						.orElse(null));
			String target = state.getTraining().getAnnouncedTarget();
			state.setEnemyOrders(state.getUnits()
				.stream()
				.filter(u -> u.isEnemy() && u.alive())
				.map(u -> new BattleStateOrder(u.getId(), phase == 1 && target != null ? "TRAINING_HIT" : "DEFEND",
						phase == 1 && target != null ? target : u.getId()))
				.toList());
			state.setTelegraph(course.objective()
					+ (phase == 0 ? " 本轮蓄力，下轮重击：" + target : phase == 1 ? " 本轮重击原目标：" + target : " 本轮休整。"));
			return;
		}
		if (state.getTraining().isSwapPractice()) {
			state.setEnemyOrders(state.getUnits()
				.stream()
				.filter(unit -> unit.isEnemy() && unit.alive())
				.map(unit -> new BattleStateOrder(unit.getId(), "DEFEND", unit.getId()))
				.toList());
			return;
		}
		var course = catalog.course(state.getTraining().getProfession());
		state.setEnemyOrders(state.getUnits()
			.stream()
			.filter(unit -> unit.isEnemy() && unit.alive())
			.map(unit -> new BattleStateOrder(unit.getId(), course.opponent().power() > 0 ? "TRAINING_HIT" : "DEFEND",
					"trainee"))
			.toList());
	}

	/** 由正式攻击完成后报告实际命中，空目标、未增伤或技能前死亡均不计数。 */
	public void attack(BattleState state, BattleStateUnit actor, List<BattleStateUnit> targets, String effect) {
		var progress = state.getTraining();
		if (progress == null)
			return;
		for (var target : targets) {
			if (actor.isEnemy() && target.isHero() && target.getWallUntil() >= state.getRound())
				progress.setWallHits(progress.getWallHits() + 1);
			var mark = target.getStatuses().get("MARK:" + (actor.isHero() ? actor.getId() : actor.getOwnerHero()));
			if (!actor.isEnemy() && mark != null && mark.until() >= state.getRound()
					&& !progress.getMarkedAttackers().contains(actor.getId()))
				progress.getMarkedAttackers().add(actor.getId());
		}
		if (actor.isHero() && effect.equals("AREA") && targets.size() == 3
				&& targets.stream().allMatch(unit -> !unit.alive()))
			progress.setAreaCompleted(true);
	}

	public void healed(BattleState state, BattleStateUnit target, int amount) {
		var progress = state.getTraining();
		if (progress != null && amount > 0 && List.of(1, 4, 7).contains(target.getRenewStarted()))
			progress.getHealTicks().merge(target.getId(), 1, Integer::sum);
	}

	public void finishRound(BattleState state) {
		var progress = state.getTraining();
		if (progress.capturePractice()) {
			boolean captured = state.getUnits().stream().anyMatch(u -> u.isEnemy() && u.isCaptured());
			if (captured) {
				boolean passed = progress.isCaptureInfoViewed() && progress.isCapturedAfterDamage();
				state.setStatus(passed ? "WON" : "LOST");
				if (!passed)
					state.getLog().add("捕获成功，但资料查看或削弱步骤尚未完成，可免费重开本课。");
			}
			else if (state.getStatus().equals("WON")) {
				state.setStatus("LOST");
				state.getLog().add("目标已被击倒，本次没有捕获，可免费重开练习。");
			}
			else if (state.getRound() >= academy.course("BQ020").rounds())
				state.setStatus("LOST");
			return;
		}
		if ("BQ018".equals(progress.getAcademyCourse())) {
			if (state.getStatus().equals("WON")) {
				if (!progress.isDefendedHit()
						|| !progress.getActedUnits().containsAll(List.of("trainee", "T_CAT", "T_STONE"))) {
					state.setStatus("LOST");
					state.getLog().add("战斗胜利，指令或防御练习尚未完成，可免费重试。");
				}
			}
			else if (state.getRound() >= academy.course("BQ018").rounds())
				state.setStatus("LOST");
			return;
		}
		if (progress.isSwapPractice()) {
			if (progress.isSwapped())
				state.setStatus("WON");
			else if (state.getRound() >= academy.course(progress.getAcademyCourse()).rounds()
					|| state.getStatus().equals("WON"))
				state.setStatus("LOST");
			return;
		}
		var course = catalog.course(progress.getProfession());
		var hero = state.getUnits().stream().filter(BattleStateUnit::isHero).findFirst().orElseThrow();
		boolean complete = switch (course.goal()) {
			case "WALL" -> hero.alive() && progress.getWallHits() >= 2;
			case "AREA" -> progress.isAreaCompleted();
			case "RENEW" -> state.getRound() == 9 && progress.getHealTicks().size() == 3
					&& progress.getHealTicks().values().stream().allMatch(ticks -> ticks == 3);
			case "MARK" -> progress.getMarkedAttackers().contains(hero.getId())
					&& progress.getMarkedAttackers().stream().anyMatch(id -> !id.equals(hero.getId()));
			default -> false;
		};
		if (complete)
			state.setStatus("WON");
		else if (!hero.alive() || state.getRound() >= course.rounds() || state.getStatus().equals("WON"))
			state.setStatus("LOST");
		var goals = new ArrayList<String>();
		if (course.goal().equals("WALL"))
			goals.add("盾墙承伤 " + progress.getWallHits() + "/2");
		if (course.goal().equals("AREA"))
			goals.add(progress.isAreaCompleted() ? "三靶击破" : "尚未同时击破三靶");
		if (course.goal().equals("RENEW"))
			state.getUnits()
				.stream()
				.filter(unit -> !unit.isEnemy())
				.forEach(unit -> goals
					.add(unit.getName() + "有效恢复 " + progress.getHealTicks().getOrDefault(unit.getId(), 0) + "/3"));
		if (course.goal().equals("MARK"))
			goals.add("印记增伤单位 " + progress.getMarkedAttackers().size() + "/2");
		state.setTelegraph(course.objective() + " " + String.join("；", goals));
	}

}
