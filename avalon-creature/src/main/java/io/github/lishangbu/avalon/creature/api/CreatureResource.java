package io.github.lishangbu.avalon.creature.api;

import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.command.CreatureEvolutionCommandService;
import io.github.lishangbu.avalon.creature.application.command.EvolutionTeachingCommandService;
import io.github.lishangbu.avalon.creature.application.command.ResearchExchangeCommandService;
import io.github.lishangbu.avalon.creature.application.query.EvolutionResearchQueryService;
import io.github.lishangbu.avalon.creature.application.command.CreatureEvolutionPreviewService;
import io.github.lishangbu.avalon.inventory.application.command.CultivationMaterialService.Mode;
import io.github.lishangbu.avalon.creature.application.command.CreatureNamingCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureNamingQueryService;
import io.github.lishangbu.avalon.creature.application.command.CreatureManagementService;
import io.github.lishangbu.avalon.creature.application.command.CollectionCommandService;
import io.github.lishangbu.avalon.creature.application.command.SkillLearningCommandService;
import io.github.lishangbu.avalon.creature.application.model.LearningView;
import io.github.lishangbu.avalon.creature.application.query.SkillLearningQueryService;
import io.github.lishangbu.avalon.creature.domain.SkillLearningRules;
import io.github.lishangbu.avalon.creature.application.model.CreatureView;
import io.github.lishangbu.avalon.creature.application.model.EncyclopediaView;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.platform.CurrentAccount;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * 只从认证身份读取账号，禁止客户端指定其他玩家的角色；同步 JDBC 运行于虚拟线程。
 */
@RestController
@RequestMapping("/api/creatures")
@RequiredArgsConstructor
public class CreatureResource {

	private final CreatureQueryService application;

	private final CreatureCommandService commands;

	private final CreatureManagementService management;

	private final CollectionCommandService collection;

	private final SkillLearningQueryService learningQueries;

	private final SkillLearningCommandService learningCommands;

	private final CurrentAccount identity;

	private final CreatureNamingQueryService namingQueries;

	private final CreatureNamingCommandService namingCommands;

	private final CreatureEvolutionPreviewService evolutionQueries;

	private final CreatureEvolutionCommandService evolutionCommands;

	private final EvolutionTeachingCommandService evolutionTeaching;

	private final EvolutionResearchQueryService evolutionResearch;

	private final ResearchExchangeCommandService researchExchange;

	@GetMapping("/{id}/provenance")
	public CreatureQueryService.Provenance provenance(@PathVariable("id") long id) {
		return application.provenance(owner(), id);
	}

	@PostMapping("/evolution/exchange")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void exchange(@RequestBody(required = false) ResearchExchangeCommandService.Request request) {
		researchExchange.exchange(owner(), request);
	}

	@GetMapping("/evolution/research")
	public EvolutionResearchQueryService.View research() {
		return evolutionResearch.view(owner());
	}

	@PostMapping("/evolution/teaching")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void learnEvolution(@RequestBody(required = false) EvolutionTeachingCommandService.Request request) {
		evolutionTeaching.learn(owner(), request);
	}

	@GetMapping("/{id}/evolution")
	public CreatureEvolutionPreviewService.Preview evolution(@PathVariable("id") long id,
			@RequestParam(value = "mode", required = false) Mode mode) {
		return evolutionQueries.preview(owner(), id, mode);
	}

	@PostMapping("/{id}/evolution")
	public CreatureEvolutionCommandService.Result evolve(@PathVariable("id") long id,
			@RequestBody(required = false) CreatureEvolutionCommandService.Request request) {
		return evolutionCommands.evolve(owner(), id, request);
	}

	@GetMapping("/{id}/name")
	public CreatureNamingQueryService.Preview name(@PathVariable("id") long id) {
		return namingQueries.preview(owner(), id);
	}

	@PostMapping("/{id}/name")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void rename(@PathVariable("id") long id,
			@RequestBody(required = false) CreatureNamingCommandService.Request request) {
		namingCommands.rename(owner(), id, request);
	}

	private long owner() {
		return identity.id();
	}

	@GetMapping
	public List<CreatureView> list() {
		return application.list(owner());
	}

	@GetMapping("/encyclopedia")
	public List<EncyclopediaView> encyclopedia() {
		return application.encyclopedia(owner());
	}

	@PostMapping("/encyclopedia/rewards")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void claimCollection() {
		collection.claim(owner());
	}

	@PostMapping("/{id}/position")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void position(@PathVariable("id") long id, @RequestBody(required = false) Placement request) {
		if (request == null)
			throw new BusinessException(400, "缺少编队位置");
		commands.position(owner(), id, request.position());
	}

	public record Placement(int position) {
	}

	@PostMapping("/{id}/skills/viewed")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void viewedSkills(@PathVariable("id") long id) {
		commands.viewedSkills(owner(), id);
	}

	@PostMapping("/capture-registration")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void registerCapture() {
		commands.registerCapture(owner());
	}

	@PostMapping("/{id}/deposit")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deposit(@PathVariable("id") long id) {
		management.deposit(owner(), id);
	}

	@PostMapping("/{id}/lock")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void lock(@PathVariable("id") long id, @RequestBody(required = false) Protection request) {
		if (request == null || request.locked() == null)
			throw new BusinessException(400, "缺少保护状态");
		management.lock(owner(), id, request.locked());
	}

	@PostMapping("/release")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void release(@RequestBody(required = false) CreatureManagementService.Release request) {
		management.release(owner(), request);
	}

	public record Protection(Boolean locked) {
	}

	@GetMapping("/{id}/learning")
	public LearningView learning(@PathVariable("id") long id) {
		return learningQueries.preview(owner(), id);
	}

	@PostMapping("/{id}/learning")
	public SkillLearningRules.Result learn(@PathVariable("id") long id,
			@RequestBody(required = false) SkillLearningCommandService.Request request) {
		return learningCommands.learn(owner(), id, request);
	}

}
