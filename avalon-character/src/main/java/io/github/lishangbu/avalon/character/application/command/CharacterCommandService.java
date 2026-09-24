package io.github.lishangbu.avalon.character.application.command;

import io.github.lishangbu.avalon.character.application.model.CharacterContext;
import io.github.lishangbu.avalon.character.application.model.CharacterArrived;
import io.github.lishangbu.avalon.character.application.model.CharacterView;
import io.github.lishangbu.avalon.character.persistence.entity.PlayerCharacter;
import io.github.lishangbu.avalon.character.persistence.mapper.CharacterViews;
import io.github.lishangbu.avalon.character.persistence.repository.CharacterRepository;
import io.github.lishangbu.avalon.character.persistence.repository.CharacterFactRepository;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.context.ApplicationEventPublisher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 创建与移动的唯一应用服务，返回 DTO，避免将 JPA 会话或实体关系暴露给浏览器。
 */
@Component
@RequiredArgsConstructor
public class CharacterCommandService {

	private final CharacterRepository characters;

	private final CharacterFactRepository facts;

	private final WorldCatalog catalog;

	private final GrowthRules growth;

	private final CharacterViews views;

	private final ApplicationEventPublisher arrivals;

	/** 相同创建请求在已有角色时返回原角色，不允许创建接口变相修改职业。 */
	/**
	 * 创建角色的命令事务。owner
	 * 来自已认证账号，职业和名称仍须服务端验证。相同账号的相同创建参数返回原角色；不同参数拒绝修改职业。数据库唯一约束兜底并发创建，flush 后才返回持久化版本。
	 */
	@Transactional
	public CharacterView create(long owner, String name, String profession) {
		if (name == null || !name.matches("[\\p{L}\\p{N}_]{2,16}")
				|| catalog.definition().professions().stream().noneMatch(p -> p.id().equals(profession)))
			throw new BusinessException(400, "请选择职业，角色名需为 2～16 位文字、数字或下划线");
		PlayerCharacter existing = characters.owned(owner);
		if (existing != null) {
			if (existing.getName().equals(name) && existing.getProfession().equals(profession))
				return views.from(existing);
			throw new BusinessException(409, "已有角色，不能重新选择职业");
		}
		PlayerCharacter c = new PlayerCharacter();

		c.setAccountId(owner);
		c.setName(name);
		c.setProfession(profession);
		c.setLocation(catalog.definition().start());
		characters.saveAndFlush(c);
		return views.from(c);
	}

	/** 位置版本阻止并发页面把旧路线重复执行；重复已完成移动不增加版本。 */
	/**
	 * 在角色悲观锁内校验位置修订号与地图相邻关系。仅当请求对应刚完成的同一次移动时返回现状；其他旧 revision
	 * 拒绝。位置写入与版本递增同事务完成，前端无权传入新版本号。
	 */
	@Transactional
	public CharacterView move(long owner, String destination, long expectedRevision) {
		requireOutsideDungeon(owner);
		requireNoLifeActivity(owner);
		PlayerCharacter c = characters.lockOwned(owner);
		if (c == null)
			throw new BusinessException(404, "请先创建角色");
		if (c.getLastMoveRevision() != null && c.getLastMoveRevision() == expectedRevision
				&& c.getLocation().equals(destination))
			return views.from(c);
		if (c.getVersion() != expectedRevision)
			throw new BusinessException(409, "位置已变化，请刷新地图");
		if (!catalog.place(c.getLocation()).exits().contains(destination))
			throw new BusinessException(400, "只能前往道路相连的相邻地点");
		if (!catalog.accessible(catalog.place(destination), fact -> facts.contains(c.getId(), fact)))
			throw new BusinessException(409, "此地尚未开放或已因剧情关闭，请查看当前路线");
		c.setLocation(destination);
		c.setLastMoveRevision(expectedRevision);
		arrivals.publishEvent(new CharacterArrived(owner, destination));
		characters.flush();
		return views.from(c);
	}

	/** 跨模块事务的角色锁入口，返回不可变上下文，禁止其他模块直接访问角色表。 */
	/**
	 * 供跨模块命令加入已有事务后取得角色锁。账号无角色则拒绝；返回不可变上下文，不返回 JPA 实体。MANDATORY
	 * 防止锁在短小子事务提前释放，锁持续到最外层奖励或穿戴用例结束。
	 */
	@Transactional(Transactional.TxType.MANDATORY)
	public CharacterContext lock(long owner) {
		var context = lockForCourse(owner);
		if (characters.owned(owner).getActiveCourseId() != null)
			throw new BusinessException(409, "请先完成或退出当前隔离课程");
		return context;
	}

