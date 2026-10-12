package io.github.lishangbu.avalon.world.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 最近一次部署的定义，仅用于下次部署的兼容性检查，不参与任何实例执行或领奖计算。 没有版本历史；检查通过后覆盖，所有实例仍使用当前内容。
 */
@Entity
@Table(name = "quest_definition_guard")
@Getter
@Setter
public class QuestDefinitionGuard {

	@Id
	private String id;

	@Column(nullable = false, columnDefinition = "text")
	private String definition;

}
