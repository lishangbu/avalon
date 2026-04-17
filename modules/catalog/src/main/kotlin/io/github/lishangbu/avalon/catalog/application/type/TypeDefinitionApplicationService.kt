package io.github.lishangbu.avalon.catalog.application.type

import io.github.lishangbu.avalon.catalog.domain.CatalogNotFound
import io.github.lishangbu.avalon.catalog.domain.TypeDefinition
import io.github.lishangbu.avalon.catalog.domain.TypeDefinitionDraft
import io.github.lishangbu.avalon.catalog.domain.TypeDefinitionId
import io.github.lishangbu.avalon.catalog.domain.type.TypeDefinitionRepository
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * 属性定义应用服务。
 *
 * 负责属性定义能力的标识转换与 not-found 语义，不再让统一 `CatalogService`
 * 继续承担 type 能力的 CRUD 入口。
 */
@ApplicationScoped
class TypeDefinitionApplicationService(
    private val repository: TypeDefinitionRepository,
) {
    /**
     * 列出全部属性定义。
     */
    suspend fun listTypeDefinitions(): List<TypeDefinition> = repository.listTypeDefinitions()

    /**
     * 查询单个属性定义。
     */
    suspend fun getTypeDefinition(id: UUID): TypeDefinition =
        repository.findTypeDefinition(TypeDefinitionId(id))
            ?: throw CatalogNotFound("type_definition", id.toString())

    /**
     * 创建属性定义。
     */
    suspend fun createTypeDefinition(draft: TypeDefinitionDraft): TypeDefinition =
        repository.createTypeDefinition(draft)

    /**
     * 更新属性定义。
     */
    suspend fun updateTypeDefinition(
        id: UUID,
        draft: TypeDefinitionDraft,
    ): TypeDefinition = repository.updateTypeDefinition(TypeDefinitionId(id), draft)

    /**
     * 删除属性定义。
     */
    suspend fun deleteTypeDefinition(id: UUID) {
        repository.deleteTypeDefinition(TypeDefinitionId(id))
    }
}
