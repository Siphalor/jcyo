package de.siphalor.jcyo.build

import org.gradle.api.provider.Property

interface ModuleProperties {
	val name: Property<String>
	val description: Property<String>
}
