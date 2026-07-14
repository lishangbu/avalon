package io.github.lishangbu.security.rbac

import io.github.lishangbu.common.web.security.BATTLE_RULES_ADMIN_AUTHORITY
import io.github.lishangbu.common.web.security.BATTLE_SANDBOX_RUN_AUTHORITY
import io.github.lishangbu.common.web.security.BATTLE_SESSIONS_RUN_AUTHORITY
import io.github.lishangbu.common.web.security.GAME_DATA_ADMIN_AUTHORITY
import io.github.lishangbu.common.web.security.SECURITY_ADMIN_AUTHORITY

/**
 * 系统管理 API 所需的稳定访问节点 code。
 */
const val SECURITY_ADMIN_ACCESS_NODE = SECURITY_ADMIN_AUTHORITY

/**
 * 战斗规则管理 API 所需的稳定访问节点 code。
 */
const val BATTLE_RULES_ADMIN_ACCESS_NODE = BATTLE_RULES_ADMIN_AUTHORITY

/**
 * 战斗沙盒执行 API 所需的稳定访问节点 code。
 */
const val BATTLE_SANDBOX_RUN_ACCESS_NODE = BATTLE_SANDBOX_RUN_AUTHORITY

/**
 * 战斗会话执行 API 所需的稳定访问节点 code。
 */
const val BATTLE_SESSIONS_RUN_ACCESS_NODE = BATTLE_SESSIONS_RUN_AUTHORITY

/**
 * 游戏资料管理 API 所需的稳定访问节点 code。
 */
const val GAME_DATA_ADMIN_ACCESS_NODE = GAME_DATA_ADMIN_AUTHORITY
