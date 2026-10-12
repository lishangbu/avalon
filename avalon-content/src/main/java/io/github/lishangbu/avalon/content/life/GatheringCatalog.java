package io.github.lishangbu.avalon.content.life;

import io.github.lishangbu.avalon.content.ContentChecks;
import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.life.model.GatheringDefinition;
import io.github.lishangbu.avalon.content.life.model.GatheringNode;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** 无限资源点目录；地点与产物启动校验，资源不保存枯竭次数或每日额度。 */
@Component
public class GatheringCatalog {

	private final List<GatheringNode> nodes;

	private final Map<String, GatheringNode> byId;

	@Autowired
	public GatheringCatalog(ItemCatalog items, WorldCatalog world) {
		this(ContentLoader.read("/content/gathering.json", GatheringDefinition.class), items, world);
	}

	public GatheringCatalog(GatheringDefinition definition, ItemCatalog items, WorldCatalog world) {
		nodes = definition.nodes();
		byId = ContentChecks.index("/content/gathering.json", "nodes.id", nodes, GatheringNode::id);
		for (var node : nodes) {
			items.ordinaryReference("/content/gathering.json", node.id(), "product", node.product());
			if (node.rare() != null)
				items.ordinaryReference("/content/gathering.json", node.id(), "rare", node.rare());
			world.referencedPlace("/content/gathering.json", node.id(), "location", node.location());
			if (!LifeSkillRules.belongsTo(node.skill(), LifeSkillRulesCategory.GATHERING) || node.level() < 1
					|| node.level() > LifeSkillRules.MAX_LEVEL || node.quantity() < 1 || node.quantity() > 99)
				throw ContentChecks.invalid("/content/gathering.json", node.id(), "node", node, "采集资源配置无效");
			if (node.rare() != null && node.level() < 5)
				throw ContentChecks.invalid("/content/gathering.json", node.id(), "rare", node.rare(), "稀有采集资源配置无效");
		}
	}

	public List<GatheringNode> nodes() {
		return nodes;
	}

	public GatheringNode node(String id) {
		return byId.get(id);
	}

}
