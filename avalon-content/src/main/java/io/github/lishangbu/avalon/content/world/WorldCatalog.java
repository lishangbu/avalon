package io.github.lishangbu.avalon.content.world;

import io.github.lishangbu.avalon.content.ContentChecks;
import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.world.model.WorldDefinition;
import io.github.lishangbu.avalon.content.world.model.WorldNpc;
import io.github.lishangbu.avalon.content.world.model.WorldNpcBinding;
import io.github.lishangbu.avalon.content.world.model.WorldPlace;
import io.github.lishangbu.avalon.content.world.model.WorldProfession;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

/**
 * 启动加载的只读世界定义。显示文本和相邻关系来自 JSON，不嵌入 HTTP 或前端代码。
 */
@Component
public class WorldCatalog {

	private final WorldDefinition definition;

	private final Map<String, WorldPlace> places;

	public WorldCatalog() {
		this(ContentLoader.read("/content/world/opening.json", WorldDefinition.class));
	}

	/** 内存定义入口与正式资源入口使用同一校验，测试无需重复读取 JSON。 */
	public WorldCatalog(WorldDefinition definition) {
		this.definition = validate(definition);
		places = ContentChecks.index("/content/world/opening.json", "places.id", definition.places(), WorldPlace::id);
	}

	/**
	 * 验证起点、唯一地点坐标、出口引用、道路双向性及从起点可达性，最后验证职业编号。坏配置拒绝开服，不能等玩家移动才报错。当前坐标范围是开篇内容契约，不代表已实现任意大小世界。
	 */
	public static WorldDefinition validate(WorldDefinition value) {
		if (value == null || value.places().isEmpty() || value.professions().isEmpty())
			throw new IllegalArgumentException("地图或职业为空");
		Set<String> codes = new HashSet<>(), positions = new HashSet<>();
		Set<String> npcIds = new HashSet<>();
		for (WorldNpc npc : value.npcs())
			if (npc.id() == null || npc.id().isBlank() || npc.name() == null || npc.name().isBlank()
					|| !npcIds.add(npc.id()))
				throw new IllegalArgumentException("NPC 名录编号重复或名称为空");
		for (WorldPlace place : value.places()) {
			if (place.requiredFacts().stream().anyMatch(fact -> fact == null || fact.isBlank()))
				throw new IllegalArgumentException("地点开放事实不能为空");
			Set<String> bound = new HashSet<>();
			for (WorldNpcBinding binding : place.npcs()) {
				if (!npcIds.contains(binding.npcId()) || !bound.add(binding.npcId()))
					throw new IllegalArgumentException("地点 NPC 未注册或重复绑定");
				if (binding.requiredFact() != null && binding.requiredFact().equals(binding.closedByFact()))
					throw new IllegalArgumentException("NPC 出现与离开条件相同");
			}
			if (!Set.of("SUPPLIES", "RECOVERY", "STORAGE", "PVP").containsAll(place.services()))
				throw new IllegalArgumentException("地点服务类型无效");
			if (place.id() == null || place.name() == null || place.description() == null || !codes.add(place.id())
					|| place.x() < 0 || place.x() > 2 || place.y() < 0 || place.y() > 30
					|| !positions.add(place.x() + ":" + place.y()))
				throw new IllegalArgumentException("地点编号、坐标或文本无效");
			if (new HashSet<>(place.exits()).size() != place.exits().size())
				throw new IllegalArgumentException("出口重复");
		}
		if (!codes.contains(value.start()))
			throw new IllegalArgumentException("起点不存在");
		// 只接受可直接证明互斥的交接条件，宁可拒绝模糊配置，也不让同一人物同时出现在两处。
		for (int i = 0; i < value.places().size(); i++)
			for (int j = i + 1; j < value.places().size(); j++)
				for (WorldNpcBinding left : value.places().get(i).npcs())
					for (WorldNpcBinding right : value.places().get(j).npcs())
						if (left.npcId().equals(right.npcId()) && !exclusive(left, right))
							throw new IllegalArgumentException("同一 NPC 的地点绑定不能证明互斥");
		for (var destination : value.safeReturns())
			if (!codes.contains(destination.location()) || !codes.containsAll(destination.fromLocations()))
				throw new IllegalArgumentException("安全返回地点不存在");
		if (!value.safeReturns().isEmpty() && value.safeReturns()
			.stream()
			.noneMatch(destination -> destination.requires() == null && destination.fromLocations().isEmpty()))
			throw new IllegalArgumentException("安全返回必须有初始无条件地点");
		for (WorldPlace place : value.places())
			for (String exit : place.exits()) {
				if (exit.equals(place.id()) || !codes.contains(exit))
					throw new IllegalArgumentException("出口引用无效");
				if (value.places()
					.stream()
					.filter(p -> p.id().equals(exit))
					.noneMatch(p -> p.exits().contains(place.id())))
					throw new IllegalArgumentException("道路必须双向连接");
			}
		Set<String> reachable = new HashSet<>();
		ArrayDeque<String> queue = new ArrayDeque<>();
		queue.add(value.start());
		while (!queue.isEmpty()) {
			String id = queue.remove();
			if (reachable.add(id))
				value.places()
					.stream()
					.filter(p -> p.id().equals(id))
					.findFirst()
					.orElseThrow()
					.exits()
					.forEach(queue::add);
		}
		if (reachable.size() != codes.size())
			throw new IllegalArgumentException("存在不可达地点");
		codes.clear();
		for (WorldProfession job : value.professions())
			if (job.id() == null || job.name() == null || !codes.add(job.id()))
				throw new IllegalArgumentException("职业编号重复或缺少名称");
		return value;
	}

