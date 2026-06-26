plugins {
	id("backend.publishable-library")
}

dependencies {
	api(project(":s3-spring-boot-autoconfigure"))
	api(platform(libs.aws.sdk.bom))
	api(libs.aws.sdk.s3)
}

publishing {
	publications.named<MavenPublication>("mavenJava") {
		pom {
			name.set("S3 Spring Boot Starter")
			description.set("Dependency starter for S3 Spring Boot integration.")
		}
	}
}
