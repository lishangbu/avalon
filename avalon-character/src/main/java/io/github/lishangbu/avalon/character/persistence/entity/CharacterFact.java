package io.github.lishangbu.avalon.character.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import jakarta.persistence.UniqueConstraint;
import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

/**
 * 角色已发生的游戏事实。只由实际完成对应行为的应用命令写入，客户端不能直接声明事实。
 */
@DatabaseForeignKey(name = "character_fact_character_id_fkey", columns = { "character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@Entity
@Table(name = "character_fact", comment = "角色已发生的游戏事实。只由实际完成对应行为的应用命令写入，客户端不能直接声明事实。",
		uniqueConstraints = { @UniqueConstraint(name = "character_fact_character_id_code_key",
				columnNames = { "character_id", "code" }) })
@Getter
@Setter
public class CharacterFact {

	/** 雪花主键，内部保持 Long，序列化时避免 JavaScript 精度丢失。 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	@Column(name = "id", nullable = false, comment = "雪花主键，内部保持 Long，序列化时避免 JavaScript 精度丢失。")
	private Long id;

	/** 所属角色；不通过跨模块实体关联传播持久化状态。 */
	@Column(name = "character_id", nullable = false, comment = "所属角色；不通过跨模块实体关联传播持久化状态。")
	private Long characterId;

	/** 稳定事实编号，例如在特定教学阶段完成恢复；显示文本不参与判定。 */
	@Column(name = "code", nullable = false, length = 255, comment = "稳定事实编号，例如在特定教学阶段完成恢复；显示文本不参与判定。")
	private String code;

	/** 首次实际发生时间；重复操作保留原时间，不覆盖已经取得的资格。 */
	@Column(name = "created_at", nullable = false, comment = "首次实际发生时间；重复操作保留原时间，不覆盖已经取得的资格。")
	private Instant createdAt;

}
