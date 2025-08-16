plugins {
	`maven-publish`
}

publishing {
	repositories {
		if (project.hasProperty("siphalor.maven.user")) {
			maven {
				name = "siphalor"
				url = uri("https://maven.siphalor.de/upload.php")
				credentials {
					username = project.property("siphalor.maven.user") as String
					password = project.property("siphalor.maven.password") as String
				}
			}
		}
	}
}
