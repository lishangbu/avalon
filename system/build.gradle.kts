plugins {
	alias(libs.plugins.kotlin.spring)
	alias(libs.plugins.ksp)
}

dependencies {
	implementation(project(":common-web"))
	implementation(project(":scheduler"))
	implementation(project(":security"))
	implementation(libs.spring.boot.starter)
	implementation(libs.spring.boot.starter.security)
	implementation(libs.spring.boot.starter.security.oauth2.authorization.server)
	implementation(libs.spring.boot.starter.web)
	implementation(libs.jimmer.spring.boot.starter)
	implementation(libs.kotlin.reflect)
	implementation(libs.springdoc.openapi.starter.webmvc.api)
	ksp(libs.jimmer.ksp)
	testImplementation(project(":migration"))
	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.spring.boot.starter.liquibase)
	testImplementation(libs.spring.boot.starter.security.test)
	testImplementation(platform(libs.testcontainers.bom))
	testImplementation(libs.testcontainers.junit.jupiter)
	testImplementation(libs.testcontainers.postgresql)
}
