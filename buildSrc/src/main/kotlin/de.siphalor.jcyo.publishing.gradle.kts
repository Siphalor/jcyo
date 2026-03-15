import de.siphalor.jcyo.build.ModuleProperties

plugins {
	`maven-publish`
	id("de.siphalor.jcyo.base")
}

val siphalorMavenUser = project.property("siphalor.maven.user") as String?
val siphalorMavenPassword = project.property("siphalor.maven.password") as String?
publishing {
	repositories {
		if (siphalorMavenUser != null && siphalorMavenPassword != null) {
			maven {
				name = "siphalor"
				url = uri("https://maven.siphalor.de/upload.php")
				credentials {
					username = siphalorMavenUser
					password = siphalorMavenPassword
				}
			}
		}
	}

	publications.all {
		if (this is MavenPublication) {
			pom {
				extensions.getByType<ModuleProperties>().let { module ->
					name = module.name
					description = module.description
					url = project.property("git.url") as String
					scm {
						url = project.property("git.url") as String
					}
				}
			}
		}
	}
}
