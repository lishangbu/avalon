package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.*
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher

/** 认证模块常用对象抓取器 */
internal object AuthorizationFetchers {
    /** 用户标量字段抓取器 */
    val USER =
        newFetcher(User::class).`by` {
            allScalarFields()
        }

    /** 用户及角色抓取器 */
    val USER_WITH_ROLES =
        newFetcher(User::class).`by` {
            allScalarFields()
            roles {
                allScalarFields()
            }
        }

    /** 角色标量字段抓取器 */
    val ROLE =
        newFetcher(Role::class).`by` {
            allScalarFields()
        }

    /** 角色及菜单抓取器 */
    val ROLE_WITH_MENUS =
        newFetcher(Role::class).`by` {
            allScalarFields()
            menus {
                allScalarFields()
            }
        }

    /** 菜单标量字段抓取器 */
    val MENU =
        newFetcher(Menu::class).`by` {
            allScalarFields()
        }
}
