plugins {
	alias(libs.plugins.kotlin.spring)
	alias(libs.plugins.ksp)
}

dependencies {
	implementation(project(":common-persistence"))
	implementation(libs.jackson.databind)
	implementation(libs.jimmer.spring.boot.starter)
	implementation(libs.kotlin.reflect)
	implementation(libs.spring.boot.autoconfigure)
	implementation(libs.spring.boot.starter)
	implementation(libs.spring.boot.starter.quartz)
	implementation(libs.springdoc.openapi.starter.webmvc.api)
	ksp(libs.jimmer.ksp)
	runtimeOnly(libs.postgresql)
	testImplementation(project(":migration"))
	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.spring.boot.starter.liquibase)
	testImplementation(platform(libs.testcontainers.bom))
	testImplementation(libs.testcontainers.junit.jupiter)
	testImplementation(libs.testcontainers.postgresql)
}
