package io.github.lishangbu.avalon.jimmer.jackson

import tools.jackson.databind.module.SimpleModule

/**
 * 为 Jackson 3 注册 Jimmer Input DTO 的反序列化兼容能力。
 */
class InputModuleV3 : SimpleModule() {
    override fun getRegistrationId(): Any = MODULE_ID

    override fun getModuleName(): String = MODULE_ID

    override fun setupModule(context: SetupContext) {
        super.setupModule(context)
        context.insertAnnotationIntrospector(InputAnnotationIntrospectorV3())
    }

    companion object {
        const val MODULE_ID: String = "io.github.lishangbu.avalon.jimmer.jackson.InputModuleV3"
    }
}
