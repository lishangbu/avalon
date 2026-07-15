plugins {
	alias(libs.plugins.kotlin.jvm) apply false
	alias(libs.plugins.kotlin.kapt) apply false
	alias(libs.plugins.kotlin.spring) apply false
	alias(libs.plugins.ksp) apply false
	alias(libs.plugins.spring.boot) apply false
	alias(libs.plugins.spring.dependency.management) apply false
}

val versionCatalog = extensions.getByType<org.gradle.api.artifacts.VersionCatalogsExtension>().named("libs")

group = "io.github.lishangbu"
version = "0.0.1-SNAPSHOT"

val integrationTest = tasks.register("integrationTest") {
	description = "运行所有模块中标记为 integration 的测试。"
	group = "verification"
}

allprojects {
	repositories {
		mavenCentral()
	}
}

subprojects {
	group = rootProject.group
	version = rootProject.version

	apply(plugin = "org.jetbrains.kotlin.jvm")
	apply(plugin = "io.spring.dependency-management")

	extensions.configure<org.gradle.api.plugins.JavaPluginExtension> {
		toolchain {
			languageVersion = JavaLanguageVersion.of(25)
		}
	}

	extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
		compilerOptions {
			freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
		}
	}

	extensions.configure<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension> {
		imports {
			mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
		}
	}

	dependencies {
		"testImplementation"(versionCatalog.findLibrary("kotlin-test-junit5").get())
		"testRuntimeOnly"(versionCatalog.findLibrary("junit-platform-launcher").get())
	}

	tasks.withType<Test> {
		useJUnitPlatform()
	}

	val integrationTestTask = tasks.register<Test>("integrationTest") {
		description = "运行依赖容器或完整外部基础设施的集成测试。"
		group = "verification"
		val sourceSets = project.extensions.getByType<org.gradle.api.tasks.SourceSetContainer>()
		testClassesDirs = sourceSets["test"].output.classesDirs
		classpath = sourceSets["test"].runtimeClasspath
		shouldRunAfter(tasks.named("test"))
		useJUnitPlatform {
			includeTags("integration")
		}
	}

	tasks.named<Test>("test") {
		useJUnitPlatform {
			excludeTags("integration")
		}
	}

	integrationTest.configure {
		dependsOn(integrationTestTask)
	}
}
