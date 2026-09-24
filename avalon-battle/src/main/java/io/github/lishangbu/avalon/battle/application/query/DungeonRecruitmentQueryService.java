package io.github.lishangbu.avalon.battle.application.query;

import org.springframework.data.domain.PageRequest;

import io.github.lishangbu.avalon.battle.application.model.DungeonLobbyView;
import io.github.lishangbu.avalon.battle.application.model.PartyFactory;
import io.github.lishangbu.avalon.battle.domain.DungeonLobbyState;
import io.github.lishangbu.avalon.battle.domain.DungeonProgressState;
import tools.jackson.databind.ObjectMapper;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsRepository;
import io.github.lishangbu.avalon.battle.persistence.repository.DungeonLobbyRepository;
import io.github.lishangbu.avalon.battle.persistence.repository.DungeonRunRecordRepository;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.DungeonCatalog;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 准备页面只读投影；查询不会过期清理、默认确认、恢复资源或触发开本。 */
@Component
@RequiredArgsConstructor
public class DungeonRecruitmentQueryService {

	private final DungeonLobbyQueryService queries;

	private final DungeonLobbyRepository lobbies;

	private final DungeonRunRecordRepository dungeonRunRecordRepository;

	private final CharacterQueryService characters;

	private final InventoryQueryService inventory;

	private final PartyFactory parties;

	private final CombatVitalsRepository combatVitalsRepository;

	private final DungeonCatalog catalog;

	private final ObjectMapper codec;

	@Transactional
	public DungeonLobbyView current(long owner) {
		Long lobby = characters.lobby(owner);
		return lobby == null ? null : view(lobby, owner);
	}

	/** 战内通知只读取本人确认状态，避免轮询加载全队编队、资源及背包。 */
	@Transactional
	public Notice notice(long owner) {
		Long lobby = characters.lobby(owner);
		if (lobby == null)
			return new Notice(null, false, null, false, 0);
		var row = lobbies.get(lobby);
		if (row == null)
			return new Notice(null, false, null, false, 0);
		var state = codec.readValue(row.getState(), DungeonLobbyState.class);
		var member = state.getMembers().get(owner);
		if (state.isClosed() || member == null)
			return new Notice(null, false, null, false, 0);
		return new Notice(lobby.toString(), member.isConfirmed(), member.getDeadline(),
				characters.adventure(owner).battle() != null, state.getMembers().size());
	}

	public record Notice(String id, boolean confirmed, Instant deadline, boolean inBattle, int members) {
	}

	@Transactional
	public DungeonLobbyView view(long lobby, long owner) {
		var state = queries.restore(lobby, owner);
		var members = state.getMembers().entrySet().stream().map(entry -> {
			long account = entry.getKey();
			var member = entry.getValue();
			var character = characters.find(account);
			boolean inBattle = characters.adventure(account).battle() != null;
			var units = parties.create(account, false).stream().map(unit -> {
				var saved = combatVitalsRepository.get(Long.parseLong(unit.getId()));
				int hp = saved == null ? unit.getHp() : Math.min(unit.getHp(), saved.getHp());
				int mp = saved == null ? unit.getMp() : Math.min(unit.getMp(), saved.getMp());
				return new DungeonLobbyView.Unit(unit.getId(), unit.getName(), unit.getLevel(), unit.isHero(),
						unit.getPosition(), hp, unit.getStats().health(), mp, unit.getStats().mana());
			}).toList();
			Map<String, Integer> supplies = new LinkedHashMap<>();
			inventory.list(account)
				.stream()
				.filter(item -> catalog.definition().supplies().contains(item.code()))
				.forEach(item -> supplies.merge(item.code(), item.quantity(), Integer::sum));
			return new DungeonLobbyView.Member(character.id(), character.name(), character.profession(),
					character.level(), account == owner, member.isConfirmed(), member.isReady() && !inBattle, inBattle,
					member.getDeadline(), units, Map.copyOf(supplies));
		}).toList();
		boolean canLaunch = !state.isClosed() && state.getMembers().containsKey(state.getLeader())
				&& state.getMembers()
					.keySet()
					.stream()
					.allMatch(account -> catalog.definition().entrance().equals(characters.find(account).location()))
				&& members.size() == (state.getTargetRun() == null ? catalog.definition().players() : 1)
				&& members.stream()
					.allMatch(member -> member.confirmed() && member.ready() && member.deadline() != null
							&& Instant.now().isBefore(member.deadline()));
		return new DungeonLobbyView(Long.toString(lobby), catalog.definition().name(),
				state.getLeader() == 0 ? null : characters.requireId(state.getLeader()).toString(),
				state.getRun() == null ? null : state.getRun().toString(),
				state.getTargetRun() == null ? null : state.getTargetRun().toString(), state.isClosed(), canLaunch,
				members);
	}

	/** 按稳定编号游标翻页；每页有界扫描，不因大厅刷新读取所有历史队伍。 */
	@Transactional
	public Page recruitment(long after) {
		var rows = lobbies.recruitment(after, PageRequest.of(0, 50));
		var offers = rows.stream()
			.filter(row -> codec.readValue(row.getState(), DungeonLobbyState.class).getTargetRun() == null)
			.map(row -> {
				var state = codec.readValue(row.getState(), DungeonLobbyState.class);
				return new DungeonLobbyView.Recruitment(row.getId().toString(), catalog.definition().name(),
						state.getLeader() == 0 ? null : characters.find(state.getLeader()).name(),
						state.getMembers().size(), catalog.definition().players(),
						state.getMembers()
							.keySet()
							.stream()
							.map(account -> characters.find(account).profession())
							.toList());
			})
			.filter(offer -> offer.members() < offer.capacity())
			.toList();
		return new Page(offers, rows.size() == 50 ? rows.getLast().getId().toString() : null);
	}

	public record Page(List<DungeonLobbyView.Recruitment> offers, String next) {
	}

	/** 每次最多读取五十份存档；只公开战间缺员队伍，不泄露账号、编队、物资或掉落。 */
	@Transactional
	public ReplacementPage replacements(long after) {
		var rows = dungeonRunRecordRepository.recruitment(after, PageRequest.of(0, 50));
		var offers = rows.stream().filter(row -> {
			var state = codec.readValue(row.getState(), DungeonProgressState.class);
			long count = state.getMembers().values().stream().filter(member -> !member.isExited()).count();
			return state.isRecruiting() && state.getActiveBattle() == null
					&& state.getCleared() < catalog.definition().stages().size() && count > 0
					&& count < catalog.definition().players();
		}).map(row -> {
			var state = codec.readValue(row.getState(), DungeonProgressState.class);
			var members = state.getMembers()
				.values()
				.stream()
				.filter(member -> !member.isExited())
				.map(member -> characters.find(member.getOwner()))
				.toList();
			return new Replacement(row.getId().toString(), catalog.definition().name(), state.getCleared(),
					catalog.definition().stages().size(), members.size(), catalog.definition().players(),
					members.stream().map(member -> member.profession()).toList());
		}).toList();
		return new ReplacementPage(offers, rows.size() == 50 ? rows.getLast().getId().toString() : null);
	}

	public record Replacement(String id, String name, int cleared, int stages, int members, int capacity,
			List<String> professions) {
	}

	public record ReplacementPage(List<Replacement> offers, String next) {
	}

}
