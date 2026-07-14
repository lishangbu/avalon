plugins {
	alias(libs.plugins.kotlin.spring)
	alias(libs.plugins.ksp)
}

dependencies {
	implementation(project(":common-persistence"))
	implementation(project(":common-web"))
	implementation(libs.spring.boot.starter)
	implementation(libs.spring.security.crypto)
	implementation(libs.spring.boot.starter.web)
	implementation(libs.spring.boot.starter.validation)
	implementation(libs.jackson.databind)
	implementation(libs.jimmer.spring.boot.starter)
	implementation(libs.sa.token.spring.boot3.starter)
	implementation(libs.kotlin.reflect)
	ksp(libs.jimmer.ksp)
	runtimeOnly(libs.postgresql)
	testImplementation(project(":migration"))
	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.spring.boot.starter.liquibase)
	testImplementation(libs.spring.boot.starter.web)
	testImplementation(platform(libs.testcontainers.bom))
	testImplementation(libs.testcontainers.junit.jupiter)
	testImplementation(libs.testcontainers.postgresql)
}
