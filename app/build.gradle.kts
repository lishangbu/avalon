plugins {
	alias(libs.plugins.kotlin.spring)
	alias(libs.plugins.spring.boot)
}

dependencies {
	implementation(project(":common-web"))
	implementation(project(":migration"))
	implementation(project(":scheduler"))
	implementation(project(":battle-rules"))
	implementation(project(":game-data"))
	implementation(project(":security"))
	implementation(project(":system"))
	implementation(libs.spring.boot.starter)
	implementation(libs.spring.boot.starter.actuator)
	implementation(libs.spring.boot.starter.web)
	implementation(libs.spring.boot.starter.validation)
	implementation(libs.jimmer.spring.boot.starter)
	implementation(libs.kotlin.reflect)
	implementation(libs.springdoc.openapi.starter.webmvc.ui)
	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.spring.boot.starter.security.test)
	testImplementation(platform(libs.testcontainers.bom))
	testImplementation(libs.testcontainers.junit.jupiter)
	testImplementation(libs.testcontainers.postgresql)
}

tasks.withType<Test> {
	maxHeapSize = "4g"
}
