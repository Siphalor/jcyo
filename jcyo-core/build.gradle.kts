plugins {
	`java-library`
	id("de.siphalor.jcyo.publishing")
}

group = rootProject.group
version = rootProject.version

repositories {
	mavenCentral()
}

dependencies {
	annotationProcessor(libs.lombok)
	compileOnly(libs.lombok)
	testAnnotationProcessor(libs.lombok)
	testCompileOnly(libs.lombok)

	implementation(libs.acl)

	compileOnly(libs.jetbrains.annotations)
	compileOnly(libs.jspecify.annotations)

	testImplementation(platform(libs.junit.platform))
	testImplementation(libs.junit.core)
	testRuntimeOnly(libs.junit.launcher)
	testImplementation(libs.assertj)
}

java {
	withSourcesJar()
}

tasks.test {
	useJUnitPlatform()
	systemProperties(
		"junit.jupiter.execution.timeout.mode" to "disabled_on_debug",
		"junit.jupiter.execution.timeout.testable.method.default" to "10s",
		"junit.jupiter.execution.timeout.thread.mode.default" to "SEPARATE_THREAD",
	)
}

publishing {
	publications {
		create<MavenPublication>("jar") {
			from(components.getByName("java"))
		}
	}
}
