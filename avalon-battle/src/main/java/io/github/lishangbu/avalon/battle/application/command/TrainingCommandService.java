package io.github.lishangbu.avalon.battle.application.command;

import io.github.lishangbu.avalon.battle.application.model.BattleView;
import io.github.lishangbu.avalon.battle.application.model.BattleViews;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.TrainingPolicy;
import io.github.lishangbu.avalon.battle.persistence.entity.Battle;
import io.github.lishangbu.avalon.battle.persistence.repository.BattleRecordRepository;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.content.battle.AcademyTrainingCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import io.github.lishangbu.avalon.battle.replay.BattleReplayJournal;
import io.github.lishangbu.avalon.battle.replay.BattleReplayCodec;
import io.github.lishangbu.avalon.battle.domain.BattleRoundResult;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** 训练进出场与一次性奖励的事务边界。入场固定缺失的资源存档，训练过程不覆盖真实资源。 */
@Component
@RequiredArgsConstructor
public class TrainingCommandService {

	/** 文本记录只在事务提交后落盘，不参与数据库资产写入。 */
	private final BattleReplayJournal journal;

	private final CharacterCommandService characters;

	private final CharacterFactCommandService facts;

	private final CharacterFactQueryService factQueries;

	private final AcademyTrainingCatalog academy;

	private final TrainingPolicy policy;

	private final BattleRecordRepository battleRecordRepository;

	private final ObjectMapper codec;

	private final BattleViews views;

	private final SecureRandom entropy = new SecureRandom();

	/** 玩家明确查看公开资料并选择一种训练球；不强制高档球，不消耗回合或库存。 */
	@Transactional
	public BattleView captureInfo(long owner, long battleId, String ball) {
		var character = characters.lock(owner);
		var battle = battleRecordRepository.get(battleId);
		if (battle == null || !battle.getCharacterId().equals(character.id()))
			throw new BusinessException(404, "练习不存在");
		var state = codec.readValue(battle.getState(), BattleState.class);
		if (!battle.isActive() || state.getTraining() == null || !state.getTraining().capturePractice())
			throw new BusinessException(409, "当前不在捕捉练习中");
		if (ball == null || !List.of("SEAL_I", "SEAL_II").contains(ball))
			throw new BusinessException(400, "请选择本课普通封印球I或II");
		var before = codec.readValue(battle.getState(), BattleState.class);
		state.getTraining().setCaptureInfoViewed(true);
		state.getTraining().setSelectedBall(ball);
		battle.setState(codec.writeValueAsString(state));
		battleRecordRepository.flush();
		journal.state("SINGLE", battleId, "TRAINING_CAPTURE_INFO", before, state);
		return views.view(owner, battleId, state);
	}

	/** 学院课程入口：服务器决定借用资源，重试必须复用同课程与同编队。 */
	@Transactional
	public BattleView startAcademy(long owner, String courseId, List<String> selected, String requestId) {
		if (courseId == null || courseId.isBlank() || selected != null && selected.stream().anyMatch(Objects::isNull))
			throw new BusinessException(400, "请选择有效课程和借用伙伴");
		try {
			if (requestId == null || !UUID.fromString(requestId).toString().equals(requestId))
				throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException error) {
			throw new BusinessException(400, "请提供有效请求号");
		}
		var selection = selected == null ? List.<String>of() : List.copyOf(selected);
		var character = characters.lock(owner);
		var prior = battleRecordRepository.request(character.id(), requestId);
		if (prior != null) {
			var state = codec.readValue(prior.getState(), BattleState.class);
			if (state.getTraining() == null || !Objects.equals(state.getTraining().getAcademyCourse(), courseId)
					|| !state.getTraining().getSelectedPartners().equals(selection))
				throw new BusinessException(409, "请求号已用于其他课程或编队");
			return views.view(owner, prior.getId(), state);
		}
		characters.requireOutsideDungeon(owner);
		var course = academy.course(courseId);
		if (course == null)
			throw new BusinessException(404, "学院课程不存在");
		if (!character.location().equals(academy.definition().location())
				|| !factQueries.codes(owner).contains(course.requires()))
			throw new BusinessException(409, "请完成前置课程并到学院参加练习");
		var state = policy.createAcademy(courseId, character.profession(), selection, entropy.nextLong());
		state.getLog().add("进入" + course.name() + "，本课使用借用队伍，不消耗真实物品。");
		return persist(owner, character.id(), requestId, state);
	}

	/** 学院课程共用持久化边界，只有通过各自资格校验后才创建隔离战局。 */
	private BattleView persist(long owner, long characterId, String requestId, BattleState state) {
		var battle = new Battle();

		battle.setCharacterId(characterId);
		battle.setRequestId(requestId);
		battle.setActive(true);
		battle.setState(codec.writeValueAsString(state));
		battleRecordRepository.saveAndFlush(battle);
		journal.start("SINGLE", battle.getId(), state);
		characters.setBattle(owner, battle.getId(), false);
		return views.view(owner, battle.getId(), state);
	}

	/** 课程仅记录完成事实与职业技能；经验和金币由正式任务交付，避免额外的训练首通奖励。 */
	@Transactional(Transactional.TxType.MANDATORY)
	public void settle(long owner, BattleState state) {
		String courseId = state.getTraining().getAcademyCourse();
		if (courseId == null || academy.course(courseId) == null)
			throw new IllegalStateException("训练战局缺少正式学院课程");
		if (state.getStatus().equals("WON")) {
			facts.record(owner, "TRAINING:" + courseId);
			if (courseId.equals("BQ023"))
				facts.record(owner, "SKILL:BQ023_GRANTED");
			state.getLog().add("学院课程完成。借用资源已归还，请向导师回报领取任务奖励。");
		}
		else
			state.getLog().add("练习未完成，可免费重开；真实队伍与资源不变。");
	}

}
