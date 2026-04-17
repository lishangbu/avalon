package io.github.lishangbu.avalon.catalog.domain.type

import io.github.lishangbu.avalon.catalog.domain.TypeDefinition
import io.github.lishangbu.avalon.catalog.domain.TypeDefinitionDraft
import io.github.lishangbu.avalon.catalog.domain.TypeDefinitionId

/**
 * 属性定义写模型仓储契约。
 *
 * 该仓储只承接属性定义本身的 CRUD，不混入属性矩阵、物种或招式等其他 catalog 能力。
 */
interface TypeDefinitionRepository {
    /**
     * 列出全部属性定义。
     */
    suspend fun listTypeDefinitions(): List<TypeDefinition>

    /**
     * 按标识查询属性定义。
     */
    suspend fun findTypeDefinition(id: TypeDefinitionId): TypeDefinition?

    /**
     * 创建属性定义。
     */
    suspend fun createTypeDefinition(draft: TypeDefinitionDraft): TypeDefinition

    /**
     * 更新属性定义。
     */
    suspend fun updateTypeDefinition(
        id: TypeDefinitionId,
        draft: TypeDefinitionDraft,
    ): TypeDefinition

    /**
     * 删除属性定义。
     */
    suspend fun deleteTypeDefinition(id: TypeDefinitionId)
}
