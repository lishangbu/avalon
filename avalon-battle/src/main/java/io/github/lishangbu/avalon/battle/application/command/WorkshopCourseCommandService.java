package io.github.lishangbu.avalon.battle.application.command;

import io.github.lishangbu.avalon.battle.application.WorkshopCourseProjector;
import io.github.lishangbu.avalon.battle.application.model.WorkshopCourseAction;
import io.github.lishangbu.avalon.battle.application.model.WorkshopCourseView;
import io.github.lishangbu.avalon.battle.domain.WorkshopCourseRules;
import io.github.lishangbu.avalon.battle.domain.WorkshopCourseState;
import io.github.lishangbu.avalon.battle.persistence.entity.WorkshopCourseReceipt;
import io.github.lishangbu.avalon.battle.persistence.entity.WorkshopCourseSession;
import io.github.lishangbu.avalon.battle.persistence.repository.WorkshopCourseReceiptRepository;
import io.github.lishangbu.avalon.battle.persistence.repository.WorkshopCourseSessionRepository;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** 角色锁串行化课内命令；真实资产在会话期间禁止写入，临时JSON和幂等结果原子保存。 */
@Component
@RequiredArgsConstructor
public class WorkshopCourseCommandService {

	private final CharacterCommandService characters;

	private final CharacterFactQueryService facts;

	private final CharacterFactCommandService factCommands;

	private final WorkshopCourseSessionRepository sessions;

	private final WorkshopCourseReceiptRepository receipts;

	private final WorkshopCourseRules rules;

	private final WorkshopCourseProjector projector;

	private final ObjectMapper json;

	private final SecureRandom random = new SecureRandom();

	/** 开始请求重复时读取现有会话，包括已退出结果，不借重试免费重开或重抽学习结果。 */
	@Transactional
	public WorkshopCourseView start(long owner, String course, String requestId) {
		uuid(requestId);
		if (course == null || !Set.of("WORKSHOP_SKILLS", "WORKSHOP_GEAR").contains(course))
			throw new BusinessException(400, "请选择有效战外课程");
		var context = characters.lockForCourse(owner);
		var prior = sessions.prior(context.id(), requestId);
		if (prior != null) {
			if (!prior.getCourse().equals(course))
				throw new BusinessException(409, "请求号已用于其他课程");
			return view(prior);
		}
		characters.requireOutsideDungeon(owner);
		boolean academyLocation = context.location().equals("ACADEMY")
				|| course.equals("WORKSHOP_GEAR") && context.location().equals("ACADEMY_PETS");
		if (!academyLocation || !facts.codes(owner).contains("QUEST:O012"))
			throw new BusinessException(409, "请完成前往诺姆的旅程，再到学院参加练习");
		var session = new WorkshopCourseSession();
		session.setCharacterId(context.id());
		session.setRequestId(requestId);
		session.setCourse(course);
		session.setStatus("ACTIVE");
		session.setCreatedAt(Instant.now());
		session.setState(json.writeValueAsString(rules.create(course, context.profession())));
		sessions.saveAndFlush(session);
		characters.beginCourse(owner, session.getId());
		return view(session);
	}

	/** 请求修订号防旧页面推进新状态；重放分支先于状态检查且必须匹配完整负载。 */
	@Transactional
	public WorkshopCourseView act(long owner, long id, WorkshopCourseAction request) {
		validate(request);
		var context = characters.lockForCourse(owner);
		String payload = json.writeValueAsString(
				new Payload(id, request.revision(), request.type(), request.target(), request.position()));
		var prior = receipts.prior(context.id(), request.requestId());
		if (prior != null) {
			if (!prior.getPayload().equals(payload))
				throw new BusinessException(409, "请求号已用于其他课程操作");
			return json.readValue(prior.getResponse(), WorkshopCourseView.class);
		}
		var session = sessions.get(id);
		if (session == null || !session.getCharacterId().equals(context.id()))
			throw new BusinessException(404, "课程会话不存在");
		if (!session.getStatus().equals("ACTIVE") || sessions.active(context.id()) == null)
			throw new BusinessException(409, "课程已经结束");
		var state = json.readValue(session.getState(), WorkshopCourseState.class);
		if (request.revision() != state.getRevision())
			throw new BusinessException(409, "课程进度已变化，请刷新后继续");
		if (request.type().equals("FINISH") || request.type().equals("EXIT")) {
			boolean passed = request.type().equals("FINISH");
			if (passed && !state.getPhase().equals("DONE"))
				throw new BusinessException(409, "尚未完成全部实际练习");
			characters.endCourse(owner, id);
			if (passed)
				factCommands.record(owner, "TRAINING:" + session.getCourse());
			session.setStatus(passed ? "PASSED" : "EXITED");
			rules.discard(state, passed);
		}
		else
			rules.apply(state, request.type(), request.target(), request.position(), random);
		state.setRevision(Math.incrementExact(state.getRevision()));
		session.setState(json.writeValueAsString(state));
		var result = projector.project(id, session.getCourse(), session.getStatus(), state);
		var receipt = new WorkshopCourseReceipt();
		receipt.setCharacterId(context.id());
		receipt.setRequestId(request.requestId());
		receipt.setPayload(payload);
		receipt.setResponse(json.writeValueAsString(result));
		receipts.saveAndFlush(receipt);
		return result;
	}

	private WorkshopCourseView view(WorkshopCourseSession session) {
		return projector.project(session.getId(), session.getCourse(), session.getStatus(),
				json.readValue(session.getState(), WorkshopCourseState.class));
	}

	private void validate(WorkshopCourseAction request) {
		if (request == null || request.type() == null || request.revision() < 0)
			throw new BusinessException(400, "缺少课程操作或修订号");
		uuid(request.requestId());
		if (!Set
			.of("VIEW_POOL", "LEARN", "INSPECT_EQUIPMENT", "EQUIP", "RESET_VITALS", "UNEQUIP", "START_BAG",
					"USE_HEALTH", "CLAIM_MANA", "DEPOSIT", "WITHDRAW", "LOCK", "FINISH", "EXIT")
			.contains(request.type()))
			throw new BusinessException(400, "未知课程操作");
		boolean target = Set.of("LEARN", "EQUIP", "DEPOSIT", "WITHDRAW", "LOCK").contains(request.type());
		if (target && (request.target() == null || request.target().isBlank()) || !target && request.target() != null
				|| request.type().equals("WITHDRAW") != (request.position() != null))
			throw new BusinessException(400, "课程目标或位置参数无效");
	}

	private void uuid(String value) {
		try {
			if (value == null || !UUID.fromString(value).toString().equals(value))
				throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException error) {
			throw new BusinessException(400, "课程请求号须为标准UUID");
		}
	}

	private record Payload(long session, long revision, String type, String target, Integer position) {
	}

}