	/** 会改变位置或开启另一种活动的命令必须先检查在线生活占用。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void requireNoLifeActivity(long owner) {
		lock(owner);
		if (characters.owned(owner).getActiveLifeSession() != null)
			throw new BusinessException(409, "请先结束当前采集或钓鱼");
	}

	/** 只限制派遣者，查看和操作其他伙伴不受生活占用影响。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void requireCreatureNotDispatched(long owner, long creature) {
		lock(owner);
		if (Objects.equals(characters.owned(owner).getDispatchedCreatureId(), creature))
			throw new BusinessException(409, "伙伴正在采集，请先结束派遣");
	}

	/** 调用者先验证生活资格及伙伴归属；该门禁与会话持久化共用外层事务。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void beginLifeActivity(long owner, String session, Long creature) {
		if (session == null || session.length() != 36 || (creature != null && creature <= 0))
			throw new IllegalArgumentException("生活会话或派遣编号无效");
		if (!UUID.fromString(session).toString().equals(session))
			throw new IllegalArgumentException("生活会话编号无效");
		requireOutsideDungeon(owner);
		requireNoLifeActivity(owner);
		var character = characters.owned(owner);
		if (character.getDungeonLobbyId() != null)
			throw new BusinessException(409, "请先退出副本组队再开始生活活动");
		character.setActiveLifeSession(session);
		character.setDispatchedCreatureId(creature);
	}

	/** 旧会话停止请求不能释放后续新会话占用。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void endLifeActivity(long owner, String session) {
		lock(owner);
		var character = characters.owned(owner);
		if (!Objects.equals(character.getActiveLifeSession(), session))
			throw new BusinessException(409, "生活会话已变化，请刷新");
		character.setActiveLifeSession(null);
		character.setDispatchedCreatureId(null);
	}

	/** 仅隔离课程用例使用的底层角色锁；允许读取当前课程，但不授权任何真实资产写入。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public CharacterContext lockForCourse(long owner) {
		PlayerCharacter character = characters.lockOwned(owner);
		if (character == null)
			throw new BusinessException(404, "请先创建角色");
		return new CharacterContext(character.getId(), character.getProfession(), character.getLocation(),
				growth.level(character.getExperience()));
	}

	/** 开课与会话持久化共用事务；不能在战斗、副本或已确认组队中叠加另一套资源上下文。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void beginCourse(long owner, long course) {
		requireOutsideDungeon(owner);
		requireNoLifeActivity(owner);
		var character = characters.owned(owner);
		if (character.getDungeonLobbyId() != null)
			throw new BusinessException(409, "请先退出组队再参加隔离课程");
		character.setActiveCourseId(course);
	}

	/** 课用例持锁验证会话归属后调用；同一事务先释放占用，再通过正常事实服务写通过回执。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void endCourse(long owner, long course) {
		lockForCourse(owner);
		var character = characters.owned(owner);
		if (!Objects.equals(character.getActiveCourseId(), course))
			throw new BusinessException(409, "隔离课程会话已变化");
		character.setActiveCourseId(null);
	}

	/**
	 * 战外操作统一门禁。先锁再检查，避免另一标签页在检查与修改之间开启战斗。
	 */
	@Transactional(Transactional.TxType.MANDATORY)
	public CharacterContext requireOutsideBattle(long owner) {
		var context = lock(owner);
		if (characters.owned(owner).getActiveBattleId() != null)
			throw new BusinessException(409, "请先结束当前战斗");
		return context;
	}

