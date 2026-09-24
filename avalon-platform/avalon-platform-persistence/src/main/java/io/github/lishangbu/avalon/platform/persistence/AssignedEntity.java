package io.github.lishangbu.avalon.platform.persistence;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

/**
 * 自然主键或共享主键在入库前已赋值，不能用主键是否为空判断 INSERT。普通雪花主键使用 CosIdGenerated，无需继承此类。 显式记录持久化生命周期，确保
 * Spring Data save 对新实例使用 persist， 后续同事务修改仍作用于受管实例，而不是被 merge 复制后遗留的游离对象。
 */
@MappedSuperclass
public abstract class AssignedEntity<ID> implements Persistable<ID> {

	@Transient
	private boolean newEntity = true;

	@Override
	@JsonIgnore
	public boolean isNew() {
		return newEntity;
	}

	@PostPersist
	@PostLoad
	protected void markPersisted() {
		newEntity = false;
	}

}
