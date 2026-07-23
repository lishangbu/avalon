package io.github.lishangbu

import cn.dev33.satoken.SaManager
import cn.dev33.satoken.dao.SaTokenDao
import cn.dev33.satoken.stp.StpInterface
import org.springframework.test.context.TestContext
import org.springframework.test.context.support.AbstractTestExecutionListener

/**
 * 在多个 Spring 测试上下文之间恢复当前上下文拥有的 Sa-Token 持久化协作方。
 *
 * Sa-Token 将 DAO 和 RBAC 服务保存在进程级静态槽位中，而 Spring Test 会缓存并复用多个
 * ApplicationContext。另一个上下文关闭后，静态槽位可能仍指向它已经关闭的数据源，因此每个测试方法
 * 开始前都必须重新绑定当前上下文中的 Bean。
 */
class SaTokenSpringContextTestExecutionListener : AbstractTestExecutionListener() {
	override fun beforeTestMethod(testContext: TestContext) {
		val applicationContext = testContext.applicationContext
		applicationContext.getBeanProvider(SaTokenDao::class.java).ifAvailable(SaManager::setSaTokenDao)
		applicationContext.getBeanProvider(StpInterface::class.java).ifAvailable(SaManager::setStpInterface)
	}
}
