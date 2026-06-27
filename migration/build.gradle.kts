plugins {
	alias(libs.plugins.kotlin.spring)
}

dependencies {
	implementation(libs.spring.boot.starter)
	implementation(libs.spring.boot.starter.jdbc)
	implementation(libs.spring.boot.starter.liquibase)
	runtimeOnly(libs.postgresql)
	testImplementation(libs.spring.boot.starter.test)
	testImplementation(platform(libs.testcontainers.bom))
	testImplementation(libs.testcontainers.junit.jupiter)
	testImplementation(libs.testcontainers.postgresql)
}

tasks.withType<Test> {
	maxHeapSize = "2g"
}
