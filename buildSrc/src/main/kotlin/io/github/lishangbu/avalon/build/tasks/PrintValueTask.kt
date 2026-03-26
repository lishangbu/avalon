package io.github.lishangbu.avalon.build.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * 输出单个 Gradle 属性或计算值，便于 shell/CI 直接消费。
 *
 * 当 CI 只需要从 Gradle 拿到一个值时，可以通过这个小任务避免
 * 解析冗长的命令输出。
 */
@DisableCachingByDefault(because = "Prints a configured value to standard output.")
abstract class PrintValueTask : DefaultTask() {
    @get:Input
    abstract val value: Property<String>

    @TaskAction
    fun printValue() {
        println(value.get())
    }
}
