plugins {
	`java-gradle-plugin`
	alias(libs.plugins.kotlin.jvm)
	id("de.siphalor.jcyo.publishing")
}

group = rootProject.group
version = rootProject.version

repositories {
	mavenCentral()
}

dependencies {
	implementation(project(":jcyo-core"))
}

gradlePlugin {
	val jcyo by plugins.creating {
		id = "de.siphalor.jcyo"
		implementationClass = "de.siphalor.jcyo.gradle.JcyoGradlePlugin"
	}
}

java {
	withSourcesJar()
}

testing {
	suites {
		val test by getting(JvmTestSuite::class) {
			useKotlinTest(libs.versions.kotlin.get())
		}

		val functionalTest by registering(JvmTestSuite::class) {
			useKotlinTest(libs.versions.kotlin.get())

			dependencies {
				implementation(project())
			}

			targets {
				all {
					testTask.configure { shouldRunAfter(test) }
				}
			}
		}
	}
}

gradlePlugin.testSourceSets.add(sourceSets["functionalTest"])

tasks.named("check") {
	dependsOn(testing.suites.named("functionalTest"))
}
