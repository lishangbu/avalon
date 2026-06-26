import org.gradle.api.file.DuplicatesStrategy
import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
	id("backend.publishable-library")
	alias(libs.plugins.kotlin.kapt)
	alias(libs.plugins.kotlin.spring)
}

dependencies {
	api(project(":s3-core"))
	api(libs.spring.boot.autoconfigure)
	api(platform(libs.aws.sdk.bom))
	api(libs.aws.sdk.s3)
	annotationProcessor(libs.spring.boot.configuration.processor)
	kapt(libs.spring.boot.configuration.processor)
	testImplementation(libs.jackson.databind)
	testImplementation(libs.spring.boot.starter.test)
	testImplementation(platform(libs.testcontainers.bom))
	testImplementation(libs.testcontainers.junit.jupiter)
}

tasks.named<ProcessResources>("processResources") {
	dependsOn("kaptKotlin")
	from(layout.buildDirectory.dir("tmp/kapt3/classes/main")) {
		include("META-INF/spring-configuration-metadata.json")
	}
}

tasks.named<Jar>("jar") {
	filesMatching("META-INF/spring-configuration-metadata.json") {
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	}
}

publishing {
	publications.named<MavenPublication>("mavenJava") {
		pom {
			name.set("S3 Spring Boot Autoconfigure")
			description.set("Spring Boot auto-configuration for S3 operations.")
		}
	}
}
