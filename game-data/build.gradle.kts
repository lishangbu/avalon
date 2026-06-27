plugins {
	alias(libs.plugins.kotlin.spring)
}

dependencies {
	implementation(project(":common-web"))
	implementation(libs.spring.boot.starter)
	implementation(libs.spring.boot.starter.jdbc)
	implementation(libs.spring.boot.starter.web)
	implementation(libs.springdoc.openapi.starter.webmvc.api)
	testImplementation(project(":migration"))
	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.spring.boot.starter.liquibase)
	testImplementation(platform(libs.testcontainers.bom))
	testImplementation(libs.testcontainers.junit.jupiter)
	testImplementation(libs.testcontainers.postgresql)
}