	/**
	 * 返回启动构建的不可变世界定义，供内容验证与浏览器地图展示。返回集合不能被请求修改。
	 */
	public WorldDefinition definition() {
		return definition;
	}

	/**
	 * 按稳定地点编号查只读模板。持久化角色引用已经移除的地点时抛内部错误，不能静默传送玩家到起点。
	 */
	public WorldPlace place(String id) {
		var place = places.get(id);
		if (place == null)
			throw new IllegalStateException("/content/world/opening.json places.id=" + id + "：玩家地点已不在当前配置中");
		return place;
	}

	/** 城镇能力由地点声明；命令与页面共用配置，不按固定城镇名称判断资格。 */
	public boolean provides(String location, String service) {
		return place(location).services().contains(service);
	}

	/** 地图投影与移动命令共用阶段门禁，设施按配置事实开放或关闭，不能只在页面隐藏。 */
	/** 配置引用保留引用方上下文，避免只报告目标地图而丢失错误条目。 */
	public WorldPlace referencedPlace(String resource, String entry, String field, String location) {
		var place = places.get(location);
		if (place == null)
			throw ContentChecks.invalid(resource, entry, field, location, "地点引用不存在");
		return place;
	}

	public boolean accessible(WorldPlace place, Predicate<String> hasFact) {
		return allowsAccess(place, hasFact);
	}

	/** requiredFact 必须满足，requiredFacts 是任选其一；关闭条件优先。 */
	public static boolean allowsAccess(WorldPlace place, Predicate<String> hasFact) {
		boolean required = (place.requiredFact() == null || hasFact.test(place.requiredFact()))
				&& (place.requiredFacts().isEmpty() || place.requiredFacts().stream().anyMatch(hasFact));
		return required && (place.closedByFact() == null || !hasFact.test(place.closedByFact()));
	}

	private static boolean exclusive(WorldNpcBinding left, WorldNpcBinding right) {
		return left.closedByFact() != null && Objects.equals(left.closedByFact(), right.requiredFact())
				|| right.closedByFact() != null && Objects.equals(right.closedByFact(), left.requiredFact());
	}

	/** 名录显示名不参与身份判断，条件必须在每次命令执行时重新读取。 */
	public static boolean present(WorldNpcBinding binding, Predicate<String> hasFact) {
		return (binding.requiredFact() == null || hasFact.test(binding.requiredFact()))
				&& (binding.closedByFact() == null || !hasFact.test(binding.closedByFact()));
	}

	/** 坏配置应在开服前暴露，不能等玩家走到某个节点才发现出口无效。 */

}
