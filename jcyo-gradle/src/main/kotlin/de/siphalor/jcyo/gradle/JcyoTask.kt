package de.siphalor.jcyo.gradle

import de.siphalor.jcyo.core.api.Jcyo
import de.siphalor.jcyo.core.api.JcyoOptions
import de.siphalor.jcyo.core.api.JcyoVariables
import de.siphalor.jcyo.core.api.import_order.ImportOrder
import de.siphalor.jcyo.core.api.value.JcyoValue
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction

abstract class JcyoTask : DefaultTask() {
	@get:SkipWhenEmpty
	@get:InputDirectory
	@get:PathSensitive(PathSensitivity.RELATIVE)
	abstract val inputDirectory: DirectoryProperty

	@get:Input
	abstract val updateInputFiles: Property<Boolean>

	@get:Input
	abstract val variables: MapProperty<String, Any>

	@get:Input
	abstract val importOrder: ListProperty<String>

	@get:OutputDirectory
	@get:Optional
	abstract val cleanOutputDirectory: DirectoryProperty

	init {
		updateInputFiles.convention(true)
		variables.convention(mapOf())
		importOrder.convention(listOf())
	}

	@TaskAction
	fun run() {
		val jcyoVariables = JcyoVariables()
		variables.get().forEach { (key, value) -> jcyoVariables.set(key, JcyoValue.of(value)) }

		Jcyo.builder()
			.variables(jcyoVariables)
			.options(JcyoOptions.builder()
				.updateInput(updateInputFiles.get())
				.importOrder(importOrder.get().ifEmpty { null }?.let { elements -> ImportOrder.fromSpotless(elements) })
				.build())
			.baseDirectory(inputDirectory.get().asFile.toPath())
			.cleanOutputDirectory(cleanOutputDirectory.orNull?.asFile?.toPath())
			.build()
			.processAll()
	}
}
