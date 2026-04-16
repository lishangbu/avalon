package io.github.lishangbu.avalon.identity.access.domain.iam

import io.github.lishangbu.avalon.shared.kernel.domain.DomainRuleViolation

/**
 * 菜单层级中允许持久化和对外暴露的节点类型。
 *
 * 该枚举既服务菜单持久化，也服务前端菜单树展示，因此只保留对双方都稳定的节点分类。
 */
enum class MenuType {
    DIRECTORY,
    MENU,
    BUTTON,
    LINK,
    ;

    /**
     * 返回数据库持久化使用的小写值。
     *
     * @return 面向存储层的稳定小写枚举值。
     */
    fun storageValue(): String = name.lowercase()

    companion object {
        /**
         * 从持久化值恢复菜单类型。
         *
         * @param value 数据库存储或外部传入的菜单类型值。
         * @return 对应的菜单类型枚举。
         * @throws DomainRuleViolation 当值无法映射到已知菜单类型时抛出。
         */
        fun fromStorage(value: String): MenuType =
            entries.firstOrNull { it.storageValue() == value.lowercase() }
                ?: throw DomainRuleViolation("Unsupported menu type: $value")
    }
}