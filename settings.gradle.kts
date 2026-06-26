rootProject.name = "avalon"

include(
	":common-persistence",
	":s3-core",
	":s3-spring-boot-autoconfigure",
	":s3-spring-boot-starter",
	":scheduler",
	":migration",
	":security",
	":system",
	":app",
)