	/** 个人冒险、编队及城镇服务门禁；战间允许的换装仍使用 requireOutsideBattle。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public CharacterContext requireOutsideDungeon(long owner) {
		var context = requireOutsideBattle(owner);
		if (characters.owned(owner).getActiveDungeonId() != null)
			throw new BusinessException(409, "请先退出当前副本");
		return context;
	}

	/** 仅正式开本／离场事务调用，重复绑定同一进度允许，不能覆盖另一进度。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void enterDungeon(long owner, long run) {
		requireOutsideBattle(owner);
		requireNoLifeActivity(owner);
		var character = characters.owned(owner);
		if (character.getActiveDungeonId() != null && character.getActiveDungeonId() != run)
			throw new BusinessException(409, "已在其他副本进度中");
		character.setActiveDungeonId(run);
	}

	@Transactional(Transactional.TxType.MANDATORY)
	public void leaveDungeon(long owner, long run) {
		requireOutsideBattle(owner);
		var character = characters.owned(owner);
		if (character.getActiveDungeonId() == null)
			return;
		if (character.getActiveDungeonId() != run)
			throw new BusinessException(409, "副本进度已变化");
		character.setActiveDungeonId(null);
	}

	/** 不反向锁副本，避免换装持角色锁时与副本开战形成锁循环。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void invalidatePreparation(long owner) {
		lock(owner);
		var character = characters.owned(owner);
		character.setPreparationRevision(Math.incrementExact(character.getPreparationRevision()));
	}

	/** 组队事务先锁房间，再锁角色；角色只能占一个房间，但允许带着当前个人战斗确认。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void reserveLobby(long owner, long lobby) {
		lock(owner);
		requireNoLifeActivity(owner);
		var character = characters.owned(owner);
		if (character.getActiveDungeonId() != null)
			throw new BusinessException(409, "请先退出当前副本");
		if (character.getDungeonLobbyId() != null && character.getDungeonLobbyId() != lobby)
			throw new BusinessException(409, "已在其他副本队伍中");
		character.setDungeonLobbyId(lobby);
	}

	@Transactional(Transactional.TxType.MANDATORY)
	public void confirmLobby(long owner, long lobby, Instant until) {
		reserveLobby(owner, lobby);
		characters.owned(owner).setDungeonHoldUntil(until);
	}

	@Transactional(Transactional.TxType.MANDATORY)
	public void releaseLobby(long owner, long lobby) {
		lock(owner);
		var character = characters.owned(owner);
		if (!Objects.equals(character.getDungeonLobbyId(), lobby))
			return;
		character.setDungeonLobbyId(null);
		character.setDungeonHoldUntil(null);
	}

	/**
	 * 战斗用例持有同一角色锁时修改门禁；结束失败或奖励失败都会连同战局回滚。
	 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void setBattle(long owner, Long battleId, boolean defeated) {
		lock(owner);
		if (battleId != null)
			requireNoLifeActivity(owner);
		var character = characters.owned(owner);
		if (battleId != null && character.getActiveBattleId() == null && character.getActiveDungeonId() == null
				&& character.getDungeonHoldUntil() != null && Instant.now().isBefore(character.getDungeonHoldUntil()))
			throw new BusinessException(409, "已确认副本队伍，请先准备或退出组队，不能新开个人战斗");
		character.setActiveBattleId(battleId);
		if (defeated) {
			character.setLocation(catalog.definition()
				.safeReturns()
				.stream()
				// 航段等局部返回规则只适用于列出的地点，不能改变其他区域的失败返回点。
				.filter(destination -> destination.fromLocations().isEmpty()
						|| destination.fromLocations().contains(character.getLocation()))
				.filter(destination -> destination.requires() == null
						|| facts.contains(character.getId(), destination.requires()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("没有可用安全城镇"))
				.location());
			character.setLastMoveRevision(null);
		}
	}

	/**
	 * 余额不得透支，负数只用于已验证商品的消费。调用方负责保存业务幂等凭证。
	 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void changeGold(long owner, long delta) {
		lock(owner);
		var character = characters.owned(owner);
		long balance = Math.addExact(character.getGold(), delta);
		if (balance < 0)
			throw new BusinessException(409, "金币不足");
		character.setGold(balance);
	}

	/** 奖励流程已持有角色锁；经验保存累计值，满级不额外囤积。 */
	/**
	 * 先锁定角色，再通过统一成长规则增加累计经验并封顶。此方法必须加入调用方事务，后续装备或伙伴失败会连同经验一起回滚。flush
	 * 不是提交，只让同事务后续读取观察到最新状态。
	 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void grantExperience(long owner, int amount) {
		lock(owner);
		PlayerCharacter character = characters.lockOwned(owner);
		if (character == null || amount < 0)
			throw new IllegalArgumentException("经验奖励参数无效");
		character.setExperience(growth.addExperience(character.getExperience(), amount));
		characters.flush();
	}

}
