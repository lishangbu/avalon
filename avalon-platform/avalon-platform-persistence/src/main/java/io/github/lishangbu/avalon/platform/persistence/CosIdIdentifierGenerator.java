package io.github.lishangbu.avalon.platform.persistence;

import java.util.EnumSet;

import me.ahoo.cosid.IdGenerator;
import me.ahoo.cosid.provider.IdGeneratorProvider;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;

/**
 * Hibernate 插入前的主键生成适配，复用 Starter 管理的 CosId，不另建发号器。 从当前 SessionFactory 的 Spring Bean 容器取得
 * 共享 CosId 生成器，避免静态注册表串用不同应用／测试上下文。 延迟到实际持久化时获取，防止构建实体映射阶段提前初始化 CosId 或产生容器依赖环。
 */
public class CosIdIdentifierGenerator implements BeforeExecutionGenerator, BeanContainer.LifecycleOptions {

	@Override
	public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue,
			EventType eventType) {
		return session.getFactory()
			.getServiceRegistry()
			.requireService(ManagedBeanRegistry.class)
			.getBeanContainer()
			.getBean(IdGeneratorProvider.SHARE + "SnowflakeId", IdGenerator.class, this,
					FallbackBeanInstanceProducer.INSTANCE)
			.getBeanInstance()
			.generate();
	}

	/** 生成器由 Spring 管理，可在当前容器中复用，不能跨应用缓存。 */
	@Override
	public boolean canUseCachedReferences() {
		return true;
	}

	/** CosId 注册的是已有实例；禁止 JPA 重新构造 IdGenerator 接口或创建另一套发号状态。 */
	@Override
	public boolean useJpaCompliantCreation() {
		return false;
	}

	/** 更新实体不会重新发号；编号生成不需要数据库往返。 */
	@Override
	public EnumSet<EventType> getEventTypes() {
		return EventTypeSets.INSERT_ONLY;
	}

}
