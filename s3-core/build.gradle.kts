plugins {
	id("backend.publishable-library")
}

dependencies {
	api(platform(libs.aws.sdk.bom))
	api(libs.aws.sdk.s3)
	testImplementation(libs.spring.boot.starter.test)
}

publishing {
	publications.named<MavenPublication>("mavenJava") {
		pom {
			name.set("S3 Core")
			description.set("Provider-neutral S3 operation boundary and AWS SDK v2 adapter.")
		}
	}
}
