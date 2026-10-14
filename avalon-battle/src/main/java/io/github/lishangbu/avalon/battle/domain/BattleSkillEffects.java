package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.combat.*;
import io.github.lishangbu.avalon.combat.model.CombatStatus;
import io.github.lishangbu.avalon.combat.model.CombatStatusStacking;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.battle.model.*;
import java.util.*;
import java.util.function.BiConsumer;
import org.springframework.stereotype.Component;

/** Avalon内置效果注册器；规则核心不认识这些效果，新增能力可增加处理器而不修改回合引擎。 */
@Component
public class BattleSkillEffects {

	private final CombatEffects<BattleSkillEffect, BattleEffectContext> effects;

	/** 启动时装配全部内置效果并校验生产技能。重复注册和未知效果拒绝启动，无默认伤害降级。 */
	public BattleSkillEffects(DirectAttackResolver attacks, BattleCatalog catalog,
			List<BattleSkillEffectHandler> extensions) {
		List<CombatEffectHandler<BattleSkillEffect, BattleEffectContext>> handlers = new ArrayList<>();
		handlers.add(handler("WALL", (effect, context) -> {
			var state = context.state();
			var actor = context.actor();
			var target = context.target();
			actor.installStatus(new CombatStatus("WALL", actor.getId(), state.getRound(), state.getRound() + 1,
					effect.power(), Map.of("percent", (long) effect.power()), Map.of()), CombatStatusStacking.REPLACE,
					100);
			state.getLog().add(actor.getName() + "施放盾墙，本轮及下一轮直接伤害降低" + effect.power() + "%。");
		}));
		handlers.add(handler("RENEW", (effect, context) -> {
			var state = context.state();
			var actor = context.actor();
			var target = context.target();
			long amount = (long) Math.floor(actor.getStats().magicAttack() * effect.power() / 100.0);
			target.installStatus(new CombatStatus("RENEW", actor.getId(), state.getRound(), state.getRound() + 2,
					amount, Map.of("amount", amount), Map.of()), CombatStatusStacking.REPLACE, Integer.MAX_VALUE);
			state.getLog().add(actor.getName() + "为" + target.getName() + "施放恢复。");
		}));
		handlers.add(handler("MARK", (effect, context) -> {
			var state = context.state();
			var actor = context.actor();
			var target = context.target();
			// 每名施法人物独立保存印记，己方幻兽只读取所属人物的实例。
			target.getStatuses()
				.put("MARK:" + actor.getId(), new CombatStatus("MARK", actor.getId(), state.getRound(),
						state.getRound() + 2, effect.power(), Map.of(), Map.of()));
			state.getLog().add(actor.getName() + "为" + target.getName() + "施加猎人印记。");
		}));
		handlers.add(handler("MAGIC_GUARD", (effect, context) -> {
			var state = context.state();
			var actor = context.actor();
			var target = context.target();
			target.installStatus(new CombatStatus("MAGIC_GUARD", actor.getId(), state.getRound(), state.getRound(), 15,
					Map.of("active", 1L), Map.of()), CombatStatusStacking.REPLACE, 15);
			state.getLog().add(actor.getName() + "为" + target.getName() + "施放光幕，本回合法术直接攻击减伤15%。");
		}));
		handlers.add(handler("HEAL", (effect, context) -> {
			var state = context.state();
			var actor = context.actor();
			var target = context.target();
			double amount = actor.getStats().magicAttack() * effect.power() / 100.0;
			if ("回春".equals(actor.effect()) && target.getHp() * 2 <= target.getStats().health())
				amount *= 1.15;
			if ("滋养".equals(actor.effect()))
				amount *= 1.05;
			int healing = Math.min(target.getStats().health() - target.getHp(), (int) Math.floor(amount));
			target.setHp(target.getHp() + healing);
			if ("生命共鸣".equals(actor.effect()) && !actor.isHero() && !target.isHero() && actor != target
					&& actor.isEnemy() == target.isEnemy()
					&& Objects.equals(actor.getOwnerHero(), target.getOwnerHero()))
				actor.setHp(Math.min(actor.getStats().health(), actor.getHp() + healing / 4));
			if ("净羽".equals(actor.effect()) && actor.getCleanseRound() != state.getRound()
					&& target.getSlowUntil() >= state.getRound()) {
				target.setSlowUntil(0);
				actor.setCleanseRound(state.getRound());
			}
			state.getLog()
				.add(actor.getName() + "使用" + context.skill().id() + "，恢复" + target.getName() + " " + healing + "生命。");
		}));
		handlers.add(handler("COVER", (effect, context) -> {
			var state = context.state();
			var actor = context.actor();
			var target = context.target();
			var covers = context.covers();
			covers.computeIfAbsent(target.getId(), k -> new ArrayList<>())
				.add(new DirectAttackResolverProtection(actor.getId(), context.skill().id().equals("援护")));
			state.getLog().add(actor.getName() + "保护" + target.getName() + "。");
		}));
		handlers.add(handler("SHIELD", (effect, context) -> {
			var state = context.state();
			var actor = context.actor();
			var target = context.target();
			long amount = Math.multiplyExact(actor.getStats().magicAttack(), 2);
			actor.installStatus(new CombatStatus("SHIELD", actor.getId(), state.getRound(), state.getRound() + 2,
					amount, Map.of("amount", amount), Map.of()), CombatStatusStacking.REPLACE, Integer.MAX_VALUE);
			state.getLog().add(actor.getName() + "展开法力护盾。");
		}));
		handlers.add(handler("DAMAGE", (effect, context) -> {
			var state = context.state();
			var actor = context.actor();
			var target = context.target();
			var skill = new BattleSkill(context.skill().id(), effect.kind(), context.skill().element(),
					context.skill().attack(), context.skill().defense(), effect.power(), context.skill().mana(),
					context.skill().cooldown(), context.skill().priority(), context.skill().target(),
					context.skill().lowHealthPower(), context.skill().description());
			var defending = context.defending();
			var covers = context.covers();
			var random = context.random();
			attacks.apply(state, actor, context.targets(), skill, context.area(), .9375 + random.nextDouble() * .125,
					defending, covers);
		}));
		handlers.add(handler("SLOW", (effect, context) -> {
			var state = context.state();
			var actor = context.actor();
			var target = context.target();
			var skill = new BattleSkill(context.skill().id(), effect.kind(), context.skill().element(),
					context.skill().attack(), context.skill().defense(), effect.power(), context.skill().mana(),
					context.skill().cooldown(), context.skill().priority(), context.skill().target(),
					context.skill().lowHealthPower(), context.skill().description());
			var defending = context.defending();
			var covers = context.covers();
			var random = context.random();
			attacks.apply(state, actor, context.targets(), skill, context.area(), .9375 + random.nextDouble() * .125,
					defending, covers);
		}));
		handlers.add(handler("POISON", (effect, context) -> {
			var state = context.state();
			var actor = context.actor();
			var target = context.target();
			var skill = new BattleSkill(context.skill().id(), effect.kind(), context.skill().element(),
					context.skill().attack(), context.skill().defense(), effect.power(), context.skill().mana(),
					context.skill().cooldown(), context.skill().priority(), context.skill().target(),
					context.skill().lowHealthPower(), context.skill().description());
			var defending = context.defending();
			var covers = context.covers();
			var random = context.random();
			attacks.apply(state, actor, context.targets(), skill, context.area(), .9375 + random.nextDouble() * .125,
					defending, covers);
		}));
		handlers.add(handler("AREA", (effect, context) -> {
			var state = context.state();
			var actor = context.actor();
			var target = context.target();
			var skill = new BattleSkill(context.skill().id(), effect.kind(), context.skill().element(),
					context.skill().attack(), context.skill().defense(), effect.power(), context.skill().mana(),
					context.skill().cooldown(), context.skill().priority(), context.skill().target(),
					context.skill().lowHealthPower(), context.skill().description());
			var defending = context.defending();
			var covers = context.covers();
			var random = context.random();
			attacks.apply(state, actor, context.targets(), skill, context.area(), .9375 + random.nextDouble() * .125,
					defending, covers);
		}));
		handlers.addAll(extensions);
		effects = new CombatEffects<>(handlers, BattleSkillEffect::kind);
		catalog.definition().skills().forEach(skill -> effects.validate(skill.effects()));
	}

	/** 以配置顺序执行整次技能，调用方负责先付一次费用；每项效果都消耗核心触发预算。 */
	public void apply(BattleEffectContext context, CombatFrame<?, ?> frame) {
		effects.apply(context.skill().effects(), context, frame);
	}

	/** 使用函数封装无共享状态的内置处理器；参数校验与执行由同一契约提供。 */
	private BattleSkillEffectHandler handler(String kind, BiConsumer<BattleSkillEffect, BattleEffectContext> execute) {
		return new BattleSkillEffectHandler() {
			public String kind() {
				return kind;
			}

			public void validate(BattleSkillEffect effect) {
				if (effect.power() < 0 || (kind.equals("WALL") && effect.power() > 100))
					throw new IllegalArgumentException("技能效果数值无效");
			}

			public void apply(BattleSkillEffect effect, BattleEffectContext context) {
				execute.accept(effect, context);
			}
		};
	}

}
